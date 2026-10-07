package scheduler;

import model.Charger;
import model.EV;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SchedulingDecisionTest {
    @Test
    void testValidDecision() {
        EV ev = new EV(1, 0, 100, 50.0, 0.2, 0.8, 11.0);
        Charger charger = new Charger(1, 50.0);
        SchedulingDecision decision = new SchedulingDecision(ev, charger, 11.0);
        assertEquals(ev, decision.getEV());
        assertEquals(charger, decision.getCharger());
        assertEquals(11.0, decision.getAllocatedPowerKW());
    }

    @Test
    void testInvalidNullEV() {
        Charger charger = new Charger(1, 50.0);
        assertThrows(IllegalArgumentException.class, () -> new SchedulingDecision(null, charger, 11.0));
    }

    @Test
    void testInvalidNullCharger() {
        EV ev = new EV(1, 0, 100, 50.0, 0.2, 0.8, 11.0);
        assertThrows(IllegalArgumentException.class, () -> new SchedulingDecision(ev, null, 11.0));
    }

    @Test
    void testInvalidPower() {
        EV ev = new EV(1, 0, 100, 50.0, 0.2, 0.8, 11.0);
        Charger charger = new Charger(1, 50.0);
        assertThrows(IllegalArgumentException.class, () -> new SchedulingDecision(ev, charger, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new SchedulingDecision(ev, charger, -1.0));
        
        // Power exceeds EV max
        assertThrows(IllegalArgumentException.class, () -> new SchedulingDecision(ev, charger, 12.0));
        
        // Power exceeds Charger max
        Charger weakCharger = new Charger(2, 7.0);
        assertThrows(IllegalArgumentException.class, () -> new SchedulingDecision(ev, weakCharger, 11.0));
    }
}
