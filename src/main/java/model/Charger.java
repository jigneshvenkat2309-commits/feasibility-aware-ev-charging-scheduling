package model;

/** A physical charging point. */
public class Charger {
    private final int id;
    private final double maxPowerKW;
    private boolean failed;
    private EV currentEV;
    private int busyTimeMinutes;

    public Charger(int id, double maxPowerKW) {
        if (id < 0 || !Double.isFinite(maxPowerKW) || maxPowerKW <= 0)
            throw new IllegalArgumentException("Invalid charger parameters");
        this.id = id;
        this.maxPowerKW = maxPowerKW;
    }

    public int getId() {
        return id;
    }

    public double getMaxPowerKW() {
        return maxPowerKW;
    }

    public boolean isAvailable() {
        return !failed && currentEV == null;
    }

    public boolean isFailed() {
        return failed;
    }

    public EV getCurrentEV() {
        return currentEV;
    }

    public int getBusyTimeMinutes() {
        return busyTimeMinutes;
    }

    public int getBusyTime() {
        return busyTimeMinutes;
    }

    public void assignEV(EV ev) {
        if (ev == null || !isAvailable())
            throw new IllegalStateException("Charger is unavailable");
        currentEV = ev;
    }

    public void releaseEV() {
        currentEV = null;
    }

    public void recordBusyTime(int minutes) {
        if (minutes < 0)
            throw new IllegalArgumentException("Minutes must be non-negative");
        busyTimeMinutes += minutes;
    }

    public void fail() {
        failed = true;
        // Note: Any EV currently assigned must be released and requeued by the
        // Simulation/Station before fail() is called. This method does NOT
        // automatically release the EV to prevent violating the separation of concerns.
        // See Simulation.fail(Charger) for the complete charger failure handling.
    }

    public void repair() {
        failed = false;
    }
}