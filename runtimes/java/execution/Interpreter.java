package execution;

import classloading.ClassDefinition;
import classloading.MiniClassLoader;
import memory.HeapObject;

import java.util.*;

/**
 * Executes the model's small instruction set; it is not a JVM bytecode
 * interpreter.
 */
public final class Interpreter {
    private final MiniClassLoader classLoader;
    private final Map<Long, HeapObject> heap = new LinkedHashMap<>();
    private long nextObjectId = 1;

    public Interpreter(MiniClassLoader classLoader) {
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
    }

    public Object execute(List<Instruction> instructions, int localCount) {
        StackFrame frame = new StackFrame(instructions, localCount);
        OperandStack stack = frame.operandStack();
        while (frame.hasNext()) {
            Instruction instruction = frame.next();
            switch (instruction.opcode()) {
                case CONST -> stack.push(instruction.operand());
                case LOAD -> stack.push(frame.readLocal((Integer) instruction.operand()));
                case STORE -> frame.writeLocal((Integer) instruction.operand(), stack.pop());
                case ADD -> {
                    int right = asInteger(stack.pop());
                    int left = asInteger(stack.pop());
                    stack.push(Math.addExact(left, right));
                }
                case NEW -> {
                    ClassDefinition definition = classLoader.initialize((String) instruction.operand());
                    HeapObject object = new HeapObject(nextObjectId++, definition.name(), definition.fields());
                    heap.put(object.id(), object);
                    stack.push(object);
                }
                case GET_FIELD -> {
                    Object value = asObject(stack.pop()).readField((String) instruction.operand());
                    stack.push(Objects.requireNonNull(value, "uninitialized field"));
                }
                case PUT_FIELD -> {
                    Object value = stack.pop();
                    asObject(stack.pop()).writeField((String) instruction.operand(), value);
                }
                case RETURN -> {
                    return stack.pop();
                }
            }
        }
        throw new IllegalStateException("program ended without RETURN");
    }

    public int heapSize() {
        return heap.size();
    }

    private static int asInteger(Object value) {
        if (value instanceof Integer integer)
            return integer;
        throw new IllegalStateException("expected integer, got " + value.getClass().getSimpleName());
    }

    private static HeapObject asObject(Object value) {
        if (value instanceof HeapObject object)
            return object;
        throw new IllegalStateException("expected heap object, got " + value.getClass().getSimpleName());
    }

    public static void main(String[] args) {
        MiniClassLoader loader = new MiniClassLoader(null);
        loader.define(new ClassDefinition("Counter", null, Set.of("value")));
        List<Instruction> code = List.of(
                Instruction.of(Opcode.NEW, "Counter"), Instruction.of(Opcode.STORE, 0),
                Instruction.of(Opcode.LOAD, 0), Instruction.of(Opcode.CONST, 41),
                Instruction.of(Opcode.PUT_FIELD, "value"), Instruction.of(Opcode.LOAD, 0),
                Instruction.of(Opcode.LOAD, 0), Instruction.of(Opcode.GET_FIELD, "value"),
                Instruction.of(Opcode.CONST, 1), Instruction.of(Opcode.ADD),
                Instruction.of(Opcode.PUT_FIELD, "value"), Instruction.of(Opcode.LOAD, 0),
                Instruction.of(Opcode.GET_FIELD, "value"), Instruction.of(Opcode.RETURN));
        Interpreter interpreter = new Interpreter(loader);
        if (!interpreter.execute(code, 1).equals(42) || interpreter.heapSize() != 1) {
            throw new AssertionError();
        }
        System.out.println("Interpreter: ok");
    }
}
