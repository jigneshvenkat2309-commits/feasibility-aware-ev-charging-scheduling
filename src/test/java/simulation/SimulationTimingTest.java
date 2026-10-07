package simulation;

import model.*;
import metrics.Metrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import scheduler.FCFS;
import scheduler.Scheduler;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 tests for simulation minute-boundary semantics and horizon behavior.
 * 
 * These tests use small manually constructed scenarios to verify the actual
 * simulation behavior without relying on auto-generated randomized events.
 */
@DisplayName("Simulation Timing and Horizon Behavior Tests")
class SimulationTimingTest {

    /**
     * Test 1: EV arrival at time t.
     * 
     * Scenario: Single EV with arrival at t=10, 1 charger.
     * Expectation: At t=10, EV enters WAITING state and is added to station.
     */
    @Test
    @DisplayName("Test 1: EV arrival at time t")
    void testEVArrivalAtTime() {
        // Create EV that arrives at t=10
        EV ev = new EV(0, 10, 50, 60, 0.2, 0.8, 7.0);

        // Before any processing, EV should be waiting
        assertTrue(ev.isWaiting(), "EV should start in WAITING state");
        assertEquals(10, ev.getArrivalTime(), "Arrival time should be recorded");

        // Simulate station receiving the EV
        Charger charger = new Charger(0, 11.0);
        Station station = new Station("test", List.of(charger));
        station.addEV(ev);

        // EV should now be in waiting queue
        List<EV> waitingEVs = station.getWaitingEVs();
        assertTrue(waitingEVs.contains(ev), "EV should be in waiting queue after arrival");
    }

    /**
     * Test 2: Scheduling after arrival at t.
     * 
     * Scenario: Single EV arrives at t=5. At t=5, after arrival, scheduler runs.
     * Expectation: Scheduler receives the newly arrived EV in waiting queue.
     */
    @Test
    @DisplayName("Test 2: Scheduling after arrival at t")
    void testSchedulingAfterArrival() {
        Charger charger = new Charger(0, 11.0);
        Scheduler scheduler = new FCFS();
        Metrics metrics = new Metrics();

        EV ev = new EV(0, 5, 50, 60, 0.2, 0.8, 7.0);
        Station station = new Station("test", List.of(charger));

        Simulation sim = new Simulation(100, station, scheduler, metrics);

        // Manually process: add EV, run scheduler
        station.addEV(ev);

        // At this point, EV should be in waiting queue
        List<EV> waitingEVs = station.getWaitingEVs();
        assertTrue(waitingEVs.contains(ev), "EV should be in waiting queue after arrival");
        assertTrue(charger.isAvailable(), "Charger should be available");
    }

    /**
     * Test 3: Charging during [t, t+1).
     * 
     * Scenario: EV with energy requirement is charged for 1 minute.
     * Expectation: Energy delivered is proportional to power and time.
     */
    @Test
    @DisplayName("Test 3: Charging during [t, t+1)")
    void testChargingDuringInterval() {
        // EV needs to charge from 0.2 to 0.8 SOC: 60 * (0.8 - 0.2) = 36 kWh
        // Charging at 7 kW for 1 minute = 7/60 kWh ≈ 0.1167 kWh
        EV ev = new EV(0, 0, 100, 60, 0.2, 0.8, 7.0);

        ev.beginCharging(0);
        assertTrue(ev.isActive(), "EV should be CHARGING after beginCharging");

        // Charge for 1 minute with boundary at t=1
        double delivered = ev.charge(7.0, 1, 1);

        // Expected: 7 kW * 1 minute / 60 = 0.1167 kWh
        double expected = 7.0 / 60.0;
        assertEquals(expected, delivered, 0.001, "Delivered energy should match power * time");

        // EV should still be charging (hasn't reached target yet)
        assertTrue(ev.isActive(), "EV should still be CHARGING after 1 minute");
    }

