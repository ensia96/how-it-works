package execution;

import java.util.List;
import java.util.Objects;

/**
 * Models per-invocation code, local variables, control position, and operand
 * storage.
 */
public final class StackFrame {
    private final List<Instruction> instructions;
    private final Object[] locals;
    private final OperandStack operandStack = new OperandStack();
    private int pc;

    public StackFrame(List<Instruction> instructions, int localCount) {
        this.instructions = List.copyOf(Objects.requireNonNull(instructions, "instructions"));
        if (this.instructions.isEmpty()) {
            throw new IllegalArgumentException("instructions must not be empty");
        }
        if (localCount < 0) {
            throw new IllegalArgumentException("localCount must be non-negative");
        }
        locals = new Object[localCount];
    }

    public boolean hasNext() {
        return pc < instructions.size();
    }

    public Instruction next() {
        if (!hasNext()) {
            throw new IllegalStateException("program counter is past the code");
        }
        return instructions.get(pc++);
    }

    public void jump(int target) {
        if (target < 0 || target > instructions.size()) {
            throw new IllegalArgumentException("invalid jump target: " + target);
        }
        pc = target;
    }

    public Object readLocal(int slot) {
        checkSlot(slot);
        return Objects.requireNonNull(locals[slot], "uninitialized local " + slot);
    }

    public void writeLocal(int slot, Object value) {
        checkSlot(slot);
        locals[slot] = Objects.requireNonNull(value, "value");
    }

    public OperandStack operandStack() {
        return operandStack;
    }

    private void checkSlot(int slot) {
        if (slot < 0 || slot >= locals.length) {
            throw new IllegalArgumentException("invalid local slot: " + slot);
        }
    }
}
