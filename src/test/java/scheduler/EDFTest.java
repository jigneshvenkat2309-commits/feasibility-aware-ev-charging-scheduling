package scheduler;

import model.Charger;
import model.EV;
import model.Station;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EDFTest {
    private Station station;
    private EDF scheduler;

    @BeforeEach
    void setUp() {
        station = new Station("ST-1", List.of(
                new Charger(1, 50.0),
                new Charger(2, 50.0)
        ));
        scheduler = new EDF();
    }

    @Test
    void testEmptyQueue() {
        assertTrue(scheduler.schedule(0, station).isEmpty());
    }

    @Test
    void testOrderingByDeadline() {
        EV ev1 = new EV(1, 0, 20, 50.0, 0.2, 0.8, 11.0); // departs at 20
        EV ev2 = new EV(2, 5, 15, 50.0, 0.2, 0.8, 11.0); // departs at 15
        station.addEV(ev1);
        station.addEV(ev2);
        
        List<SchedulingDecision> decisions = scheduler.schedule(10, station);
        assertEquals(2, decisions.size());
        assertEquals(ev2, decisions.get(0).ev());
        assertEquals(ev1, decisions.get(1).ev());
    }

    @Test
    void testTieBreakByArrivalTime() {
        EV ev1 = new EV(1, 10, 20, 50.0, 0.2, 0.8, 11.0); 
        EV ev2 = new EV(2, 5, 20, 50.0, 0.2, 0.8, 11.0); // same departure, earlier arrival
        station.addEV(ev1);
        station.addEV(ev2);
        
        List<SchedulingDecision> decisions = scheduler.schedule(10, station);
        assertEquals(2, decisions.size());
        assertEquals(ev2, decisions.get(0).ev());
        assertEquals(ev1, decisions.get(1).ev());
    }

    @Test
    void testTieBreakById() {
        EV ev1 = new EV(2, 5, 20, 50.0, 0.2, 0.8, 11.0); 
        EV ev2 = new EV(1, 5, 20, 50.0, 0.2, 0.8, 11.0); // same departure, same arrival, lower ID
        station.addEV(ev1);
        station.addEV(ev2);
        
        List<SchedulingDecision> decisions = scheduler.schedule(10, station);
        assertEquals(2, decisions.size());
        assertEquals(ev2, decisions.get(0).ev());
        assertEquals(ev1, decisions.get(1).ev());
    }
}
