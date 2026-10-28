package execution;

import java.util.*;
import java.util.function.IntUnaryOperator;

/**
 * Switches to a provided compiled entry after a call threshold; it does not
 * compile code.
 */
public final class JitCompiler {
    private final int threshold;
    private final Map<String, MethodState> methods = new LinkedHashMap<>();

    public JitCompiler(int threshold) {
        if (threshold < 1) {
            throw new IllegalArgumentException("threshold must be positive");
        }
        this.threshold = threshold;
    }

    public void register(String name, IntUnaryOperator interpreted, IntUnaryOperator compiled) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        MethodState method = new MethodState(
                Objects.requireNonNull(interpreted, "interpreted"),
                Objects.requireNonNull(compiled, "compiled"));
        if (methods.putIfAbsent(name, method) != null) {
            throw new IllegalArgumentException("already registered: " + name);
        }
    }

    public int invoke(String name, int argument) {
        MethodState method = method(name);
        method.calls++;
        IntUnaryOperator entry = method.calls > threshold ? method.compiled : method.interpreted;
        return entry.applyAsInt(argument);
    }

    public int calls(String name) {
        return method(name).calls;
    }

    public boolean usesCompiledEntry(String name) {
        return calls(name) > threshold;
    }

    private MethodState method(String name) {
        MethodState method = methods.get(name);
        if (method == null)
            throw new NoSuchElementException("method not found: " + name);
        return method;
    }

    public static void main(String[] args) {
        JitCompiler jit = new JitCompiler(2);
        jit.register("work", value -> value + 1, value -> value + 100);
        if (jit.invoke("work", 1) != 2 || jit.invoke("work", 2) != 3)
            throw new AssertionError();
        if (jit.invoke("work", 1) != 101 || !jit.usesCompiledEntry("work"))
            throw new AssertionError();
        System.out.println("JitCompiler: ok");
    }

    private static final class MethodState {
        private final IntUnaryOperator interpreted;
        private final IntUnaryOperator compiled;
        private int calls;

        private MethodState(IntUnaryOperator interpreted, IntUnaryOperator compiled) {
            this.interpreted = interpreted;
            this.compiled = compiled;
        }
    }
}
