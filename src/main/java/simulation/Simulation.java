package simulation;

import metrics.Metrics;
import model.Charger;
import model.EV;
import model.Event;
import model.Station;
import scheduler.Scheduler;
import scheduler.SchedulingDecision;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Owns discrete time, event processing, charging, and metric updates.
 *
 * SIMULATION MINUTE-BOUNDARY SEMANTICS:
 *
 * At each simulation time t ∈ [0, simulationDuration):
 *
 * 1. PROCESS EVENTS scheduled at time t, in order:
 * - EV_DEPARTURE events (Event.EventType.ordinal() = 0)
 * - CHARGER_FAILURE events (Event.EventType.ordinal() = 1)
 * - CHARGER_REPAIR events (Event.EventType.ordinal() = 2)
 * - EV_ARRIVAL events (Event.EventType.ordinal() = 3)
 * This ordering is enforced by Event.compareTo() which sorts by time, then
 * EventType.ordinal().
 *
 * 2. SCHEDULE: Run scheduler.schedule(t, station) to generate decisions.
 *
 * 3. APPLY: Assign waiting EVs to available chargers, marking them CHARGING at
 * time t.
 *
 * 4. CHARGE: For each EV actively charging at t, deliver power during interval
 * [t, t+1).
 * - Completion boundary is set to t+1, so EVs complete exactly at minute
 * boundaries.
 * - If EV reaches targetSOC during [t, t+1), it transitions to COMPLETED at
 * t+1.
 * - Release completed EVs from chargers.
 *
 * 5. OBSERVE: Record queue length, load, and energy metrics at time t.
 *
 * 6. ADVANCE: currentTime++ (now currentTime = t+1 for next iteration).
 *
 * SIMULATION HORIZON:
 *
 * The simulation runs while currentTime < simulationDuration.
 * When currentTime reaches simulationDuration, the main loop exits.
 * Any EV that has not reached COMPLETED or DEPARTED_NOT_ACHIEVED is marked
 * HORIZON_ABANDONED, indicating it was still waiting or charging when the
 * simulation ended. No artificial departure time is created for such EVs.
 *
 * EV STATE MODEL:
 * - WAITING: in queue, not yet assigned to charger
 * - CHARGING: actively receiving power from charger
 * - COMPLETED: reached targetSOC before departure event
 * - DEPARTED_NOT_ACHIEVED: departure event fired (actualDepartureTime set) but
 * not COMPLETED
 * - HORIZON_ABANDONED: simulation ended while still WAITING or CHARGING
 */
public class Simulation {
    private final Scenario scenario;
    private final Station station;
    private final Scheduler scheduler;
    private final Metrics metrics;
    private final PriorityQueue<Event> events = new PriorityQueue<>();
    private int currentTime;
    private final int simulationDuration;
    private boolean initialized;

    public Simulation(Scenario scenario, Scheduler scheduler) {
        this(scenario, scheduler, new EventGenerator(scenario).generateEVs());
    }

    public Simulation(Scenario scenario, Scheduler scheduler, List<EV> evs) {
        this.scenario = scenario;
        require(scheduler);
        this.scheduler = scheduler;
        this.station = new Station("station", new EventGenerator(scenario).generateChargers());
        this.metrics = new Metrics();
        this.simulationDuration = scenario.getSimulationDurationMinutes();
        this.metrics.getEnergyMetrics().configure(scenario.getNumberOfChargers(), simulationDuration);
        for (EV ev : evs) {
            events.add(new Event(ev.getArrivalTime(), Event.EventType.EV_ARRIVAL, ev, null));
            events.add(new Event(ev.getExpectedDepartureTime(), Event.EventType.EV_DEPARTURE, ev, null));
        }
        if (scenario.isChargerFailureEnabled() && !station.getChargers().isEmpty()) {
            int numToFail = Math.max(1, station.getChargers().size() / 4);
            int failure = 180;
            int repair = 300;
            for (int i = 0; i < numToFail; i++) {
                Charger charger = station.getChargers().get(i);
                events.add(new Event(failure, Event.EventType.CHARGER_FAILURE, null, charger));
                events.add(new Event(repair, Event.EventType.CHARGER_REPAIR, null, charger));
            }
        }
    }

