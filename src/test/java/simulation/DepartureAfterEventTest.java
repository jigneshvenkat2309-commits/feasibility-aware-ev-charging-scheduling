package simulation;

import model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import scheduler.FCFS;
import scheduler.Scheduler;

import java.util.List;

import static model.EV.EVState.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for EV departure event processing.
 * Critical boundary: Once an EV_DEPARTURE event is processed at time t,
 * the EV must not receive any charging energy during [t, t+1).
 */
public class DepartureAfterEventTest {

    @Test
    @DisplayName("Test 1: EV departs incomplete, state locked at DEPARTED_NOT_ACHIEVED")
    void testDepartureLocksIncompleteEV() {
        Charger charger = new Charger(0, 40.0);
        Station station = new Station("test", List.of(charger));

        EV ev = new EV(0, 10, 40, 100, 0.0, 0.3, 40.0);
        station.addEV(ev);
        station.assignEVToCharger(ev, charger);
        ev.beginCharging(10);

        // Charge for 20 minutes: t=10 to t=30
        for (int t = 10; t < 30; t++) {
            ev.charge(40.0, 1, t + 1);
            if (ev.isCompleted()) {
                charger.releaseEV();
            }
        }

        assertTrue(ev.isActive(), "EV should still be CHARGING at t=30");

        // At t=40: Process EV_DEPARTURE event
        for (Charger c : station.getChargers()) {
            if (c.getCurrentEV() == ev) {
                c.releaseEV();
                break;
            }
        }
        ev.depart(40);

        assertEquals(DEPARTED_NOT_ACHIEVED, ev.getState(),
                "EV should be in DEPARTED_NOT_ACHIEVED state");
        assertEquals(40, ev.getActualDepartureTime(), "actualDepartureTime should be 40");
        assertFalse(ev.isActive(), "EV should not be CHARGING");
        assertEquals(-1, ev.getCompletionTime(), "completionTime should be -1");
        assertTrue(ev.isDeadlineViolated(), "deadlineViolated should be true");

        // Attempt charge after departure - should deliver 0 energy
        double energy = ev.charge(40.0, 1, 41);
        assertEquals(0, energy, 0.001, "No energy should be delivered");
        assertEquals(DEPARTED_NOT_ACHIEVED, ev.getState(), "State should remain DEPARTED_NOT_ACHIEVED");
        assertEquals(-1, ev.getCompletionTime(), "completionTime should remain -1");
    }

    @Test
    @DisplayName("Test 2: Completed EV with later departure remains COMPLETED")
    void testCompletedEVWithLaterDeparture() {
        Charger charger = new Charger(0, 30.0);
        Station station = new Station("test", List.of(charger));

        EV ev = new EV(0, 10, 40, 100, 0.0, 0.1, 30.0);
        station.addEV(ev);
        station.assignEVToCharger(ev, charger);
        ev.beginCharging(10);

        // Charge for 20 minutes: t=10 to t=30 (completes)
        for (int t = 10; t < 30; t++) {
            ev.charge(30.0, 1, t + 1);
            if (ev.isCompleted()) {
                charger.releaseEV();
            }
        }

        assertTrue(ev.isCompleted(), "EV should be COMPLETED");
        assertEquals(30, ev.getCompletionTime(), "Completion time should be 30");
        assertFalse(ev.isDeadlineViolated(), "No deadline violation");

        // At t=40: Process departure - EV already COMPLETED
        ev.depart(40);

        assertEquals(COMPLETED, ev.getState(), "State should remain COMPLETED");
        assertEquals(30, ev.getCompletionTime(), "Completion time should remain 30");
        assertEquals(40, ev.getActualDepartureTime(), "actualDepartureTime should be 40");
        assertFalse(ev.isDeadlineViolated(), "No deadline violation");
    }

    @Test
    @DisplayName("Test 3: Departed EV not in waiting queue after release")
    void testDepartedEVNotRescheduled() {
        Charger charger = new Charger(0, 20.0);
        Station station = new Station("test", List.of(charger));

        EV ev = new EV(0, 10, 50, 100, 0.0, 0.5, 20.0);
        station.addEV(ev);
        station.assignEVToCharger(ev, charger);
        ev.beginCharging(40);

        // Charge for 8 minutes
        for (int t = 40; t < 48; t++) {
            ev.charge(20.0, 1, t + 1);
        }

        assertTrue(ev.isActive(), "EV should be CHARGING");
        assertTrue(station.getWaitingEVs().isEmpty(), "No waiting EVs");

        // Departure event: release and mark departed
        for (Charger c : station.getChargers()) {
            if (c.getCurrentEV() == ev) {
                c.releaseEV();
                break;
            }
        }
        ev.depart(50);
        station.refreshWaitingQueue();

        assertTrue(station.getAllEVs().contains(ev), "EV still in station");
        assertFalse(station.getWaitingEVs().contains(ev), "EV not in waiting queue");
        assertTrue(charger.isAvailable(), "Charger available");
    }

    @Test
    @DisplayName("Test 4: Departed EV charge() returns 0 (guard: not CHARGING)")
    void testDepartedEVCannotReceiveEnergy() {
        EV ev = new EV(0, 10, 50, 100, 0.0, 0.5, 20.0);
        ev.depart(50);

        assertEquals(DEPARTED_NOT_ACHIEVED, ev.getState(), "State should be DEPARTED_NOT_ACHIEVED");
        assertFalse(ev.isActive(), "isActive() should be false");

        // Attempt charge - guard condition prevents energy delivery
        double energy = ev.charge(20.0, 1, 51);
        assertEquals(0, energy, 0.001, "No energy delivered to departed EV");
    }

    @Test
    @DisplayName("Test 5: Departed EV cannot complete despite charge attempts")
    void testDepartedEVCannotComplete() {
        EV ev = new EV(0, 10, 30, 100, 0.0, 0.01, 50.0);
        ev.depart(30);

        assertEquals(DEPARTED_NOT_ACHIEVED, ev.getState(), "State should be DEPARTED_NOT_ACHIEVED");

        // Try to charge multiple times - all should return 0
        double totalEnergy = 0;
        for (int i = 0; i < 10; i++) {
            totalEnergy += ev.charge(50.0, 1, 31 + i);
        }

        assertEquals(0, totalEnergy, 0.001, "No total energy delivered");
        assertEquals(DEPARTED_NOT_ACHIEVED, ev.getState(), "State remains DEPARTED_NOT_ACHIEVED");
        assertEquals(-1, ev.getCompletionTime(), "No completion time");
    }
}
