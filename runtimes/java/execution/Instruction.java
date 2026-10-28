package execution;

import java.util.Objects;

/**
 * Represents a validated opcode-and-operand pair in the educational execution
 * model.
 */
public record Instruction(Opcode opcode, Object operand) {
    public Instruction {
        Objects.requireNonNull(opcode, "opcode");
        switch (opcode) {
            case CONST -> {
                if (!(operand instanceof Integer)) {
                    throw new IllegalArgumentException("CONST requires an integer");
                }
            }
            case LOAD, STORE -> {
                if (!(operand instanceof Integer slot) || slot < 0) {
                    throw new IllegalArgumentException(opcode + " requires a non-negative slot");
                }
            }
            case NEW, GET_FIELD, PUT_FIELD -> {
                if (!(operand instanceof String name) || name.isBlank()) {
                    throw new IllegalArgumentException(opcode + " requires a non-blank name");
                }
            }
            case ADD, RETURN -> {
                if (operand != null) {
                    throw new IllegalArgumentException(opcode + " takes no operand");
                }
            }
        }
    }

    public static Instruction of(Opcode opcode) {
        return new Instruction(opcode, null);
    }

    public static Instruction of(Opcode opcode, Object operand) {
        return new Instruction(opcode, operand);
    }
}
