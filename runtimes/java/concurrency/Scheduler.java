package concurrency;

import java.util.*;

/**
 * Runs one model step at a time and records round-robin carrier IDs; it does
 * not schedule JVM threads.
 */
public final class Scheduler {
    private final int carrierCount;
    private final Deque<VirtualThread> runnable = new ArrayDeque<>();
    private final Map<String, VirtualThread> threads = new LinkedHashMap<>();
    private final List<String> events = new ArrayList<>();
    private int nextCarrier;

    public Scheduler(int carrierCount) {
        if (carrierCount < 1)
            throw new IllegalArgumentException("carrierCount must be positive");
        this.carrierCount = carrierCount;
    }

    public void submit(VirtualThread thread) {
        Objects.requireNonNull(thread, "thread");
        if (thread.state() != VirtualThread.State.RUNNABLE) {
            throw new IllegalArgumentException("thread must be runnable");
        }
        if (threads.putIfAbsent(thread.id(), thread) != null) {
            throw new IllegalArgumentException("duplicate thread: " + thread.id());
        }
        runnable.addLast(thread);
    }

    public void runNext() {
        VirtualThread thread = runnable.pollFirst();
        if (thread == null)
            throw new IllegalStateException("no runnable thread");
        int carrier = nextCarrier++ % carrierCount;
        thread.mount();
        events.add(thread.id() + " mount carrier-" + carrier);
        VirtualThread.Signal signal = thread.step();
        thread.unmount(signal);
        switch (signal) {
            case YIELD -> {
                events.add(thread.id() + " unmount:yield");
                runnable.addLast(thread);
            }
            case BLOCK -> events.add(thread.id() + " unmount:block");
            case DONE -> events.add(thread.id() + " unmount:terminate");
        }
    }

    public void unblock(String id) {
        VirtualThread thread = threads.get(id);
        if (thread == null)
            throw new NoSuchElementException("thread not found: " + id);
        thread.unblock();
        runnable.addLast(thread);
        events.add(id + " unblock");
    }

    public List<String> events() {
        return List.copyOf(events);
    }

    public static void main(String[] args) {
        Scheduler scheduler = new Scheduler(1);
        VirtualThread a = new VirtualThread("VT-A",
                List.of(VirtualThread.Signal.BLOCK, VirtualThread.Signal.DONE));
        VirtualThread b = new VirtualThread("VT-B", List.of(VirtualThread.Signal.DONE));
        scheduler.submit(a);
        scheduler.submit(b);
        scheduler.runNext();
        scheduler.runNext();
        scheduler.unblock("VT-A");
        scheduler.runNext();
        List<String> expected = List.of("VT-A mount carrier-0", "VT-A unmount:block",
                "VT-B mount carrier-0", "VT-B unmount:terminate", "VT-A unblock",
                "VT-A mount carrier-0", "VT-A unmount:terminate");
        if (!scheduler.events().equals(expected)
                || a.state() != VirtualThread.State.TERMINATED
                || b.state() != VirtualThread.State.TERMINATED)
            throw new AssertionError();
        System.out.println("Scheduler: ok");
    }
}
