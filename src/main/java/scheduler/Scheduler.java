package scheduler;

import model.Station;
import java.util.List;
public interface Scheduler { List<SchedulingDecision> schedule(int currentTime, Station station); default String getName() { return getClass().getSimpleName(); } }