package model;

import model.Event.EventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Step 3: Event ordering determinism tests.
 *
 * Verifies that Event.compareTo() provides a total order:
 * 1. time (ascending)
 * 2. EventType ordinal (ascending)
 * 3. sequenceNumber (ascending, FIFO tie-breaker)
 */
class EventOrderingTest {

    // ── Test 1: Same time, different types ─────────────────────────────────

    @Test
    @DisplayName("Test 1: All four EventTypes at same time are ordered by ordinal")
    void testAllEventTypesOrderedByOrdinal() {
        EV ev = new EV(0, 10, 50, 60, 0.2, 0.8, 7.0);
        Charger charger = new Charger(0, 7.0);

        Event departure = new Event(10, EventType.EV_DEPARTURE, ev, null);
        Event failure   = new Event(10, EventType.CHARGER_FAILURE, null, charger);
        Event repair    = new Event(10, EventType.CHARGER_REPAIR, null, charger);
        Event arrival   = new Event(10, EventType.EV_ARRIVAL, ev, null);

        // Departure < Failure < Repair < Arrival at the same time
        assertTrue(departure.compareTo(failure) < 0,
                "DEPARTURE (0) should precede FAILURE (1)");
        assertTrue(failure.compareTo(repair) < 0,
                "FAILURE (1) should precede REPAIR (2)");
        assertTrue(repair.compareTo(arrival) < 0,
                "REPAIR (2) should precede ARRIVAL (3)");
        assertTrue(departure.compareTo(arrival) < 0,
                "DEPARTURE (0) should precede ARRIVAL (3)");

        // Verify ordinals explicitly
        assertEquals(0, EventType.EV_DEPARTURE.ordinal());
        assertEquals(1, EventType.CHARGER_FAILURE.ordinal());
        assertEquals(2, EventType.CHARGER_REPAIR.ordinal());
        assertEquals(3, EventType.EV_ARRIVAL.ordinal());
    }

    // ── Test 2: Same time, same type — FIFO ordering by sequence number ───

    @Test
    @DisplayName("Test 2: Same time same type events ordered by sequence number (FIFO)")
    void testSameTimeSameTypeOrderedBySequenceNumber() {
        EV ev1 = new EV(100, 10, 50, 60, 0.2, 0.8, 7.0);
        EV ev2 = new EV(101, 10, 55, 60, 0.3, 0.8, 7.0);

        Event first  = new Event(10, EventType.EV_ARRIVAL, ev1, null);
        Event second = new Event(10, EventType.EV_ARRIVAL, ev2, null);

        // First-created event should come before second-created event
        assertTrue(first.compareTo(second) < 0,
                "First-created event should precede second-created event");
        assertTrue(second.compareTo(first) > 0,
                "Second-created event should follow first-created event");
        assertEquals(0, first.compareTo(first),
                "Event compared to itself should return 0");
    }

    // ── Test 3: PriorityQueue polls same-type events in FIFO order ────────

    @Test
    @DisplayName("Test 3: PriorityQueue polls same-time same-type events in insertion (FIFO) order")
    void testPriorityQueueFIFOForSameTimeSameType() {
        EV ev1 = new EV(200, 5, 40, 60, 0.2, 0.8, 7.0);
        EV ev2 = new EV(201, 5, 45, 60, 0.3, 0.8, 7.0);
        EV ev3 = new EV(202, 5, 50, 60, 0.4, 0.8, 7.0);

        Event e1 = new Event(5, EventType.EV_ARRIVAL, ev1, null);
        Event e2 = new Event(5, EventType.EV_ARRIVAL, ev2, null);
        Event e3 = new Event(5, EventType.EV_ARRIVAL, ev3, null);

        PriorityQueue<Event> pq = new PriorityQueue<>();
        // Insert in reverse to prove it's not just array order
        pq.add(e3);
        pq.add(e1);
        pq.add(e2);

        assertSame(e1, pq.poll(), "First polled should be e1 (lowest sequence number)");
        assertSame(e2, pq.poll(), "Second polled should be e2");
        assertSame(e3, pq.poll(), "Third polled should be e3 (highest sequence number)");
    }

    // ── Test 4: Sequence number monotonicity ──────────────────────────────

    @Test
    @DisplayName("Test 4: Sequence numbers are strictly monotonically increasing")
    void testSequenceNumberMonotonicity() {
        Event e1 = new Event(0, EventType.EV_ARRIVAL, null, null);
        Event e2 = new Event(0, EventType.EV_DEPARTURE, null, null);
        Event e3 = new Event(5, EventType.CHARGER_FAILURE, null, null);
        Event e4 = new Event(5, EventType.EV_ARRIVAL, null, null);

        assertTrue(e1.getSequenceNumber() < e2.getSequenceNumber(),
                "e2 sequence should be greater than e1");
        assertTrue(e2.getSequenceNumber() < e3.getSequenceNumber(),
                "e3 sequence should be greater than e2");
        assertTrue(e3.getSequenceNumber() < e4.getSequenceNumber(),
                "e4 sequence should be greater than e3");
    }

    // ── Test 5: Burst arrivals — deterministic station order ──────────────

    @Test
    @DisplayName("Test 5: Burst arrivals at same time produce deterministic poll order")
    void testBurstArrivalsDeterministicOrder() {
        int burstTime = 10;
        int count = 5;
        List<EV> evs = new ArrayList<>();
        List<Event> events = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            EV ev = new EV(300 + i, burstTime, burstTime + 30, 60, 0.2, 0.8, 7.0);
            evs.add(ev);
            events.add(new Event(burstTime, EventType.EV_ARRIVAL, ev, null));
        }

        PriorityQueue<Event> pq = new PriorityQueue<>(events);

        // Poll order should match insertion order (FIFO) since all have same time and type
        for (int i = 0; i < count; i++) {
            Event polled = pq.poll();
            assertNotNull(polled, "Should have event at index " + i);
            assertSame(evs.get(i), polled.getEv(),
                    "EV at poll position " + i + " should be EV id=" + evs.get(i).getId());
        }
        assertTrue(pq.isEmpty(), "PriorityQueue should be empty after polling all events");
    }

    // ── Test 6: Same-time departures — deterministic processing order ─────

    @Test
    @DisplayName("Test 6: Same-time departures are polled in insertion (FIFO) order")
    void testSameTimeDeparturesDeterministicOrder() {
        int departureTime = 50;
        EV ev1 = new EV(400, 0, departureTime, 60, 0.2, 0.8, 7.0);
        EV ev2 = new EV(401, 5, departureTime, 60, 0.3, 0.8, 7.0);
        EV ev3 = new EV(402, 8, departureTime, 60, 0.4, 0.8, 7.0);

        Event d1 = new Event(departureTime, EventType.EV_DEPARTURE, ev1, null);
        Event d2 = new Event(departureTime, EventType.EV_DEPARTURE, ev2, null);
        Event d3 = new Event(departureTime, EventType.EV_DEPARTURE, ev3, null);

        PriorityQueue<Event> pq = new PriorityQueue<>();
        // Insert in scrambled order
        pq.add(d2);
        pq.add(d3);
        pq.add(d1);

        assertSame(d1, pq.poll(), "First departure should be d1 (lowest sequence number)");
        assertSame(d2, pq.poll(), "Second departure should be d2");
        assertSame(d3, pq.poll(), "Third departure should be d3 (highest sequence number)");
    }
}
