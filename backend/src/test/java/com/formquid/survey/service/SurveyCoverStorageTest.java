package com.formquid.survey.service;

import com.formquid.common.exception.ApiException;
import com.formquid.survey.repository.SurveyCoverRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SurveyCoverStorageTest {
    @TempDir Path directory;
    @Mock SurveyCoverRepository repository;

    @Test
    void save_thenRead_usesDatabaseWithoutWritingFile() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        byte[] bytes = output.toByteArray();
        SurveyCoverStorage storage = new SurveyCoverStorage(directory.toString(), repository);

        String key = storage.save(12L, new MockMultipartFile("file", "cover.png", "image/png", bytes));
        String filename = key.substring(key.lastIndexOf('/') + 1);
        when(repository.read(filename)).thenReturn(Optional.of(bytes));

        assertThat(storage.read(filename).bytes()).isEqualTo(bytes);
        assertThat(storage.read(filename).contentType()).isEqualTo("image/png");
        assertThat(storage.inlineForAuthorizedRespondent(12L, key))
            .startsWith("data:image/png;base64,");
        verify(repository).save(12L, filename, "image/png", bytes);
        assertThat(directory.resolve(filename)).doesNotExist();
    }

    @Test
    void save_rejectsContentThatIsNotAnImage() {
        SurveyCoverStorage storage = new SurveyCoverStorage(directory.toString(), repository);
        MockMultipartFile file = new MockMultipartFile("file", "fake.png", "image/png", "not an image".getBytes());

        assertThatThrownBy(() -> storage.save(12L, file)).isInstanceOf(ApiException.class);
    }

    @Test
    void deleteAfterCommit_preservesOldCoverUntilTransactionCommits() throws Exception {
        SurveyCoverStorage storage = new SurveyCoverStorage(directory.toString(), repository);
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
        verify(repository).delete(filename);
    }

    @Test
    void read_whenCoverPredatesDatabaseStorage_readsExistingFile() throws Exception {
        SurveyCoverStorage storage = new SurveyCoverStorage(directory.toString(), repository);
        String filename = "123e4567-e89b-12d3-a456-426614174000.png";
        Files.write(directory.resolve(filename), new byte[] {1, 2, 3});
        when(repository.read(filename)).thenReturn(Optional.empty());

        assertThat(storage.read(filename).bytes()).containsExactly(1, 2, 3);
    }
}
