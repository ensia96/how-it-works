package memory;

import java.util.*;

/**
 * Traces explicit roots and sweeps a supplied object map; it is not a JVM
 * garbage collector.
 */
public final class MarkSweepCollector {
    public int collect(Map<Long, HeapObject> heap, Collection<HeapObject> roots) {
        Objects.requireNonNull(heap, "heap");
        Objects.requireNonNull(roots, "roots");
        for (Map.Entry<Long, HeapObject> entry : heap.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null
                    || entry.getKey().longValue() != entry.getValue().id()) {
                throw new IllegalArgumentException("heap key must match object id");
            }
        }

        Set<Long> reachable = new LinkedHashSet<>();
        Deque<HeapObject> pending = new ArrayDeque<>();
        for (HeapObject root : roots) {
            requireMember(heap, root);
            pending.push(root);
        }
        while (!pending.isEmpty()) {
            HeapObject object = pending.pop();
            if (!reachable.add(object.id()))
                continue;
            for (HeapObject reference : object.references()) {
                requireMember(heap, reference);
                pending.push(reference);
            }
        }

        int before = heap.size();
        heap.entrySet().removeIf(entry -> !reachable.contains(entry.getKey()));
        return before - heap.size();
    }

    private static void requireMember(Map<Long, HeapObject> heap, HeapObject object) {
        Objects.requireNonNull(object, "root or reference");
        if (heap.get(object.id()) != object) {
            throw new IllegalArgumentException("object is outside the heap: " + object.id());
        }
    }

    public static void main(String[] args) {
        HeapObject root = new HeapObject(1, "Node", Set.of("next"));
        HeapObject child = new HeapObject(2, "Node", Set.of("next"));
        HeapObject garbage = new HeapObject(3, "Node", Set.of());
        root.writeField("next", child);
        Map<Long, HeapObject> heap = new LinkedHashMap<>();
        heap.put(root.id(), root);
        heap.put(child.id(), child);
        heap.put(garbage.id(), garbage);
        int removed = new MarkSweepCollector().collect(heap, Set.of(root));
        if (removed != 1 || !heap.containsKey(1L) || !heap.containsKey(2L)
                || heap.containsKey(3L))
            throw new AssertionError();
        System.out.println("MarkSweepCollector: ok");
    }
}
