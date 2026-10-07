package scheduler;

import model.EV;
import model.Station;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Earliest-deadline-first scheduler: O(N log N + C). */
public class EDF implements Scheduler {
    public List<SchedulingDecision> schedule(int currentTime, Station station) {
        List<EV> evs = new ArrayList<>(station.getWaitingEVs());
        evs.sort(Comparator.comparingInt(EV::getExpectedDepartureTime).thenComparingInt(EV::getArrivalTime)
                .thenComparingInt(EV::getId));
        return FCFS.pair(evs, station.getAvailableChargers());
    }
}