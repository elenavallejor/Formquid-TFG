package com.formquid.user.service;

import com.formquid.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.UUID;

/** Guarda las fotos de perfil en un directorio persistente fuera del contenedor. */
@Service
public class ProfilePhotoStorage {
    private static final long MAX_BYTES = 3L * 1024 * 1024;
    private final Path directory;

    public ProfilePhotoStorage(@Value("${app.profile-photo.directory:./data/profile-photos}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    public String save(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La foto debe ser una imagen de hasta 3 MB");
        }
        try {
            byte[] bytes = file.getBytes();
            String filename = UUID.randomUUID() + imageExtension(bytes);
            Files.createDirectories(directory);
            Path target = directory.resolve(filename);
            Path temporary = Files.createTempFile(directory, "profile-", ".tmp");
            try {
                Files.write(temporary, bytes);
                try {
                    Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException exception) {
                    Files.move(temporary, target);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
            onTransactionCompletion(false, target);
            return "/api/v1/users/" + userId + "/photo/" + filename;
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se ha podido guardar la foto de perfil");
        }
    }

    public StoredPhoto read(String filename) {
        if (!validFilename(filename)) throw new ApiException(HttpStatus.NOT_FOUND, "Foto de perfil no encontrada");
        try {
            byte[] bytes = Files.readAllBytes(directory.resolve(filename));
            return new StoredPhoto(bytes, filename.endsWith(".png") ? "image/png" : "image/jpeg");
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Foto de perfil no encontrada");
        }
    }

    public void deleteAfterCommit(Long userId, String imageKey) {
        String prefix = "/api/v1/users/" + userId + "/photo/";
        if (imageKey == null || !imageKey.startsWith(prefix)) return;
        String filename = imageKey.substring(prefix.length());
        if (validFilename(filename)) onTransactionCompletion(true, directory.resolve(filename));
    }

    private boolean validFilename(String filename) {
        return filename != null && filename.matches("[0-9a-f-]{36}\\.(png|jpg)");
    }

    private void onTransactionCompletion(boolean deleteOnCommit, Path path) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            if (deleteOnCommit) deleteQuietly(path);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if ((status == STATUS_COMMITTED) == deleteOnCommit) deleteQuietly(path);
            }
        });
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // La foto activa permanece disponible aunque falle una limpieza posterior.
        }
    }

    private String imageExtension(byte[] bytes) {
        if (bytes.length > MAX_BYTES) throw new ApiException(HttpStatus.BAD_REQUEST, "La foto no puede superar 3 MB");
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (stream == null) throw new ApiException(HttpStatus.BAD_REQUEST, "La imagen no es válida");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new ApiException(HttpStatus.BAD_REQUEST, "La imagen no es válida");
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream);
                if (reader.getWidth(0) > 4000 || reader.getHeight(0) > 4000) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "La imagen no puede superar 4000 píxeles por lado");
                }
                String format = reader.getFormatName();
                if ("png".equalsIgnoreCase(format)) return ".png";
                if ("jpeg".equalsIgnoreCase(format)) return ".jpg";
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La imagen no es válida");
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "Solo se admiten imágenes PNG o JPEG");
    }

    public record StoredPhoto(byte[] bytes, String contentType) {}
}
