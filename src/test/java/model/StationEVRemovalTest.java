package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Station EV removal consistency (Fix 1.1).
 * 
 * Invariant: If an EV is removed from the station, no charger may continue
 * referencing that EV.
 */
public class StationEVRemovalTest {

    @Test
    @DisplayName("Fix 1.1-1: Removing a WAITING EV succeeds")
    void testRemoveWaitingEVSucceeds() {
        Charger charger = new Charger(0, 20.0);
        Station station = new Station("test", List.of(charger));

        EV ev = new EV(0, 10, 50, 100, 0.0, 0.5, 20.0);
        station.addEV(ev);

        assertTrue(ev.isWaiting(), "EV should be WAITING");
        assertTrue(station.getWaitingEVs().contains(ev), "EV should be in waiting queue");

        // Remove should succeed for WAITING EV
        station.removeEV(ev);

        assertFalse(station.getAllEVs().contains(ev), "EV should be removed from station");
        assertFalse(station.getWaitingEVs().contains(ev), "EV should be removed from waiting queue");
    }

    @Test
    @DisplayName("Fix 1.1-2: Removing a CHARGING EV fails with clear exception")
    void testRemoveChargingEVFails() {
        Charger charger = new Charger(0, 20.0);
        Station station = new Station("test", List.of(charger));

        EV ev = new EV(0, 10, 50, 100, 0.0, 0.5, 20.0);
        station.addEV(ev);

        // Assign EV to charger and start charging
        station.assignEVToCharger(ev, charger);
        ev.beginCharging(10);

        assertTrue(ev.isActive(), "EV should be CHARGING");
        assertEquals(ev, charger.getCurrentEV(), "Charger should have the EV");

        // Attempt to remove should fail
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> station.removeEV(ev),
                "Removing a CHARGING EV should throw exception");

        assertTrue(ex.getMessage().contains("CHARGING"), "Exception should mention CHARGING state");
        assertTrue(station.getAllEVs().contains(ev), "EV should still be in station");
    }

    @Test
    @DisplayName("Fix 1.1-3: After removal, no charger references the removed EV")
    void testNoChargerReferencesRemovedEV() {
        Charger charger1 = new Charger(0, 20.0);
        Charger charger2 = new Charger(1, 20.0);
        Station station = new Station("test", List.of(charger1, charger2));

        EV ev1 = new EV(0, 10, 50, 100, 0.0, 0.5, 20.0);
        EV ev2 = new EV(1, 15, 60, 100, 0.0, 0.5, 20.0);

        station.addEV(ev1);
        station.addEV(ev2);

        // Assign different EVs to different chargers
        station.assignEVToCharger(ev1, charger1);
        ev1.beginCharging(10);
        station.assignEVToCharger(ev2, charger2);
        ev2.beginCharging(15);

        // Release ev1 from charger1 and remove it
        charger1.releaseEV();
        ev1.depart(50); // Mark as departed so it's no longer ACTIVE

        // Now removal should succeed
        station.removeEV(ev1);

        // Verify no charger has ev1
        for (Charger charger : station.getChargers()) {
            assertNotEquals(ev1, charger.getCurrentEV(), "No charger should have removed EV");
        }

        // Verify ev2 is unaffected
        assertEquals(ev2, charger2.getCurrentEV(), "ev2 should still be on charger2");
    }

    @Test
    @DisplayName("Fix 1.1-4: Station collections remain consistent after removal")
    void testStationConsistencyAfterRemoval() {
        Charger charger = new Charger(0, 20.0);
        Station station = new Station("test", List.of(charger));

        EV ev1 = new EV(0, 10, 50, 100, 0.0, 0.5, 20.0);
        EV ev2 = new EV(1, 15, 60, 100, 0.0, 0.5, 20.0);
        EV ev3 = new EV(2, 20, 70, 100, 0.0, 0.5, 20.0);

        station.addEV(ev1);
        station.addEV(ev2);
        station.addEV(ev3);

        assertEquals(3, station.getAllEVs().size(), "Should have 3 EVs");
        assertEquals(3, station.getWaitingEVs().size(), "Should have 3 waiting EVs");

        // Remove ev2
        station.removeEV(ev2);

        assertEquals(2, station.getAllEVs().size(), "Should have 2 EVs after removal");
        assertEquals(2, station.getWaitingEVs().size(), "Should have 2 waiting EVs after removal");
        assertTrue(station.getAllEVs().contains(ev1), "ev1 should still be present");
        assertTrue(station.getAllEVs().contains(ev3), "ev3 should still be present");
        assertFalse(station.getAllEVs().contains(ev2), "ev2 should be removed");
    }
}
