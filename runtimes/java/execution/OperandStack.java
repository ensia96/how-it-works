package execution;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/** Models the LIFO operand storage owned by one educational stack frame. */
public final class OperandStack {
    private final Deque<Object> values = new ArrayDeque<>();

    public void push(Object value) {
        values.push(Objects.requireNonNull(value, "value"));
    }

    public Object pop() {
        if (values.isEmpty()) {
            throw new IllegalStateException("operand stack underflow");
        }
        return values.pop();
    }

    public void clear() {
        values.clear();
    }

    public int size() {
        return values.size();
    }
}
