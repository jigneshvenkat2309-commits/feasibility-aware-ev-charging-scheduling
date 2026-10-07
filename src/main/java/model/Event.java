package model;

/**
 * A deterministic simulation event.
 *
 * EVENT PRIORITY ORDERING (total order):
 * Events are ordered by:
 * 1. time (ascending)
 * 2. EventType.ordinal() (ascending):
 *    - EV_DEPARTURE (ordinal 0) - processed first at each time
 *    - CHARGER_FAILURE (ordinal 1)
 *    - CHARGER_REPAIR (ordinal 2)
 *    - EV_ARRIVAL (ordinal 3) - processed last at each time
 * 3. sequenceNumber (ascending) - monotonic insertion-order tie-breaker
 *
 * This enforces the canonical minute-boundary semantics:
 * at each time t, departures/failures/repairs are processed before arrivals.
 * Within the same time and type, events are processed in FIFO insertion order.
 */
public class Event implements Comparable<Event> {
    public enum EventType {
        EV_DEPARTURE, CHARGER_FAILURE, CHARGER_REPAIR, EV_ARRIVAL
    }

    private static int nextSequence = 0;

    private final int time;
    private final EventType type;
    private final EV ev;
    private final Charger charger;
    private final int sequenceNumber;

    public Event(int time, EventType type, EV ev, Charger charger) {
        if (time < 0 || type == null)
            throw new IllegalArgumentException("Invalid event");
        this.time = time;
        this.type = type;
        this.ev = ev;
        this.charger = charger;
        this.sequenceNumber = nextSequence++;
    }

    public int getTime() {
        return time;
    }

    public EventType getType() {
        return type;
    }

    public EV getEv() {
        return ev;
    }

    public Charger getCharger() {
        return charger;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    /** Reset the sequence counter. For test isolation only. */
    static void resetSequence() {
        nextSequence = 0;
    }

    @Override
    public int compareTo(Event other) {
        int byTime = Integer.compare(time, other.time);
        if (byTime != 0) return byTime;
        int byType = Integer.compare(type.ordinal(), other.type.ordinal());
        if (byType != 0) return byType;
        return Integer.compare(sequenceNumber, other.sequenceNumber);
    }
}