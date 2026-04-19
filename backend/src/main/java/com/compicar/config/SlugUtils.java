package com.compicar.config;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtils {

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
    private static final Pattern MULTI_DASH = Pattern.compile("-{2,}");

    private SlugUtils() {
    }

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "item";
        }

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT);

        String slug = NON_ALNUM.matcher(normalized).replaceAll("-");
        slug = MULTI_DASH.matcher(slug).replaceAll("-");
        slug = slug.replaceAll("(^-|-$)", "");

        return slug.isBlank() ? "item" : slug;
    }
}