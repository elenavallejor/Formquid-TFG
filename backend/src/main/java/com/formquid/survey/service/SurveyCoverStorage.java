package com.formquid.survey.service;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Iterator;
import java.util.UUID;

/** Almacena las portadas fuera de la imagen del contenedor. */
@Service
public class SurveyCoverStorage {
    private static final long MAX_BYTES = 2L * 1024 * 1024;
    private final Path directory;

    public SurveyCoverStorage(@Value("${app.survey-cover.directory:./data/survey-covers}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    public String save(Long surveyId, MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La portada debe ser una imagen de hasta 2 MB");
        }
        try {
            byte[] bytes = file.getBytes();
            String extension = imageExtension(bytes);
            String filename = UUID.randomUUID() + extension;
            Path target = directory.resolve(filename);
            Files.createDirectories(directory);
            Path temporary = Files.createTempFile(directory, "cover-", ".tmp");
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
            return "/api/v1/public/surveys/" + surveyId + "/cover/" + filename;
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se ha podido guardar la portada");
        }
    }

    public StoredCover read(String filename) {
        if (!filename.matches("[0-9a-f-]{36}\\.(png|jpg)")) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Portada no encontrada");
        }
        try {
            byte[] bytes = Files.readAllBytes(directory.resolve(filename));
            return new StoredCover(bytes, filename.endsWith(".png") ? "image/png" : "image/jpeg");
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Portada no encontrada");
        }
    }

    public String inlineForAuthorizedRespondent(Long surveyId, String imageKey) {
        String prefix = "/api/v1/public/surveys/" + surveyId + "/cover/";
        if (imageKey == null || !imageKey.startsWith(prefix)) return imageKey;
        StoredCover cover = read(imageKey.substring(prefix.length()));
        return "data:" + cover.contentType() + ";base64," + Base64.getEncoder().encodeToString(cover.bytes());
    }

    public void deleteAfterCommit(Long surveyId, String imageKey) {
        String prefix = "/api/v1/public/surveys/" + surveyId + "/cover/";
        if (imageKey == null || !imageKey.startsWith(prefix)) return;
        String filename = imageKey.substring(prefix.length());
        if (!filename.matches("[0-9a-f-]{36}\\.(png|jpg)")) return;
        onTransactionCompletion(true, directory.resolve(filename));
    }

    private void onTransactionCompletion(boolean committed, Path path) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            if (committed) deleteQuietly(path);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if ((status == STATUS_COMMITTED) == committed) deleteQuietly(path);
            }
        });
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // A failed cleanup leaves an unreferenced file, without losing the active cover.
        }
    }

    private String imageExtension(byte[] bytes) {
        if (bytes.length > MAX_BYTES) throw new ApiException(HttpStatus.BAD_REQUEST, "La portada no puede superar 2 MB");
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (stream == null) throw new ApiException(HttpStatus.BAD_REQUEST, "La imagen no es válida");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new ApiException(HttpStatus.BAD_REQUEST, "La imagen no es válida");
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream);
                String format = reader.getFormatName();
                if (reader.getWidth(0) > 4000 || reader.getHeight(0) > 4000) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "La imagen no puede superar 4000 píxeles por lado");
                }
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

    public record StoredCover(byte[] bytes, String contentType) {}
}
