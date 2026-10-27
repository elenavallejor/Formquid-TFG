package com.formquid.user.service;

import com.formquid.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfilePhotoStorageTest {
    @TempDir Path directory;

    @Test
    void saveAndReadAcceptsRealPng() throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", output);
        var storage = new ProfilePhotoStorage(directory.toString());

        String key = storage.save(7L, new MockMultipartFile("file", "portrait.png", "image/png", output.toByteArray()));
        String filename = key.substring(key.lastIndexOf('/') + 1);

        assertThat(key).startsWith("/api/v1/users/7/photo/");
        assertThat(storage.read(filename).bytes()).isEqualTo(output.toByteArray());
        assertThat(storage.read(filename).contentType()).isEqualTo("image/png");
    }

    @Test
    void rejectsContentThatIsNotAnImage() {
        var storage = new ProfilePhotoStorage(directory.toString());
        var file = new MockMultipartFile("file", "portrait.png", "image/png", "invalid".getBytes());

        assertThatThrownBy(() -> storage.save(7L, file)).isInstanceOf(ApiException.class);
    }

    @Test
    void removesOldPhotoOnlyAfterCommit() throws Exception {
        var storage = new ProfilePhotoStorage(directory.toString());
        String filename = "123e4567-e89b-12d3-a456-426614174000.png";
        Path photo = directory.resolve(filename);
        Files.write(photo, new byte[] {1});
        TransactionSynchronizationManager.initSynchronization();
        try {
            storage.deleteAfterCommit(7L, "/api/v1/users/7/photo/" + filename);
            assertThat(photo).exists();
            TransactionSynchronizationManager.getSynchronizations().forEach(
                callback -> callback.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
            assertThat(photo).doesNotExist();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