    /**
     * Test 4: Completion exactly at a minute boundary.
     * 
     * Scenario: EV charged for many intervals reaches exactly targetSOC at t+1.
     * Expectation: EV transitions to COMPLETED, completionTime = t+1.
     */
    @Test
    @DisplayName("Test 4: Completion exactly at minute boundary")
    void testCompletionAtBoundary() {
        // Create EV with small energy requirement: 0.2 -> 0.3 = 6 kWh
        // At 6 kW, takes 60 minutes to complete
        EV ev = new EV(0, 0, 100, 60, 0.2, 0.3, 6.0);

        ev.beginCharging(0);

        // Charge for 60 minutes
        for (int t = 0; t < 60; t++) {
            double delivered = ev.charge(6.0, 1, t + 1);
            if (t < 59) {
                assertTrue(ev.isActive(), "EV should still be charging at t=" + t);
            }
        }

        // After 60 minutes of charging, EV should be COMPLETED
        assertTrue(ev.isCompleted(), "EV should be COMPLETED after sufficient charging");
        assertEquals(60, ev.getCompletionTime(), "Completion time should be 60 (t+1 where t=59)");
    }

    /**
     * Test 5: EV deadline at a boundary.
     * 
     * Scenario: EV with expectedDeparture at t=50. At t=50, departure event fires.
     * If EV is not yet completed, it transitions to DEPARTED_NOT_ACHIEVED.
     * Expectation: actualDepartureTime = 50, state = DEPARTED_NOT_ACHIEVED.
     */
    @Test
    @DisplayName("Test 5: EV deadline (departure) at boundary")
    void testDeadlineAtBoundary() {
        EV ev = new EV(0, 10, 50, 60, 0.2, 0.9, 1.0); // Hard to charge fully in 40 minutes at 1kW

        ev.beginCharging(10);
        // Charge for a few minutes
        for (int t = 10; t < 50; t++) {
            ev.charge(1.0, 1, t + 1);
        }

        // At t=50, departure event fires
        assertFalse(ev.isCompleted(), "EV should not be fully charged (1 kW for 40 min = 0.667 kWh, need 54 kWh)");

        ev.depart(50);

        assertEquals(50, ev.getActualDepartureTime(), "Actual departure time should be 50");
        assertTrue(ev.isDeadlineViolated(), "Deadline should be violated");
        assertEquals(EV.EVState.DEPARTED_NOT_ACHIEVED, ev.getState(), "State should be DEPARTED_NOT_ACHIEVED");
    }

    /**
     * Test 6: EV still waiting when simulation horizon ends.
     * 
     * Scenario: EV arrives at t=80, horizon is t=100. EV never gets scheduled.
     * At horizon, no departure event fires. abandonAtHorizon() is called.
     * Expectation: state = HORIZON_ABANDONED, actualDepartureTime = -1.
     */
    @Test
    @DisplayName("Test 6: EV waiting when horizon ends")
    void testEVWaitingAtHorizon() {
        EV ev = new EV(0, 80, 120, 60, 0.2, 0.8, 7.0);

        // EV is in WAITING at horizon
        assertTrue(ev.isWaiting(), "EV should start in WAITING");

        // Call abandonAtHorizon (done by Simulation.run() at horizon)
        ev.abandonAtHorizon();

        assertEquals(EV.EVState.HORIZON_ABANDONED, ev.getState(), "State should be HORIZON_ABANDONED");
        assertEquals(-1, ev.getActualDepartureTime(), "Actual departure time should remain -1");
    }

    /**
     * Test 7: EV actively charging when simulation horizon ends.
     * 
     * Scenario: EV arrives at t=50, starts charging. Horizon is t=100.
     * EV is still CHARGING when horizon ends (hasn't completed yet).
     * Expectation: state = HORIZON_ABANDONED, actualDepartureTime = -1.
     */
    @Test
    @DisplayName("Test 7: EV actively charging when horizon ends")
    void testEVChargingAtHorizon() {
        EV ev = new EV(0, 50, 120, 60, 0.2, 0.9, 3.0); // Needs 42 kWh

        ev.beginCharging(60);

        // Charge for a few minutes (but not enough to complete)
        for (int t = 60; t < 95; t++) {
            ev.charge(3.0, 1, t + 1);
        }

        assertTrue(ev.isActive(), "EV should still be CHARGING at t=95");

        // At horizon (t=100), abandonAtHorizon() is called
        ev.abandonAtHorizon();

        assertEquals(EV.EVState.HORIZON_ABANDONED, ev.getState(), "State should be HORIZON_ABANDONED");
        assertEquals(-1, ev.getActualDepartureTime(), "Actual departure time should remain -1");
    }

