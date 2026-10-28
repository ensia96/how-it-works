package classloading;

import java.util.*;

/**
 * Models definition lookup and class lifecycle transitions; it does not load
 * JVM class files.
 */
public final class MiniClassLoader {
    public enum State {
        DEFINED, LOADED, LINKED, INITIALIZED
    }

    private final MiniClassLoader parent;
    private final Map<String, ClassDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, State> states = new HashMap<>();

    public MiniClassLoader(MiniClassLoader parent) {
        this.parent = parent;
    }

    public void define(ClassDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        if (definitions.putIfAbsent(definition.name(), definition) != null) {
            throw new IllegalArgumentException("already defined: " + definition.name());
        }
        states.put(definition.name(), State.DEFINED);
    }

    public ClassDefinition load(String name) {
        Located located = locate(name);
        located.owner.advance(name, State.LOADED);
        return located.definition;
    }

    public void link(String name) {
        Located located = locate(name);
        located.owner.advance(name, State.LOADED);
        if (located.definition.superName() != null)
            located.owner.link(located.definition.superName());
        located.owner.advance(name, State.LINKED);
    }

    public ClassDefinition initialize(String name) {
        Located located = locate(name);
        located.owner.link(name);
        if (located.owner.states.get(name) != State.INITIALIZED) {
            if (located.definition.superName() != null)
                located.owner.initialize(located.definition.superName());
            located.definition.initializer().run();
            located.owner.advance(name, State.INITIALIZED);
        }
        return located.definition;
    }

    public State state(String name) {
        Located located = locate(name);
        return located.owner.states.get(name);
    }

    private Located locate(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Located found = null;
        for (MiniClassLoader loader = this; loader != null; loader = loader.parent) {
            ClassDefinition definition = loader.definitions.get(name);
            if (definition != null) {
                found = new Located(loader, definition);
            }
        }
        if (found == null) {
            throw new NoSuchElementException("class not found: " + name);
        }
        return found;
    }

    private void advance(String name, State target) {
        if (states.get(name).ordinal() < target.ordinal())
            states.put(name, target);
    }

    public static void main(String[] args) {
        int[] initialized = { 0 };
        MiniClassLoader parent = new MiniClassLoader(null);
        ClassDefinition base = new ClassDefinition("Base", null, Set.of(),
                () -> initialized[0]++);
        parent.define(base);
        MiniClassLoader child = new MiniClassLoader(parent);
        child.define(new ClassDefinition("Base", null, Set.of("shadow")));
        if (child.load("Base") != base || child.state("Base") != State.LOADED)
            throw new AssertionError();
        child.initialize("Base");
        child.initialize("Base");
        if (initialized[0] != 1 || parent.state("Base") != State.INITIALIZED)
            throw new AssertionError();
        System.out.println("MiniClassLoader: ok");
    }

    private record Located(MiniClassLoader owner, ClassDefinition definition) {
    }
}
