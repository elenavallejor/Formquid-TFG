package com.formquid.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Traduce búsquedas por nombre de país a los códigos usados por el selector. */
public final class CountryCodes {

    private static final Locale SPANISH = Locale.forLanguageTag("es");
    private static final Set<String> CODES = Set.copyOf(List.of(Locale.getISOCountries()));

    private CountryCodes() {
    }

    public static String normalizeConfigured(String value) {
        String trimmed = value.trim();
        String code = trimmed.toUpperCase(Locale.ROOT);
        if (CODES.contains(code) || "XK".equals(code)) {
            return code;
        }
        if ("Kosovo".equalsIgnoreCase(trimmed)) {
            return "XK";
        }
        for (String candidate : CODES) {
            if (displayName(candidate).equalsIgnoreCase(trimmed)) {
                return candidate;
            }
        }
        return trimmed;
    }

    public static List<String> matchingCodes(String search) {
        if (search.isBlank()) {
            return List.of("ZZ");
        }
        List<String> matches = new ArrayList<>();
        for (String code : CODES) {
            if (code.toLowerCase(Locale.ROOT).contains(search) || displayName(code).toLowerCase(Locale.ROOT).contains(search)) {
                matches.add(code);
            }
        }
        if ("kosovo".contains(search) || "xk".contains(search)) {
            matches.add("XK");
        }
        return matches.isEmpty() ? List.of("ZZ") : matches;
    }

    private static String displayName(String code) {
        return new Locale.Builder().setRegion(code).build().getDisplayCountry(SPANISH);
    }
}
