package concurrency;

import java.util.List;
import java.util.Objects;

/**
 * Models virtual-thread state transitions without creating a JVM virtual
 * thread.
 */
public final class VirtualThread {
    public enum State {
        RUNNABLE, RUNNING, WAITING, TERMINATED
    }

    public enum Signal {
        YIELD, BLOCK, DONE
    }

    private final String id;
    private final List<Signal> steps;
    private State state = State.RUNNABLE;
    private int nextStep;

    public VirtualThread(String id, List<Signal> steps) {
        if (id == null || id.isBlank())
            throw new IllegalArgumentException("id must not be blank");
        this.id = id;
        this.steps = List.copyOf(Objects.requireNonNull(steps, "steps"));
        if (this.steps.isEmpty())
            throw new IllegalArgumentException("steps must not be empty");
        if (this.steps.getLast() != Signal.DONE
                || this.steps.subList(0, this.steps.size() - 1).contains(Signal.DONE)) {
            throw new IllegalArgumentException("DONE must be the final step");
        }
    }

    public String id() {
        return id;
    }

    public State state() {
        return state;
    }

    public void mount() {
        requireState(State.RUNNABLE);
        state = State.RUNNING;
    }

    public Signal step() {
        requireState(State.RUNNING);
        if (nextStep >= steps.size())
            throw new IllegalStateException("no step remains");
        return steps.get(nextStep++);
    }

    public void unmount(Signal signal) {
        requireState(State.RUNNING);
        state = switch (Objects.requireNonNull(signal, "signal")) {
            case YIELD -> State.RUNNABLE;
            case BLOCK -> State.WAITING;
            case DONE -> State.TERMINATED;
        };
    }

    public void unblock() {
        requireState(State.WAITING);
        state = State.RUNNABLE;
    }

    private void requireState(State expected) {
        if (state != expected) {
            throw new IllegalStateException(id + " is " + state + ", expected " + expected);
        }
    }
}
