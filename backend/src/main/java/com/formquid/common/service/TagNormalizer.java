package com.formquid.common.service;

import java.text.Normalizer;
import java.util.Locale;

public final class TagNormalizer {

    private TagNormalizer() {
    }

    public static String normalize(String tagName) {
        if (tagName == null || tagName.isBlank()) {
            throw new IllegalArgumentException("La etiqueta es obligatoria");
        }

        String cleaned = tagName.trim().toLowerCase(Locale.ROOT);
        if (cleaned.startsWith("##")) {
            throw new IllegalArgumentException("La etiqueta no puede empezar por ##");
        }
        if (cleaned.startsWith("#")) {
            cleaned = cleaned.substring(1);
        }

        cleaned = Normalizer.normalize(cleaned, Normalizer.Form.NFKC)
            .replaceAll("\\s+", "-");

        if (cleaned.isBlank()) {
            throw new IllegalArgumentException("La etiqueta no puede quedar vacía");
        }
        if (cleaned.contains("#")) {
            throw new IllegalArgumentException("La etiqueta no puede contener #");
        }
        if (cleaned.length() > 50) {
            throw new IllegalArgumentException("La etiqueta no puede superar 50 caracteres");
        }
        if (!cleaned.matches("[\\p{L}\\p{N}-]+")) {
            throw new IllegalArgumentException("La etiqueta solo admite letras, números y guiones");
        }
        return cleaned;
    }
}