    public Simulation(int duration, Station station, Scheduler scheduler, Metrics metrics) {
        this.scenario = null;
        this.station = station;
        require(scheduler);
        this.scheduler = scheduler;
        this.metrics = metrics == null ? new Metrics() : metrics;
        this.simulationDuration = duration;
        this.metrics.getEnergyMetrics().configure(station.getChargers().size(), duration);
    }

    private void require(Scheduler value) {
        if (value == null)
            throw new IllegalArgumentException("Scheduler cannot be null");
    }

    public void run() {
        if (initialized)
            return;
        initialized = true;
        while (currentTime < simulationDuration) {
            processEvents();
            station.refreshWaitingQueue();
            long start = System.nanoTime();
            List<SchedulingDecision> decisions = scheduler.schedule(currentTime, station);
            metrics.getComputationalMetrics().addSchedulerRuntime(System.nanoTime() - start);
            apply(decisions);
            charge();
            currentTime++;
        }
        // Mark any EVs still waiting or charging at horizon as abandoned
        // (they did not complete before the simulation ended)
        for (EV ev : station.getAllEVs()) {
            ev.abandonAtHorizon();
        }
        metrics.getServiceMetrics().record(station.getAllEVs());
    }

    private void processEvents() {
        while (!events.isEmpty() && events.peek().getTime() == currentTime) {
            Event event = events.poll();
            switch (event.getType()) {
                case EV_ARRIVAL -> station.addEV(event.getEv());
                case EV_DEPARTURE -> depart(event.getEv());
                case CHARGER_FAILURE -> fail(event.getCharger());
                case CHARGER_REPAIR -> event.getCharger().repair();
            }
        }
    }

    private void depart(EV ev) {
        if (ev == null || !station.getAllEVs().contains(ev))
            return;
        for (Charger charger : station.getChargers())
            if (charger.getCurrentEV() == ev) {
                charger.releaseEV();
                break;
            }
        ev.depart(currentTime);
    }

    private void fail(Charger charger) {
        if (charger.getCurrentEV() != null) {
            EV ev = charger.getCurrentEV();
            charger.releaseEV();
            ev.returnToWaiting();
        }
        charger.fail();
    }

    private void apply(List<SchedulingDecision> decisions) {
        for (SchedulingDecision decision : decisions) {
            EV ev = decision.ev();
            Charger charger = decision.charger();
            if (ev.isWaiting() && charger.isAvailable() && station.getWaitingEVs().contains(ev)) {
                station.assignEVToCharger(ev, charger);
                ev.beginCharging(currentTime);
            }
        }
    }

    private void charge() {
        double load = 0, energy = 0;
        int busy = 0;
        for (Charger charger : station.getChargers()) {
            EV ev = charger.getCurrentEV();
            if (ev != null && ev.isActive()) {
                double power = Math.min(ev.getMaxChargingPowerKW(), charger.getMaxPowerKW());
                // Charge for 1 minute during interval [currentTime, currentTime+1).
                // completionBoundaryTime is currentTime+1, so if EV completes this interval,
                // its completionTime is recorded as currentTime+1 (the next minute boundary).
                energy += ev.charge(power, 1, currentTime + 1);
                load += power;
                busy++;
                charger.recordBusyTime(1);
            }
        }
        // Add 1 minute of waiting time to all EVs currently in the waiting queue
        for (EV ev : station.getWaitingEVs()) {
            ev.updateWaitingTime(1);
        }
        
        metrics.getServiceMetrics().observeQueue(station.getWaitingEVs().size());
        metrics.getEnergyMetrics().observe(load, energy, busy);
        for (Charger charger : station.getChargers())
            if (charger.getCurrentEV() != null && charger.getCurrentEV().isCompleted())
                charger.releaseEV();
    }

    public int getCurrentTime() {
        return currentTime;
    }

    public Station getStation() {
        return station;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public metrics.ServiceMetrics getServiceMetrics() {
        return metrics.getServiceMetrics();
    }

    public metrics.EnergyMetrics getEnergyMetrics() {
        return metrics.getEnergyMetrics();
    }

    public metrics.ComputationalMetrics getComputationalMetrics() {
        return metrics.getComputationalMetrics();
    }

    public String getSchedulerName() {
        return scheduler.getName();
    }

    public List<EV> getEVs() {
        return new ArrayList<>(station.getAllEVs());
    }
}