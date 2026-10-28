package memory;

import java.util.*;

/**
 * Models an identified heap allocation with named fields and explicit reference
 * edges.
 */
public final class HeapObject {
    private final long id;
    private final String type;
    private final Map<String, Object> fields = new LinkedHashMap<>();

    public HeapObject(long id, String type, Set<String> fieldNames) {
        if (id < 1)
            throw new IllegalArgumentException("id must be positive");
        if (type == null || type.isBlank())
            throw new IllegalArgumentException("type must not be blank");
        this.id = id;
        this.type = type;
        for (String field : Set.copyOf(Objects.requireNonNull(fieldNames, "fieldNames"))) {
            if (field.isBlank())
                throw new IllegalArgumentException("field must not be blank");
            fields.put(field, null);
        }
    }

    public long id() {
        return id;
    }

    public String type() {
        return type;
    }

    public Object readField(String name) {
        requireField(name);
        return fields.get(name);
    }

    public void writeField(String name, Object value) {
        requireField(name);
        fields.put(name, Objects.requireNonNull(value, "value"));
    }

    public Set<HeapObject> references() {
        Set<HeapObject> references = new LinkedHashSet<>();
        for (Object value : fields.values()) {
            if (value instanceof HeapObject reference)
                references.add(reference);
        }
        return Set.copyOf(references);
    }

    private void requireField(String name) {
        if (name == null || !fields.containsKey(name)) {
            throw new IllegalArgumentException("unknown field: " + name);
        }
    }
}
