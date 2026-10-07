package scheduler;

import model.Charger;
import model.EV;
import model.Station;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AdaptiveSchedulerTest {
    private Station station;
    private AdaptiveScheduler scheduler;

    @BeforeEach
    void setUp() {
        station = new Station("ST-1", List.of(
                new Charger(1, 50.0),
                new Charger(2, 50.0)
        ));
        scheduler = new AdaptiveScheduler();
    }

    @Test
    void testDemandLevel() {
        station = new Station("ST-1", List.of(new Charger(1, 50.0), new Charger(2, 50.0), new Charger(3, 50.0), new Charger(4, 50.0)));
        scheduler = new AdaptiveScheduler();
        
        // 0 waiting, 4 available -> LOW
        assertEquals(AdaptiveScheduler.DemandLevel.LOW, scheduler.assessDemandLevel(station.getWaitingEVs(), station.getAvailableChargers()));
        
        // 2 waiting, 4 available -> ratio 0.5 < 0.75 -> LOW
        station.addEV(new EV(1, 0, 10, 50.0, 0.2, 0.8, 11.0));
        station.addEV(new EV(2, 0, 10, 50.0, 0.2, 0.8, 11.0));
        assertEquals(AdaptiveScheduler.DemandLevel.LOW, scheduler.assessDemandLevel(station.getWaitingEVs(), station.getAvailableChargers()));
        
        // 4 waiting, 4 available -> ratio 1.0 -> NORMAL
        station.addEV(new EV(3, 0, 10, 50.0, 0.2, 0.8, 11.0));
        station.addEV(new EV(4, 0, 10, 50.0, 0.2, 0.8, 11.0));
        assertEquals(AdaptiveScheduler.DemandLevel.NORMAL, scheduler.assessDemandLevel(station.getWaitingEVs(), station.getAvailableChargers()));
        
        // 8 waiting, 4 available -> ratio 2.0 -> HIGH
        station.addEV(new EV(5, 0, 10, 50.0, 0.2, 0.8, 11.0));
        station.addEV(new EV(6, 0, 10, 50.0, 0.2, 0.8, 11.0));
        station.addEV(new EV(7, 0, 10, 50.0, 0.2, 0.8, 11.0));
        station.addEV(new EV(8, 0, 10, 50.0, 0.2, 0.8, 11.0));
        assertEquals(AdaptiveScheduler.DemandLevel.HIGH, scheduler.assessDemandLevel(station.getWaitingEVs(), station.getAvailableChargers()));
    }

    @Test
    void testBasicScheduling() {
        EV ev1 = new EV(1, 0, 10, 50.0, 0.2, 0.8, 11.0);
        EV ev2 = new EV(2, 0, 10, 50.0, 0.2, 0.8, 11.0);
        station.addEV(ev1);
        station.addEV(ev2);
        
        List<SchedulingDecision> decisions = scheduler.schedule(0, station);
        assertEquals(2, decisions.size());
    }

    @Test
    void testTieBreakById() {
        EV ev1 = new EV(2, 0, 10, 50.0, 0.2, 0.8, 11.0);
        EV ev2 = new EV(1, 0, 10, 50.0, 0.2, 0.8, 11.0);
        station.addEV(ev1);
        station.addEV(ev2);
        
        List<SchedulingDecision> decisions = scheduler.schedule(0, station);
        assertEquals(2, decisions.size());
        assertEquals(ev2, decisions.get(0).ev());
        assertEquals(ev1, decisions.get(1).ev());
    }
}
