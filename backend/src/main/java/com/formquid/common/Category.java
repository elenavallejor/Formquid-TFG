package com.formquid.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;
import java.util.Objects;

/** Código persistido como texto para admitir nuevas categorías sin desplegar código. */
public record Category(String code) {
    public static final Category OTROS = new Category("OTROS");
    public static final Category DATOS_PERSONALES = new Category("DATOS_PERSONALES");
    public static final Category TELEFONO_INTELIGENTE = new Category("TELEFONO_INTELIGENTE");
    public static final Category ORDENADORES = new Category("ORDENADORES");

    private static final Map<String, String> LEGACY_CODES = Map.ofEntries(
        Map.entry("DESKTOP_COMPUTER", "ORDENADORES"),
        Map.entry("LAPTOP", "ORDENADORES"),
        Map.entry("ORDENADOR_SOBREMESA", "ORDENADORES"),
        Map.entry("PORTATIL", "ORDENADORES"),
        Map.entry("TABLET", "TABLETA"),
        Map.entry("SMARTPHONE", "TELEFONO_INTELIGENTE"),
        Map.entry("SMARTWATCH", "OTROS"),
        Map.entry("SMART_TV", "TELEVISION_INTELIGENTE"),
        Map.entry("VIDEO_GAME_CONSOLE", "VIDEOJUEGOS"),
        Map.entry("E_BOOK_READER", "LIBROS_TEXTO_DIGITALES"),
        Map.entry("INTERACTIVE_WHITEBOARD", "PIZARRA_INTERACTIVA"),
        Map.entry("DIGITAL_PROJECTOR", "PROYECTOR_DIGITAL"),
        Map.entry("LEARNING_MANAGEMENT_SYSTEM", "PLATAFORMA_APRENDIZAJE"),
        Map.entry("EDUCATIONAL_APPS", "PLATAFORMA_APRENDIZAJE"),
        Map.entry("DIGITAL_TEXTBOOKS", "LIBROS_TEXTO_DIGITALES"),
        Map.entry("EDUCATIONAL_ROBOTICS", "ROBOTICA_EDUCATIVA"),
        Map.entry("THREE_D_PRINTING", "IMPRESION_3D"),
        Map.entry("ARTIFICIAL_INTELLIGENCE", "INTELIGENCIA_ARTIFICIAL"),
        Map.entry("GENERATIVE_AI", "INTELIGENCIA_ARTIFICIAL"),
        Map.entry("IA_GENERATIVA", "INTELIGENCIA_ARTIFICIAL"),
        Map.entry("CHATBOTS", "INTELIGENCIA_ARTIFICIAL"),
        Map.entry("VOICE_ASSISTANTS", "ASISTENTES_VOZ"),
        Map.entry("VIRTUAL_REALITY", "REALIDAD_VIRTUAL"),
        Map.entry("AUGMENTED_REALITY", "REALIDAD_AUMENTADA"),
        Map.entry("SOCIAL_MEDIA", "REDES_SOCIALES"),
        Map.entry("INSTANT_MESSAGING", "REDES_SOCIALES"),
        Map.entry("VIDEO_CONFERENCING", "OTROS"),
        Map.entry("VIDEO_GAMES", "VIDEOJUEGOS"),
        Map.entry("STREAMING_PLATFORMS", "OTROS"),
        Map.entry("OTHER", "OTROS"),
        Map.entry("PERSONAL_DATA", "DATOS_PERSONALES"),
        Map.entry("RELOJ_INTELIGENTE", "OTROS"),
        Map.entry("CONSOLA_VIDEOJUEGOS", "VIDEOJUEGOS"),
        Map.entry("APLICACIONES_EDUCATIVAS", "PLATAFORMA_APRENDIZAJE"),
        Map.entry("BOTS_CONVERSACION", "INTELIGENCIA_ARTIFICIAL"),
        Map.entry("MENSAJERIA_INSTANTANEA", "REDES_SOCIALES"),
        Map.entry("VIDEOCONFERENCIAS", "OTROS"),
        Map.entry("LECTOR_LIBROS_ELECTRONICOS", "LIBROS_TEXTO_DIGITALES"),
        Map.entry("PLATAFORMAS_TRANSMISION", "OTROS")
    );

    public Category {
        Objects.requireNonNull(code, "La categoría es obligatoria");
        if (!code.matches("[A-Z][A-Z0-9_]{1,63}")) {
            throw new IllegalArgumentException("El código de categoría no es válido");
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Category of(String code) {
        if (code == null) return null;
        return new Category(LEGACY_CODES.getOrDefault(code, code));
    }

    @JsonValue
    public String value() {
        return code;
    }
}
