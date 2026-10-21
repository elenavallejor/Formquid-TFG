package com.formquid.survey.service;

import com.formquid.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SurveyCoverStorageTest {
    @TempDir Path directory;

    @Test
    void save_thenRead_preservesImageOnDisk() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        byte[] bytes = output.toByteArray();
        SurveyCoverStorage storage = new SurveyCoverStorage(directory.toString());

        String key = storage.save(12L, new MockMultipartFile("file", "cover.png", "image/png", bytes));
        String filename = key.substring(key.lastIndexOf('/') + 1);

        assertThat(storage.read(filename).bytes()).isEqualTo(bytes);
        assertThat(storage.read(filename).contentType()).isEqualTo("image/png");
        assertThat(storage.inlineForAuthorizedRespondent(12L, key))
            .startsWith("data:image/png;base64,");
    }

    @Test
    void save_rejectsContentThatIsNotAnImage() {
        SurveyCoverStorage storage = new SurveyCoverStorage(directory.toString());
        MockMultipartFile file = new MockMultipartFile("file", "fake.png", "image/png", "not an image".getBytes());

        assertThatThrownBy(() -> storage.save(12L, file)).isInstanceOf(ApiException.class);
    }

    @Test
    void deleteAfterCommit_preservesOldCoverUntilTransactionCommits() throws Exception {
        SurveyCoverStorage storage = new SurveyCoverStorage(directory.toString());
        String filename = "123e4567-e89b-12d3-a456-426614174000.png";
        Path cover = directory.resolve(filename);
        Files.write(cover, new byte[] {1, 2, 3});
        TransactionSynchronizationManager.initSynchronization();
        try {
            storage.deleteAfterCommit(12L, "/api/v1/public/surveys/12/cover/" + filename);
            assertThat(cover).exists();
            TransactionSynchronizationManager.getSynchronizations().forEach(
                callback -> callback.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
            assertThat(cover).doesNotExist();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
