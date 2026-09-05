package com.mikelcrm.licenseservice.util;

import java.text.Normalizer;

/**
 * Utilidad para derivar el slug de un tenant a partir de su nombre.
 * <p>
 * DEBE coincidir con deriveSlug() de SMT (src/lib/slug.ts) para que la
 * correlación por slug (nombre de schema) sea consistente entre ambos sistemas:
 * minúsculas, se eliminan acentos/diacríticos, los caracteres no alfanuméricos
 * se convierten en '-', los guiones se colapsan y se recortan en los extremos.
 * <p>
 * Fuente única de verdad: cualquier lugar que necesite derivar el slug (creación,
 * suspensión por impago, reactivación por pago, etc.) debe usar este método para
 * evitar implementaciones divergentes.
 */
public final class SlugUtil {

    private SlugUtil() {
        // Utility class: no instances.
    }

    public static String toSlug(String nombre) {
        String normalized = Normalizer.normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", ""); // elimina diacríticos (acentos)
        return normalized.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
    }
}
