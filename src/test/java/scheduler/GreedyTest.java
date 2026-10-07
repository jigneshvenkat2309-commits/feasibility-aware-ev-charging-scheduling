package scheduler;

import model.Charger;
import model.EV;
import model.Station;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GreedyTest {
    private Station station;
    private Greedy scheduler;

    @BeforeEach
    void setUp() {
        station = new Station("ST-1", List.of(
                new Charger(1, 50.0),
                new Charger(2, 50.0)
        ));
        scheduler = new Greedy();
    }

    @Test
    void testEmptyQueue() {
        assertTrue(scheduler.schedule(0, station).isEmpty());
    }

    @Test
    void testGreedyObjective() {
        // urgency = (energy / hours) / feasible
        // ev1: energy = 20, hours = 10/60 = 1/6. urgency = (20 / (1/6)) / 50 = 120 / 50 = 2.4
        EV ev1 = new EV(1, 0, 10, 100.0, 0.0, 0.2, 50.0);
        // ev2: energy = 40, hours = 10/60 = 1/6. urgency = (40 / (1/6)) / 50 = 240 / 50 = 4.8
        EV ev2 = new EV(2, 0, 10, 100.0, 0.0, 0.4, 50.0);
        
        station.addEV(ev1);
        station.addEV(ev2);
        
        List<SchedulingDecision> decisions = scheduler.schedule(0, station);
        assertEquals(2, decisions.size());
        // ev2 has higher urgency, should be scheduled first
        assertEquals(ev2, decisions.get(0).ev());
        assertEquals(ev1, decisions.get(1).ev());
    }

    @Test
    void testTieBreak() {
        // Same urgency
        EV ev1 = new EV(1, 0, 10, 100.0, 0.0, 0.2, 50.0);
        EV ev2 = new EV(2, 0, 10, 100.0, 0.0, 0.2, 50.0);
        
        station.addEV(ev2);
        station.addEV(ev1);
        
        List<SchedulingDecision> decisions = scheduler.schedule(0, station);
        assertEquals(2, decisions.size());
        // Tie breaks by ID
        assertEquals(ev1, decisions.get(0).ev());
        assertEquals(ev2, decisions.get(1).ev());
    }

    @Test
    void testNoDuplicateAssignment() {
        EV ev1 = new EV(1, 0, 10, 100.0, 0.0, 0.2, 50.0);
        EV ev2 = new EV(2, 0, 10, 100.0, 0.0, 0.4, 50.0);
        
        station = new Station("ST-1", List.of(new Charger(1, 50.0)));
        scheduler = new Greedy();
        
        station.addEV(ev1);
        station.addEV(ev2);
        
        List<SchedulingDecision> decisions = scheduler.schedule(0, station);
        assertEquals(1, decisions.size());
        // ev2 has higher urgency, gets the only charger
        assertEquals(ev2, decisions.get(0).ev());
        assertEquals(1, decisions.get(0).charger().getId());
    }
}
