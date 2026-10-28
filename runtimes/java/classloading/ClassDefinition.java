package classloading;

import java.util.Objects;
import java.util.Set;

/**
 * Holds minimal class metadata for this model; it is not a parsed JVM class
 * file.
 */
public record ClassDefinition(
        String name, String superName, Set<String> fields, Runnable initializer) {
    public ClassDefinition {
        requireName(name, "name");
        if (superName != null) {
            requireName(superName, "superName");
            if (superName.equals(name)) {
                throw new IllegalArgumentException("a class cannot extend itself");
            }
        }
        fields = Set.copyOf(Objects.requireNonNull(fields, "fields"));
        for (String field : fields) {
            requireName(field, "field");
        }
        Objects.requireNonNull(initializer, "initializer");
    }

    public ClassDefinition(String name, String superName, Set<String> fields) {
        this(name, superName, fields, () -> {
        });
    }

    private static void requireName(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
    }
}
