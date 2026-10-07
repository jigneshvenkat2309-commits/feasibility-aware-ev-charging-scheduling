package model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Infrastructure state and assignment operations for one station. */
public class Station {
    private final String stationId;
    private final List<EV> allEVs = new ArrayList<>();
    private final List<EV> waitingEVs = new ArrayList<>();
    private final List<Charger> chargers;

    public Station(String stationId, List<Charger> chargers) {
        if (stationId == null || chargers == null)
            throw new IllegalArgumentException("Station data cannot be null");
        this.stationId = stationId;
        this.chargers = new ArrayList<>(chargers);
        this.chargers.sort(Comparator.comparingInt(Charger::getId));
    }

    public void addEV(EV ev) {
        if (ev == null || allEVs.contains(ev))
            throw new IllegalArgumentException("Duplicate or null EV");
        allEVs.add(ev);
        if (ev.isWaiting())
            waitingEVs.add(ev);
    }

    /**
     * Remove an EV from the station.
     * 
     * Invariant: If an EV is removed, no charger may continue referencing it.
     * 
     * Policy:
     * - Removing a WAITING EV is always safe (not attached to charger).
     * - Removing a CHARGING EV is forbidden: the EV must be released from its
     * charger first via Simulation.depart() or Charger failure handling.
     * - This prevents leaving chargers in an inconsistent state.
     * 
     * @throws IllegalStateException if the EV is currently CHARGING on a charger
     */
    public void removeEV(EV ev) {
        if (ev != null && ev.isActive()) {
            // EV is CHARGING - check if it's on a charger
            for (Charger charger : chargers) {
                if (charger.getCurrentEV() == ev) {
                    throw new IllegalStateException(
                            "Cannot remove a CHARGING EV that is still attached to charger " + charger.getId());
                }
            }
        }
        allEVs.remove(ev);
        waitingEVs.remove(ev);
    }

    public List<EV> getAllEVs() {
        return List.copyOf(allEVs);
    }

    public List<EV> getWaitingEVs() {
        return List.copyOf(waitingEVs);
    }

    public List<Charger> getChargers() {
        return List.copyOf(chargers);
    }

    public List<Charger> getAvailableChargers() {
        return chargers.stream().filter(Charger::isAvailable).toList();
    }

    public String getStationId() {
        return stationId;
    }

    public void assignEVToCharger(EV ev, Charger charger) {
        if (!chargers.contains(charger) || !waitingEVs.contains(ev) || !charger.isAvailable())
            throw new IllegalStateException("Invalid assignment");
        if (chargers.stream().anyMatch(c -> c.getCurrentEV() == ev))
            throw new IllegalStateException("EV already assigned");
        charger.assignEV(ev);
        waitingEVs.remove(ev);
    }

    public void returnEVToWaiting(EV ev, Charger charger) {
        if (charger.getCurrentEV() != ev)
            throw new IllegalStateException("EV is not on charger");
        charger.releaseEV();
        if (ev.isWaiting() && !waitingEVs.contains(ev))
            waitingEVs.add(ev);
    }

    public void releaseEVFromCharger(EV ev, Charger charger) {
        if (charger.getCurrentEV() == ev)
            charger.releaseEV();
    }

    public void refreshWaitingQueue() {
        waitingEVs.removeIf(ev -> !ev.isWaiting());
        for (EV ev : allEVs)
            if (ev.isWaiting() && !waitingEVs.contains(ev))
                waitingEVs.add(ev);
    }

    public double getCurrentLoadKW() {
        return chargers.stream().filter(c -> c.getCurrentEV() != null)
                .mapToDouble(c -> Math.min(c.getMaxPowerKW(), c.getCurrentEV().getMaxChargingPowerKW())).sum();
    }
}