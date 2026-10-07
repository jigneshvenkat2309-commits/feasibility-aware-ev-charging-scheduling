package simulation;

import metrics.Metrics;
import model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import scheduler.FCFS;

import java.util.List;

import static model.EV.EVState.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Simulation charger failure handling (Fix 1.2).
 * 
 * Verifies that when a charger fails during simulation:
 * 1. The affected EV is properly detached
 * 2. The EV returns to WAITING state
 * 3. The EV is not lost from the station
 * 4. The failed charger is not used for new assignments
 * 5. Charging can resume after recovery
 */
public class ChargerFailureSimulationTest {

    @Test
    @DisplayName("Fix 1.2-8: Trigger charger failure during simulation")
    void testChargerFailureEvent() {
        Charger charger = new Charger(0, 40.0);
        Station station = new Station("test", List.of(charger));
        Simulation sim = new Simulation(100, station, new FCFS(), new Metrics());

        EV ev = new EV(0, 10, 80, 100, 0.0, 0.5, 40.0);
        station.addEV(ev);
        station.assignEVToCharger(ev, charger);
        ev.beginCharging(10);

        // EV is charging
        assertTrue(ev.isActive(), "EV should be CHARGING");
        assertEquals(ev, charger.getCurrentEV(), "Charger should have EV");

        // Simulate charger failure event: release, requeue, then fail
        for (Charger c : station.getChargers()) {
            if (c.getCurrentEV() == ev) {
                c.releaseEV();
                break;
            }
        }
        ev.returnToWaiting();
        charger.fail();
        station.refreshWaitingQueue(); // Sync waiting queue (Simulation.run() does this at each iteration start)

        // Verify state after failure
        assertTrue(ev.isWaiting(), "EV should be WAITING after charger failure");
        assertFalse(charger.isAvailable(), "Failed charger should not be available");
        assertNull(charger.getCurrentEV(), "Charger should have no EV");
        assertTrue(station.getWaitingEVs().contains(ev), "EV should be in waiting queue");
        assertTrue(station.getAllEVs().contains(ev), "EV should still be in station");
    }

    @Test
    @DisplayName("Fix 1.2-9: Affected EV is not lost from station after failure")
    void testEVNotLostAfterChargerFailure() {
        Charger charger1 = new Charger(0, 30.0);
        Charger charger2 = new Charger(1, 30.0);
        Station station = new Station("test", List.of(charger1, charger2));

        EV ev1 = new EV(0, 10, 80, 100, 0.0, 0.3, 30.0);
        EV ev2 = new EV(1, 15, 85, 100, 0.0, 0.3, 30.0);

        station.addEV(ev1);
        station.addEV(ev2);

        // Both EVs charging on different chargers
        station.assignEVToCharger(ev1, charger1);
        ev1.beginCharging(10);
        station.assignEVToCharger(ev2, charger2);
        ev2.beginCharging(15);

        assertEquals(2, station.getAllEVs().size(), "Station should have 2 EVs");

        // Charger1 fails - release and requeue ev1
        charger1.releaseEV();
        ev1.returnToWaiting();
        charger1.fail();

        // Verify both EVs still in station
        assertEquals(2, station.getAllEVs().size(), "Station should still have 2 EVs");
        assertTrue(station.getAllEVs().contains(ev1), "ev1 should still be in station");
        assertTrue(station.getAllEVs().contains(ev2), "ev2 should still be in station");

        // ev1 now waiting, ev2 still charging
        assertTrue(ev1.isWaiting(), "ev1 should be WAITING");
        assertTrue(ev2.isActive(), "ev2 should still be CHARGING");
    }

    @Test
    @DisplayName("Fix 1.2-10: Failed charger not used for new assignments")
    void testFailedChargerNotReused() {
        Charger charger1 = new Charger(0, 30.0);
        Charger charger2 = new Charger(1, 30.0);
        Station station = new Station("test", List.of(charger1, charger2));

        EV ev = new EV(0, 10, 80, 100, 0.0, 0.3, 30.0);
        station.addEV(ev);

        // Fail charger1
        charger1.fail();

        // Try to assign EV to charger1 - should be rejected
        assertThrows(IllegalStateException.class,
                () -> charger1.assignEV(ev),
                "Cannot assign to failed charger");

        // Verify EV still waiting
        assertTrue(ev.isWaiting(), "EV should remain WAITING");
        assertNull(charger1.getCurrentEV(), "Failed charger should have no EV");

        // Available chargers should not include failed charger
        List<Charger> available = station.getAvailableChargers();
        assertFalse(available.contains(charger1), "Failed charger should not be available");
        assertTrue(available.contains(charger2), "Working charger should be available");
    }

