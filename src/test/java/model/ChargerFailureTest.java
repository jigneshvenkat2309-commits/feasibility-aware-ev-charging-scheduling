package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Charger failure consistency (Fix 1.2).
 * 
 * Policy:
 * - When a charger fails, any EV currently assigned must be released and
 * requeued by Simulation/Station before calling charger.fail().
 * - A failed charger cannot accept new EVs (isAvailable() returns false).
 * - Repairing a charger restores its availability.
 */
public class ChargerFailureTest {

    @Test
    @DisplayName("Fix 1.2-1: Failing an idle charger")
    void testFailIdleCharger() {
        Charger charger = new Charger(0, 20.0);

        assertTrue(charger.isAvailable(), "Idle charger should be available");
        assertNull(charger.getCurrentEV(), "Idle charger should have no EV");

        charger.fail();

        assertFalse(charger.isAvailable(), "Failed charger should not be available");
        assertTrue(charger.isFailed(), "Charger should be marked as failed");
        assertNull(charger.getCurrentEV(), "Failed charger should still have no EV");
    }

    @Test
    @DisplayName("Fix 1.2-2: Failing an occupied charger (pre-cleanup required)")
    void testFailOccupiedChargerWithPreCleanup() {
        Charger charger = new Charger(0, 20.0);
        EV ev = new EV(0, 10, 50, 100, 0.0, 0.5, 20.0);

        charger.assignEV(ev);
        assertTrue(charger.isAvailable() == false, "Occupied charger should not be available");
        assertEquals(ev, charger.getCurrentEV(), "Charger should have EV");

        // Before failing, must clean up (done by Simulation)
        charger.releaseEV();
        ev.returnToWaiting();

        // Now fail
        charger.fail();

        assertFalse(charger.isAvailable(), "Failed charger should not be available");
        assertTrue(charger.isFailed(), "Charger should be marked as failed");
        assertNull(charger.getCurrentEV(), "Charger should have no EV after cleanup");
    }

    @Test
    @DisplayName("Fix 1.2-3: Failed charger cannot accept new EV")
    void testFailedChargerCannotAcceptEV() {
        Charger charger = new Charger(0, 20.0);
        EV ev = new EV(0, 10, 50, 100, 0.0, 0.5, 20.0);

        charger.fail();

        assertFalse(charger.isAvailable(), "Failed charger should not be available");

        // Attempting to assign should fail
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> charger.assignEV(ev),
                "Cannot assign EV to failed charger");

        assertTrue(ex.getMessage().contains("unavailable"), "Exception should mention unavailability");
        assertNull(charger.getCurrentEV(), "Charger should still have no EV");
    }

    @Test
    @DisplayName("Fix 1.2-4: Charger repair restores availability")
    void testChargerRepairRestoresAvailability() {
        Charger charger = new Charger(0, 20.0);

        charger.fail();
        assertFalse(charger.isAvailable(), "Failed charger not available");

        charger.repair();

        assertTrue(charger.isAvailable(), "Repaired charger should be available");
        assertFalse(charger.isFailed(), "Charger should no longer be marked as failed");
    }

    @Test
    @DisplayName("Fix 1.2-5: EV can be assigned to repaired charger")
    void testEVCanChargeAfterChargerRepair() {
        Charger charger = new Charger(0, 20.0);
        EV ev = new EV(0, 10, 50, 100, 0.0, 0.5, 20.0);

        charger.fail();
        assertFalse(charger.isAvailable(), "Failed charger not available");

        charger.repair();
        assertTrue(charger.isAvailable(), "Repaired charger should be available");

        // Should now be able to assign
        charger.assignEV(ev);
        assertEquals(ev, charger.getCurrentEV(), "EV should be assigned to repaired charger");
    }

    @Test
    @DisplayName("Fix 1.2-6: Busy time persists across failure/repair")
    void testBusyTimePersistsAcrossFailureRepair() {
        Charger charger = new Charger(0, 20.0);

        charger.recordBusyTime(10);
        assertEquals(10, charger.getBusyTimeMinutes(), "Busy time should be 10");

        charger.fail();
        assertEquals(10, charger.getBusyTimeMinutes(), "Busy time should persist during failure");

        charger.repair();
        assertEquals(10, charger.getBusyTimeMinutes(), "Busy time should persist after repair");

        charger.recordBusyTime(5);
        assertEquals(15, charger.getBusyTimeMinutes(), "Busy time should accumulate after repair");
    }

    @Test
    @DisplayName("Fix 1.2-7: Multiple chargers can fail independently")
    void testMultipleChargersFailIndependently() {
        Charger charger1 = new Charger(0, 20.0);
        Charger charger2 = new Charger(1, 20.0);

        assertTrue(charger1.isAvailable(), "charger1 initially available");
        assertTrue(charger2.isAvailable(), "charger2 initially available");

        charger1.fail();

        assertFalse(charger1.isAvailable(), "charger1 should be failed");
        assertTrue(charger2.isAvailable(), "charger2 should still be available");

        charger2.fail();

        assertFalse(charger1.isAvailable(), "charger1 should still be failed");
        assertFalse(charger2.isAvailable(), "charger2 should now be failed");
    }
}
