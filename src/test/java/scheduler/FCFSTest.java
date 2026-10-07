package scheduler;

import model.Charger;
import model.EV;
import model.Station;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FCFSTest {
    private Station station;
    private FCFS scheduler;

    @BeforeEach
    void setUp() {
        station = new Station("ST-1", List.of(
                new Charger(1, 50.0),
                new Charger(2, 50.0)
        ));
        scheduler = new FCFS();
    }

    @Test
    void testEmptyQueue() {
        List<SchedulingDecision> decisions = scheduler.schedule(0, station);
        assertTrue(decisions.isEmpty());
    }

    @Test
    void testNoAvailableChargers() {
        EV ev = new EV(1, 0, 100, 50.0, 0.2, 0.8, 11.0);
        station.addEV(ev);
        station.getChargers().get(0).fail();
        station.getChargers().get(1).fail();
        List<SchedulingDecision> decisions = scheduler.schedule(0, station);
        assertTrue(decisions.isEmpty());
    }

    @Test
    void testOrderingByArrivalTime() {
        EV ev1 = new EV(1, 10, 100, 50.0, 0.2, 0.8, 11.0);
        EV ev2 = new EV(2, 5, 100, 50.0, 0.2, 0.8, 11.0); // arrived earlier
        station.addEV(ev1);
        station.addEV(ev2);
        
        List<SchedulingDecision> decisions = scheduler.schedule(15, station);
        assertEquals(2, decisions.size());
        assertEquals(ev2, decisions.get(0).ev());
        assertEquals(ev1, decisions.get(1).ev());
    }

    @Test
    void testTieBreakById() {
        EV ev1 = new EV(2, 10, 100, 50.0, 0.2, 0.8, 11.0);
        EV ev2 = new EV(1, 10, 100, 50.0, 0.2, 0.8, 11.0); // lower ID
        station.addEV(ev1);
        station.addEV(ev2);
        
        List<SchedulingDecision> decisions = scheduler.schedule(15, station);
        assertEquals(2, decisions.size());
        assertEquals(ev2, decisions.get(0).ev());
        assertEquals(ev1, decisions.get(1).ev());
    }

    @Test
    void testMoreEVsThanChargers() {
        EV ev1 = new EV(1, 0, 100, 50.0, 0.2, 0.8, 11.0);
        EV ev2 = new EV(2, 1, 100, 50.0, 0.2, 0.8, 11.0);
        EV ev3 = new EV(3, 2, 100, 50.0, 0.2, 0.8, 11.0);
        station.addEV(ev1);
        station.addEV(ev2);
        station.addEV(ev3);

        List<SchedulingDecision> decisions = scheduler.schedule(5, station);
        assertEquals(2, decisions.size()); // only 2 chargers
        assertEquals(ev1, decisions.get(0).ev());
        assertEquals(ev2, decisions.get(1).ev());
        // ev3 is not assigned
    }
}
