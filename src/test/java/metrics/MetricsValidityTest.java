package metrics;

import model.Charger;
import model.EV;
import model.Event;
import model.Station;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import scheduler.FCFS;
import scheduler.Scheduler;
import simulation.Simulation;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for ensuring that the total waiting time semantics (Step 5 fix) are mathematically correct.
 * Waiting time should only accumulate when an EV is physically in the WAITING state.
 */
@DisplayName("Metrics Validity Tests - Waiting Time Semantics")
public class MetricsValidityTest {

    @Test
    @DisplayName("G.1: Normal EV - Waits in queue before being assigned")
    void testNormalEVWaiting() {
        Charger charger = new Charger(0, 10.0);
        Station station = new Station("test", List.of(charger));
        Scheduler scheduler = new FCFS();
        Metrics metrics = new Metrics();
        
        // EV1 arrives at t=0, needs 1 hour. It will occupy charger from 0 to 60.
        EV ev1 = new EV(1, 0, 100, 10.0, 0.0, 1.0, 10.0);
        // EV2 arrives at t=10, needs 1 hour.
        EV ev2 = new EV(2, 10, 100, 10.0, 0.0, 1.0, 10.0);
        
        Simulation sim = new Simulation(150, station, scheduler, metrics);
        // Inject custom events
        try {
            java.lang.reflect.Field eventsField = Simulation.class.getDeclaredField("events");
            eventsField.setAccessible(true);
            java.util.PriorityQueue<Event> events = (java.util.PriorityQueue<Event>) eventsField.get(sim);
            events.clear();
            events.add(new Event(ev1.getArrivalTime(), Event.EventType.EV_ARRIVAL, ev1, null));
            events.add(new Event(ev1.getExpectedDepartureTime(), Event.EventType.EV_DEPARTURE, ev1, null));
            events.add(new Event(ev2.getArrivalTime(), Event.EventType.EV_ARRIVAL, ev2, null));
            events.add(new Event(ev2.getExpectedDepartureTime(), Event.EventType.EV_DEPARTURE, ev2, null));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        sim.run();
        
        // EV1 arrived at 0, started immediately. Wait time = 0.
        // EV2 arrived at 10, started at 60. Wait time = 50.
        assertEquals(0, ev1.getWaitingTimeMinutes(), "EV1 should have 0 wait time");
        assertEquals(50, ev2.getWaitingTimeMinutes(), "EV2 should have 50 wait time");
        
        // Average waiting time = 25
        assertEquals(25.0, metrics.getServiceMetrics().getAverageWaitingTime(), 0.001);
    }

    @Test
    @DisplayName("G.2: EV never served - Starved until departure")
    void testEVNeverServed() {
        Charger charger = new Charger(0, 10.0);
        Station station = new Station("test", List.of(charger));
        Scheduler scheduler = new FCFS();
        Metrics metrics = new Metrics();
        
        // EV1 occupies charger from t=0 to t=100
        EV ev1 = new EV(1, 0, 120, 100.0, 0.0, 1.0, 10.0); // 100 kWh @ 10kW = 10 hrs = 600 min
        // EV2 arrives at t=10, departs at t=30
        EV ev2 = new EV(2, 10, 30, 10.0, 0.0, 1.0, 10.0);
        
        Simulation sim = new Simulation(150, station, scheduler, metrics);
        try {
            java.lang.reflect.Field eventsField = Simulation.class.getDeclaredField("events");
            eventsField.setAccessible(true);
            java.util.PriorityQueue<Event> events = (java.util.PriorityQueue<Event>) eventsField.get(sim);
            events.clear();
            events.add(new Event(ev1.getArrivalTime(), Event.EventType.EV_ARRIVAL, ev1, null));
            events.add(new Event(ev1.getExpectedDepartureTime(), Event.EventType.EV_DEPARTURE, ev1, null));
            events.add(new Event(ev2.getArrivalTime(), Event.EventType.EV_ARRIVAL, ev2, null));
            events.add(new Event(ev2.getExpectedDepartureTime(), Event.EventType.EV_DEPARTURE, ev2, null));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        sim.run();
        
        // EV2 arrived at 10, never charged, departed at 30. Total physical wait = 20.
        assertEquals(20, ev2.getWaitingTimeMinutes(), "EV2 should have 20 wait time (t=10 to t=30)");
        assertTrue(ev2.isDeadlineViolated());
        assertEquals(EV.EVState.DEPARTED_NOT_ACHIEVED, ev2.getState());
        
        // Average wait time = (0 + 20) / 2 = 10
        assertEquals(10.0, metrics.getServiceMetrics().getAverageWaitingTime(), 0.001);
    }

    @Test
    @DisplayName("G.3: Charger failure and resume")
    void testChargerFailureAndResume() {
        Charger charger = new Charger(0, 10.0);
        Station station = new Station("test", List.of(charger));
        Scheduler scheduler = new FCFS();
        Metrics metrics = new Metrics();
        
        // EV1 arrives at t=10.
        EV ev1 = new EV(1, 10, 100, 10.0, 0.0, 1.0, 10.0);
        
        Simulation sim = new Simulation(150, station, scheduler, metrics);
        try {
            java.lang.reflect.Field eventsField = Simulation.class.getDeclaredField("events");
            eventsField.setAccessible(true);
            java.util.PriorityQueue<Event> events = (java.util.PriorityQueue<Event>) eventsField.get(sim);
            events.clear();
            events.add(new Event(ev1.getArrivalTime(), Event.EventType.EV_ARRIVAL, ev1, null));
            events.add(new Event(ev1.getExpectedDepartureTime(), Event.EventType.EV_DEPARTURE, ev1, null));
            
            // Fails at 20, repaired at 30
            events.add(new Event(20, Event.EventType.CHARGER_FAILURE, null, charger));
            events.add(new Event(30, Event.EventType.CHARGER_REPAIR, null, charger));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        sim.run();
        
        // Timeline:
        // Arr t=10. Start charge t=10 (wait=0).
        // Charge t=10..19 (10 min).
        // Fail t=20. Returns to queue.
        // Wait t=20..29 (10 min).
        // Repair t=30. Start charge t=30.
        // Finishes at t=80.
        assertEquals(10, ev1.getWaitingTimeMinutes(), "EV1 should have exactly 10 min wait time from the interruption");
        assertEquals(10.0, metrics.getServiceMetrics().getAverageWaitingTime(), 0.001);
    }

    @Test
    @DisplayName("G.4: Multiple charger failures")
    void testMultipleChargerFailures() {
        Charger charger = new Charger(0, 10.0);
        Station station = new Station("test", List.of(charger));
        Scheduler scheduler = new FCFS();
        Metrics metrics = new Metrics();
        
        EV ev1 = new EV(1, 0, 100, 10.0, 0.0, 1.0, 10.0);
        
        Simulation sim = new Simulation(150, station, scheduler, metrics);
        try {
            java.lang.reflect.Field eventsField = Simulation.class.getDeclaredField("events");
            eventsField.setAccessible(true);
            java.util.PriorityQueue<Event> events = (java.util.PriorityQueue<Event>) eventsField.get(sim);
            events.clear();
            events.add(new Event(ev1.getArrivalTime(), Event.EventType.EV_ARRIVAL, ev1, null));
            events.add(new Event(ev1.getExpectedDepartureTime(), Event.EventType.EV_DEPARTURE, ev1, null));
            
            // Fail 5, repair 7 -> wait 2
            events.add(new Event(5, Event.EventType.CHARGER_FAILURE, null, charger));
            events.add(new Event(7, Event.EventType.CHARGER_REPAIR, null, charger));
            
            // Fail 15, repair 20 -> wait 5
            events.add(new Event(15, Event.EventType.CHARGER_FAILURE, null, charger));
            events.add(new Event(20, Event.EventType.CHARGER_REPAIR, null, charger));
            
            // Fail 25, repair 28 -> wait 3
            events.add(new Event(25, Event.EventType.CHARGER_FAILURE, null, charger));
            events.add(new Event(28, Event.EventType.CHARGER_REPAIR, null, charger));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        sim.run();
        
        // Total wait = 2 + 5 + 3 = 10
        assertEquals(10, ev1.getWaitingTimeMinutes(), "Total wait should be sum of outage intervals");
        assertEquals(10.0, metrics.getServiceMetrics().getAverageWaitingTime(), 0.001);
    }

    @Test
    @DisplayName("G.5: Horizon abandonment")
    void testHorizonAbandonment() {
        Charger charger = new Charger(0, 10.0);
        Station station = new Station("test", List.of(charger));
        Scheduler scheduler = new FCFS();
        Metrics metrics = new Metrics();
        
        // EV1 occupies charger from t=0 to t=600
        EV ev1 = new EV(1, 0, 120, 100.0, 0.0, 1.0, 10.0);
        // EV2 arrives at 90, horizon at 100
        EV ev2 = new EV(2, 90, 120, 10.0, 0.0, 1.0, 10.0);
        
        // Simulation ends at 100
        Simulation sim = new Simulation(100, station, scheduler, metrics);
        try {
            java.lang.reflect.Field eventsField = Simulation.class.getDeclaredField("events");
            eventsField.setAccessible(true);
            java.util.PriorityQueue<Event> events = (java.util.PriorityQueue<Event>) eventsField.get(sim);
            events.clear();
            events.add(new Event(ev1.getArrivalTime(), Event.EventType.EV_ARRIVAL, ev1, null));
            events.add(new Event(ev1.getExpectedDepartureTime(), Event.EventType.EV_DEPARTURE, ev1, null));
            events.add(new Event(ev2.getArrivalTime(), Event.EventType.EV_ARRIVAL, ev2, null));
            events.add(new Event(ev2.getExpectedDepartureTime(), Event.EventType.EV_DEPARTURE, ev2, null));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        sim.run();
        
        // EV2 arrived at 90. Simulated until 100 (loop executes for t=90..99).
        // Wait time = 10.
        assertEquals(10, ev2.getWaitingTimeMinutes(), "EV2 should have 10 wait time at horizon");
        assertEquals(EV.EVState.HORIZON_ABANDONED, ev2.getState());
        
        // Average wait = (0 + 10) / 2 = 5
        assertEquals(5.0, metrics.getServiceMetrics().getAverageWaitingTime(), 0.001);
    }
}
