package scheduler;

import model.Charger;
import model.EV;
import model.Station;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Transparent urgency heuristic using required power divided by feasible power.
 */
public class Greedy implements Scheduler {
    public List<SchedulingDecision> schedule(int currentTime, Station station) {
        List<EV> remaining = new ArrayList<>(station.getWaitingEVs());
        List<Charger> chargers = new ArrayList<>(station.getAvailableChargers());
        List<SchedulingDecision> decisions = new ArrayList<>();
        while (!remaining.isEmpty() && !chargers.isEmpty()) {
            EV bestEV = null;
            Charger bestCharger = null;
            double bestUrgency = -1;
            for (EV ev : remaining)
                for (Charger charger : chargers) {
                    double feasible = Math.min(ev.getMaxChargingPowerKW(), charger.getMaxPowerKW());
                    double hours = Math.max(1, ev.getExpectedDepartureTime() - currentTime) / 60.0;
                    double urgency = (ev.getRemainingEnergyRequiredKWh() / hours) / feasible;
                    if (bestEV == null || urgency > bestUrgency + 1e-12
                            || (Math.abs(urgency - bestUrgency) <= 1e-12 && tie(ev, bestEV, currentTime) < 0)) {
                        bestEV = ev;
                        bestCharger = charger;
                        bestUrgency = urgency;
                    }
                }
            remaining.remove(bestEV);
            chargers.remove(bestCharger);
            decisions.add(new SchedulingDecision(bestEV, bestCharger,
                    Math.min(bestEV.getMaxChargingPowerKW(), bestCharger.getMaxPowerKW())));
        }
        return decisions;
    }

    private int tie(EV left, EV right, int time) {
        int c = Integer.compare(left.getExpectedDepartureTime() - time, right.getExpectedDepartureTime() - time);
        if (c != 0)
            return c;
        c = Double.compare(right.getRemainingEnergyRequiredKWh(), left.getRemainingEnergyRequiredKWh());
        return c != 0 ? c : Integer.compare(left.getId(), right.getId());
    }
}