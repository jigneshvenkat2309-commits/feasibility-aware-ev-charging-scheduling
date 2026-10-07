package scheduler;

import model.Charger;
import model.EV;
import model.Station;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Lightweight rule-based adaptive scheduler, approximately O(N*C + N log N).
 */
public class AdaptiveScheduler implements Scheduler {
    public enum DemandLevel {
        LOW, NORMAL, HIGH
    }

    private static final double LOW_RATIO = 0.75;
    private static final double HIGH_RATIO = 2.0;
    private static final double WAITING_REFERENCE_MINUTES = 60.0;
    private static final double[] LOW_WEIGHTS = { 0.45, 0.25, 0.30 };
    private static final double[] NORMAL_WEIGHTS = { 0.55, 0.30, 0.15 };
    private static final double[] HIGH_WEIGHTS = { 0.70, 0.25, 0.05 };

    public List<SchedulingDecision> schedule(int currentTime, Station station) {
        List<EV> remaining = new ArrayList<>(station.getWaitingEVs());
        List<Charger> chargers = new ArrayList<>(station.getAvailableChargers());
        DemandLevel demand = demandLevel(remaining.size(), chargers.size());
        double[] weights = weights(demand);
        List<SchedulingDecision> decisions = new ArrayList<>();
        while (!remaining.isEmpty() && !chargers.isEmpty()) {
            EV bestEV = null;
            Charger bestCharger = null;
            double bestScore = -1;
            for (EV ev : remaining)
                for (Charger charger : chargers) {
                    double score = score(ev, charger, currentTime, weights);
                    if (bestEV == null || score > bestScore + 1e-12
                            || (Math.abs(score - bestScore) <= 1e-12 && tie(ev, bestEV) < 0)) {
                        bestEV = ev;
                        bestCharger = charger;
                        bestScore = score;
                    }
                }
            remaining.remove(bestEV);
            chargers.remove(bestCharger);
            decisions.add(new SchedulingDecision(bestEV, bestCharger,
                    Math.min(bestEV.getMaxChargingPowerKW(), bestCharger.getMaxPowerKW())));
        }
        return decisions;
    }

    public DemandLevel assessDemandLevel(List<EV> waiting, List<Charger> available) {
        return demandLevel(waiting.size(), available.size());
    }

    private DemandLevel demandLevel(int waiting, int available) {
        if (available == 0)
            return waiting == 0 ? DemandLevel.LOW : DemandLevel.HIGH;
        double ratio = (double) waiting / available;
        return ratio < LOW_RATIO ? DemandLevel.LOW : ratio >= HIGH_RATIO ? DemandLevel.HIGH : DemandLevel.NORMAL;
    }

    private double[] weights(DemandLevel level) {
        return level == DemandLevel.LOW ? LOW_WEIGHTS : level == DemandLevel.HIGH ? HIGH_WEIGHTS : NORMAL_WEIGHTS;
    }

    private double score(EV ev, Charger charger, int time, double[] weights) {
        double feasible = Math.min(ev.getMaxChargingPowerKW(), charger.getMaxPowerKW());
        double hours = Math.max(1, ev.getExpectedDepartureTime() - time) / 60.0;
        double deadline = clamp((ev.getRemainingEnergyRequiredKWh() / hours) / feasible);
        double waiting = clamp(Math.max(0, time - ev.getArrivalTime()) / WAITING_REFERENCE_MINUTES);
        double energy = clamp(ev.getRemainingEnergyRequiredKWh() / ev.getBatteryCapacityKWh());
        return weights[0] * deadline + weights[1] * waiting + weights[2] * energy;
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private int tie(EV left, EV right) {
        int c = Integer.compare(left.getExpectedDepartureTime(), right.getExpectedDepartureTime());
        return c != 0 ? c : Integer.compare(left.getId(), right.getId());
    }
}