package scheduler;

import model.Charger;
import model.EV;
import model.Station;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdaptiveSchedulerV2Test {
    private Station station;
    private AdaptiveSchedulerV2 scheduler;

    @BeforeEach
    void setUp() {
        station = new Station(3, 50.0);
        scheduler = new AdaptiveSchedulerV2();
    }

    @Test
    void testFeasiblePreferredOverInfeasible() {
        // EV 1: Infeasible (Needs 50kWh @ 50kW = 1 hour, but has 30 mins left)
        EV evInfeasible = new EV(1, 0, 30, 50.0, 0.0, 1.0, 50.0);
        
        // EV 2: Feasible (Needs 25kWh @ 50kW = 30 mins, has 60 mins left)
        EV evFeasible = new EV(2, 0, 60, 50.0, 0.5, 1.0, 50.0);

        Station smallStation = new Station(1, 50.0);
        smallStation.addEV(evInfeasible);
        smallStation.addEV(evFeasible);

        List<SchedulingDecision> decisions = scheduler.schedule(0, smallStation);
        assertEquals(1, decisions.size());
        assertEquals(2, decisions.get(0).ev().getId(), "Feasible EV should be prioritized over infeasible");
    }

    @Test
    void testAllInfeasibleDeterministicFallback() {
        // EV 1: Infeasible (deficit = 60 - 30 = 30 mins)
        EV ev1 = new EV(1, 0, 30, 50.0, 0.0, 1.0, 50.0);
        
        // EV 2: Infeasible (deficit = 120 - 60 = 60 mins)
        EV ev2 = new EV(2, 0, 60, 100.0, 0.0, 1.0, 50.0);

        Station smallStation = new Station(1, 50.0);
        smallStation.addEV(ev1);
        smallStation.addEV(ev2);

        List<SchedulingDecision> decisions = scheduler.schedule(0, smallStation);
        assertEquals(1, decisions.size());
        
        // ev1 has smaller deficit (30 vs 60), so ev1 should be chosen.
        assertEquals(1, decisions.get(0).ev().getId(), "Should pick the one with smaller feasibility deficit");
    }

    @Test
    void testEqualScoresDeterministicTieBreaking() {
        EV ev1 = new EV(1, 0, 60, 50.0, 0.5, 1.0, 50.0);
        EV ev2 = new EV(2, 0, 60, 50.0, 0.5, 1.0, 50.0);
        
        Station smallStation = new Station(1, 50.0);
        smallStation.addEV(ev1);
        smallStation.addEV(ev2);

        List<SchedulingDecision> decisions = scheduler.schedule(0, smallStation);
        assertEquals(1, decisions.size());
        assertEquals(1, decisions.get(0).ev().getId(), "Should break tie by ID");
    }

    @Test
    void testTwoFeasibleUsesAdaptiveScore() {
        // High urgency but feasible
        EV evUrgent = new EV(1, 0, 30, 50.0, 0.5, 1.0, 50.0);
        
        // Low urgency, feasible
        EV evRelaxed = new EV(2, 0, 120, 50.0, 0.5, 1.0, 50.0);
        
        Station smallStation = new Station(1, 50.0);
        smallStation.addEV(evUrgent);
        smallStation.addEV(evRelaxed);

        List<SchedulingDecision> decisions = scheduler.schedule(0, smallStation);
        assertEquals(1, decisions.size());
        assertEquals(1, decisions.get(0).ev().getId(), "Should pick urgent but feasible over relaxed");
    }
}