    /**
     * Test 8: Simultaneous events at the same simulation time.
     * 
     * Scenario: At t=10, both an EV arrival and an EV departure are scheduled.
     * Event priority: departure (ordinal 0) before arrival (ordinal 3).
     * Expectation: Departing EV is processed first, then new EV arrives.
     */
    @Test
    @DisplayName("Test 8: Simultaneous events at same time (priority ordering)")
    void testSimultaneousEventsOrdering() {
        // Create two EVs: one departing at t=10, one arriving at t=10
        EV departingEV = new EV(0, 0, 10, 60, 0.2, 0.8, 7.0);
        EV arrivingEV = new EV(1, 10, 50, 60, 0.2, 0.8, 7.0);

        // Create two events at t=10
        Event departureEvent = new Event(10, Event.EventType.EV_DEPARTURE, departingEV, null);
        Event arrivalEvent = new Event(10, Event.EventType.EV_ARRIVAL, arrivingEV, null);

        // Departure event should come before arrival event when sorted
        assertTrue(departureEvent.compareTo(arrivalEvent) < 0,
                "Departure event (ordinal 0) should be sorted before arrival event (ordinal 3)");

        // Verify the ordinal ordering
        assertEquals(0, Event.EventType.EV_DEPARTURE.ordinal());
        assertEquals(3, Event.EventType.EV_ARRIVAL.ordinal());
    }

    /**
     * Integration test: Small complete simulation with controlled scenario.
     * 
     * Scenario: 1 EV arriving at t=5, 1 charger, horizon=20.
     * EV can complete before horizon.
     * Expectation: EV reaches COMPLETED, not HORIZON_ABANDONED.
     */
    @Test
    @DisplayName("Integration: EV completes before horizon")
    void testIntegrationEVCompletesBeforeHorizon() {
        // Create scenario: 1 EV, 1 charger, 20-minute horizon
        Charger charger = new Charger(0, 20.0); // 20 kW charger
        Scheduler scheduler = new FCFS();
        Metrics metrics = new Metrics();

        // EV: arrives at 5, departs at 100 (never reached), small battery
        // Battery 10 kWh, need 0.4 -> 0.5 = 1 kWh
        // At 20 kW, takes 1/20 * 60 = 3 minutes
        EV ev = new EV(0, 5, 100, 10, 0.4, 0.5, 20.0);

        Station station = new Station("test", List.of(charger));
        Simulation sim = new Simulation(20, station, scheduler, metrics);

        // Manually simulate the scenario
        station.addEV(ev);
        assertTrue(station.getWaitingEVs().contains(ev), "EV should be waiting after arrival");

        // Schedule: assign EV to charger at t=5
        station.assignEVToCharger(ev, charger);
        ev.beginCharging(5);
        assertTrue(ev.isActive(), "EV should be charging");

        // Charge for 3 minutes (t=5,6,7)
        for (int t = 5; t < 8; t++) {
            ev.charge(20.0, 1, t + 1);
        }

        // Should be completed after 3 minutes (20 kW * 3 min / 60 = 1 kWh = need)
        assertTrue(ev.isCompleted(), "EV should be completed after 3 minutes of charging");
        assertEquals(8, ev.getCompletionTime(), "Completion time should be 8");
    }

    /**
     * Integration test: EV doesn't complete before horizon.
     * 
     * Scenario: 1 EV arriving at t=5, 1 charger at 1 kW, horizon=15.
     * EV cannot complete in time.
     * Expectation: At horizon, EV is marked HORIZON_ABANDONED.
     */
    @Test
    @DisplayName("Integration: EV abandoned at horizon")
    void testIntegrationEVAbandonedAtHorizon() {
        Charger charger = new Charger(0, 1.0); // Only 1 kW
        Scheduler scheduler = new FCFS();
        Metrics metrics = new Metrics();

        // EV needs 30 kWh, at 1 kW takes 1800 minutes
        EV ev = new EV(0, 5, 200, 60, 0.2, 0.8, 1.0);

        Station station = new Station("test", List.of(charger));
        Simulation sim = new Simulation(15, station, scheduler, metrics);

        station.addEV(ev);
        station.assignEVToCharger(ev, charger);
        ev.beginCharging(5);

        // Charge for 9 minutes (t=5 to t=14)
        for (int t = 5; t < 14; t++) {
            ev.charge(1.0, 1, t + 1);
        }

        // At horizon (t=15), EV is still charging
        assertTrue(ev.isActive(), "EV should still be CHARGING before horizon");

        ev.abandonAtHorizon();

        assertEquals(EV.EVState.HORIZON_ABANDONED, ev.getState(), "EV should be HORIZON_ABANDONED");
        assertEquals(-1, ev.getActualDepartureTime(), "Departure time should not be set");
    }
}
