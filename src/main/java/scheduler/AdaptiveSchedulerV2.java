package scheduler;

import model.Charger;
import model.EV;
import model.Station;
import java.util.ArrayList;
import java.util.List;

/**
 * Feasibility-aware adaptive scheduler (v2).
 * Retains v1's lightweight scoring, but strictly prioritizes feasible EVs.
 * For unfeasible EVs, uses a deterministic fallback minimizing feasibility deficit.
 */
public class AdaptiveSchedulerV2 implements Scheduler {
    public enum DemandLevel {
        LOW, NORMAL, HIGH
    }

    private static final double LOW_RATIO = 0.75;
    private static final double HIGH_RATIO = 2.0;
    private static final double WAITING_REFERENCE_MINUTES = 60.0;
    private static final double[] LOW_WEIGHTS = { 0.45, 0.25, 0.30 };
    private static final double[] NORMAL_WEIGHTS = { 0.55, 0.30, 0.15 };
    private static final double[] HIGH_WEIGHTS = { 0.70, 0.25, 0.05 };

    @Override
    public List<SchedulingDecision> schedule(int currentTime, Station station) {
        List<EV> remaining = new ArrayList<>(station.getWaitingEVs());
        List<Charger> chargers = new ArrayList<>(station.getAvailableChargers());
        DemandLevel demand = demandLevel(remaining.size(), chargers.size());
        double[] weights = weights(demand);
        List<SchedulingDecision> decisions = new ArrayList<>();

        while (!remaining.isEmpty() && !chargers.isEmpty()) {
            EV bestEV = null;
            Charger bestCharger = null;
            double bestScore = -Double.MAX_VALUE;
            boolean bestIsFeasible = false;

            for (EV ev : remaining) {
                for (Charger charger : chargers) {
                    double effectivePower = Math.min(ev.getMaxChargingPowerKW(), charger.getMaxPowerKW());
                    double remainingEnergy = ev.getRemainingEnergyRequiredKWh();
                    
                    // Time required in hours and minutes
                    double requiredHours = remainingEnergy / effectivePower;
                    double requiredMinutes = requiredHours * 60.0;
                    
                    // Remaining time until departure in minutes
                    double remainingMinutes = ev.getExpectedDepartureTime() - currentTime;
                    
                    // EV is feasible if it can theoretically reach target SOC before departure
                    boolean feasible = (requiredMinutes <= remainingMinutes + 1e-9);

                    double score;
                    if (feasible) {
                        // Adaptive v1 score calculation for feasible EVs
                        double hours = Math.max(1.0, remainingMinutes) / 60.0;
                        double deadline = clamp((remainingEnergy / hours) / effectivePower);
                        double waiting = clamp(Math.max(0, currentTime - ev.getArrivalTime()) / WAITING_REFERENCE_MINUTES);
                        double energy = clamp(remainingEnergy / ev.getBatteryCapacityKWh());
                        score = weights[0] * deadline + weights[1] * waiting + weights[2] * energy;
                    } else {
                        // Strongly deprioritize infeasible EVs relative to feasible EVs
                        // Fallback: candidate with the smallest feasibility deficit / greatest urgency
                        // We set score to negative deficit so smaller deficit = higher score
                        double deficit = requiredMinutes - remainingMinutes;
                        score = -deficit;
                    }

                    boolean better = false;
                    if (bestEV == null) {
                        better = true;
                    } else {
                        if (feasible && !bestIsFeasible) {
                            better = true; // strictly prefer feasible
                        } else if (!feasible && bestIsFeasible) {
                            better = false; // strictly prefer feasible
                        } else {
                            // both feasible or both infeasible
                            if (score > bestScore + 1e-12) {
                                better = true;
                            } else if (Math.abs(score - bestScore) <= 1e-12) {
                                if (tie(ev, bestEV) < 0) {
                                    better = true; // deterministic EDF-style tie-breaking
                                }
                            }
                        }
                    }

                    if (better) {
                        bestEV = ev;
                        bestCharger = charger;
                        bestScore = score;
                        bestIsFeasible = feasible;
                    }
                }
            }
            remaining.remove(bestEV);
            chargers.remove(bestCharger);
            decisions.add(new SchedulingDecision(bestEV, bestCharger,
                    Math.min(bestEV.getMaxChargingPowerKW(), bestCharger.getMaxPowerKW())));
        }
        return decisions;
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

    private double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private int tie(EV left, EV right) {
        int c = Integer.compare(left.getExpectedDepartureTime(), right.getExpectedDepartureTime());
        return c != 0 ? c : Integer.compare(left.getId(), right.getId());
    }
}