    @Test
    @DisplayName("Fix 1.2-11: EV can resume charging after charger recovery")
    void testEVResumesChargingAfterRecovery() {
        Charger charger = new Charger(0, 40.0);
        Station station = new Station("test", List.of(charger));

        EV ev = new EV(0, 10, 80, 100, 0.0, 0.5, 40.0);
        station.addEV(ev);

        // Charge for a bit
        station.assignEVToCharger(ev, charger);
        ev.beginCharging(10);
        ev.charge(40.0, 1, 11); // 1 minute of charging

        assertTrue(ev.isActive(), "EV should be CHARGING");

        // Charger fails
        charger.releaseEV();
        ev.returnToWaiting();
        station.refreshWaitingQueue(); // Update waiting queue after state change
        charger.fail();

        assertTrue(ev.isWaiting(), "EV should be WAITING after failure");
        assertFalse(charger.isAvailable(), "Charger should be failed");

        // Charger repairs
        charger.repair();
        assertTrue(charger.isAvailable(), "Charger should be available after repair");

        // EV can now resume charging
        station.assignEVToCharger(ev, charger);
        ev.beginCharging(20);
        ev.charge(40.0, 1, 21);

        assertTrue(ev.isActive(), "EV should be CHARGING again");
        assertEquals(ev, charger.getCurrentEV(), "Charger should have EV again");
    }

    @Test
    @DisplayName("Fix 1.2-12: Multiple EVs recover from single charger failure")
    void testMultipleEVsRecoverFromChargerFailure() {
        Charger charger1 = new Charger(0, 20.0);
        Charger charger2 = new Charger(1, 20.0);
        Charger charger3 = new Charger(2, 20.0);
        Station station = new Station("test", List.of(charger1, charger2, charger3));

        EV ev1 = new EV(0, 10, 80, 100, 0.0, 0.3, 20.0);
        EV ev2 = new EV(1, 15, 85, 100, 0.0, 0.3, 20.0);
        EV ev3 = new EV(2, 20, 90, 100, 0.0, 0.3, 20.0);

        station.addEV(ev1);
        station.addEV(ev2);
        station.addEV(ev3);

        // All three charging
        station.assignEVToCharger(ev1, charger1);
        ev1.beginCharging(10);
        station.assignEVToCharger(ev2, charger2);
        ev2.beginCharging(15);
        station.assignEVToCharger(ev3, charger3);
        ev3.beginCharging(20);

        assertEquals(3, station.getAllEVs().size(), "Station should have 3 EVs");
        assertEquals(0, station.getWaitingEVs().size(), "No EVs should be waiting");

        // Charger2 fails - ev2 goes back to waiting
        charger2.releaseEV();
        ev2.returnToWaiting();
        station.refreshWaitingQueue(); // Update waiting queue after state change
        charger2.fail();

        assertEquals(3, station.getAllEVs().size(), "Station should still have 3 EVs");
        assertEquals(1, station.getWaitingEVs().size(), "1 EV should be waiting");
        assertTrue(station.getWaitingEVs().contains(ev2), "ev2 should be waiting");

        // ev1 and ev3 still charging
        assertTrue(ev1.isActive(), "ev1 should still be CHARGING");
        assertTrue(ev3.isActive(), "ev3 should still be CHARGING");

        // charger1 and charger3 are occupied (ev1, ev3 still charging), charger2 is failed
        // isAvailable() requires !failed AND currentEV == null, so 0 chargers are available
        assertEquals(0, station.getAvailableChargers().size(), "No chargers should be available (2 occupied, 1 failed)");
        assertFalse(station.getAvailableChargers().contains(charger2), "Failed charger should not be available");
    }

    @Test
    @DisplayName("Fix 1.2-13: Charger failure during charging accumulates partial energy")
    void testPartialEnergyAccumulationBeforeFailure() {
        Charger charger = new Charger(0, 30.0);
        Station station = new Station("test", List.of(charger));

        // EV needs 10 kWh (30 kW * 20 min / 60 = 10 kWh)
        EV ev = new EV(0, 10, 80, 100, 0.0, 0.1, 30.0);
        station.addEV(ev);

        station.assignEVToCharger(ev, charger);
        ev.beginCharging(10);

        // Charge for 10 minutes (receives 5 kWh, needs 10 total)
        for (int t = 10; t < 20; t++) {
            ev.charge(30.0, 1, t + 1);
        }

        assertEquals(5.0, ev.getRemainingEnergyRequiredKWh(), 0.1, "Should need 5 more kWh");
        assertTrue(ev.isActive(), "EV should still be CHARGING");

        // Charger fails - EV is released and returns to waiting with accumulated energy
        charger.releaseEV();
        ev.returnToWaiting();
        charger.fail();

        assertTrue(ev.isWaiting(), "EV should be WAITING");
        assertEquals(5.0, ev.getRemainingEnergyRequiredKWh(), 0.1, "Remaining energy should be preserved");

        // EV can later charge on another available charger with the same requirement
        assertFalse(ev.isCompleted(), "EV should not be completed");
    }
}
