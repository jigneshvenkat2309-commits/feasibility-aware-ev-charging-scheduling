package scheduler;

import model.Charger;
import model.EV;
import model.Station;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** First-come, first-served scheduler: O(N log N + C). */
public class FCFS implements Scheduler {
    public List<SchedulingDecision> schedule(int currentTime, Station station) {
        List<EV> evs = new ArrayList<>(station.getWaitingEVs());
        evs.sort(Comparator.comparingInt(EV::getArrivalTime).thenComparingInt(EV::getId));
        return pair(evs, station.getAvailableChargers());
    }

    static List<SchedulingDecision> pair(List<EV> evs, List<Charger> chargers) {
        List<SchedulingDecision> decisions = new ArrayList<>();
        int count = Math.min(evs.size(), chargers.size());
        for (int i = 0; i < count; i++) {
            EV ev = evs.get(i);
            Charger charger = chargers.get(i);
            decisions.add(
                    new SchedulingDecision(ev, charger, Math.min(ev.getMaxChargingPowerKW(), charger.getMaxPowerKW())));
        }
        return decisions;
    }
}