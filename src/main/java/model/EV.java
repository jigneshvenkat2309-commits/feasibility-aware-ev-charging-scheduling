package model;

/** A single electric vehicle and its charging state. */
public class EV {
    private static final double EPSILON = 1e-9;

    public enum EVState {
        WAITING, CHARGING, COMPLETED, DEPARTED_NOT_ACHIEVED, HORIZON_ABANDONED
    }

    private final int id;
    private final int arrivalTime;
    private final int expectedDepartureTime;
    private final double batteryCapacityKWh;
    private final double initialSOC;
    private double currentSOC;
    private final double targetSOC;
    private final double maxChargingPowerKW;
    private final double originalEnergyRequirementKWh;
    private double remainingEnergyRequiredKWh;
    private int chargingStartTime = -1;
    private int actualDepartureTime = -1;
    private int waitingTimeMinutes;
    private int chargingTimeMinutes;
    private int completionTime = -1;
    private boolean deadlineViolated;
    private EVState state = EVState.WAITING;

    public EV(int id, int arrivalTime, int expectedDepartureTime, double batteryCapacityKWh,
            double initialSOC, double targetSOC, double maxChargingPowerKW) {
        if (id < 0 || arrivalTime < 0 || expectedDepartureTime <= arrivalTime || !Double.isFinite(batteryCapacityKWh)
                || batteryCapacityKWh <= 0
                || !Double.isFinite(initialSOC) || initialSOC < 0 || initialSOC > 1 || !Double.isFinite(targetSOC)
                || targetSOC < initialSOC || targetSOC > 1
                || !Double.isFinite(maxChargingPowerKW) || maxChargingPowerKW <= 0)
            throw new IllegalArgumentException("Invalid EV parameters");
        this.id = id;
        this.arrivalTime = arrivalTime;
        this.expectedDepartureTime = expectedDepartureTime;
        this.batteryCapacityKWh = batteryCapacityKWh;
        this.initialSOC = initialSOC;
        this.currentSOC = initialSOC;
        this.targetSOC = targetSOC;
        this.maxChargingPowerKW = maxChargingPowerKW;
        this.originalEnergyRequirementKWh = batteryCapacityKWh * (targetSOC - initialSOC);
        this.remainingEnergyRequiredKWh = originalEnergyRequirementKWh;
        if (originalEnergyRequirementKWh <= EPSILON) {
            remainingEnergyRequiredKWh = 0;
            currentSOC = targetSOC;
            state = EVState.COMPLETED;
            completionTime = arrivalTime;
        }
    }

    public void beginCharging(int time) {
        if (state != EVState.WAITING)
            throw new IllegalStateException("EV is not waiting");
        if (chargingStartTime < 0)
            chargingStartTime = time;
        state = EVState.CHARGING;
    }

    public double charge(double actualPowerKW, int intervalMinutes, int completionBoundaryTime) {
        if (actualPowerKW <= 0 || intervalMinutes <= 0)
            throw new IllegalArgumentException("Power and interval must be positive");
        if (state != EVState.CHARGING)
            return 0;
        double delivered = Math.min(actualPowerKW * intervalMinutes / 60.0, remainingEnergyRequiredKWh);
        remainingEnergyRequiredKWh = Math.max(0, remainingEnergyRequiredKWh - delivered);
        currentSOC = Math.min(targetSOC,
                Math.max(initialSOC,
                        (batteryCapacityKWh * initialSOC + originalEnergyRequirementKWh - remainingEnergyRequiredKWh)
                                / batteryCapacityKWh));
        chargingTimeMinutes += intervalMinutes;
        if (remainingEnergyRequiredKWh <= EPSILON) {
            remainingEnergyRequiredKWh = 0;
            currentSOC = targetSOC;
            state = EVState.COMPLETED;
            completionTime = completionBoundaryTime;
        }
        return delivered;
    }

    public double charge(double actualPowerKW, int intervalMinutes) {
        return charge(actualPowerKW, intervalMinutes,
                chargingStartTime < 0 ? -1 : chargingStartTime + chargingTimeMinutes + intervalMinutes);
    }

    public double charge(int intervalMinutes) {
        return charge(maxChargingPowerKW, intervalMinutes);
    }

    public void depart(int time) {
        actualDepartureTime = time;
        if (!isCompleted()) {
            state = EVState.DEPARTED_NOT_ACHIEVED;
            deadlineViolated = true;
        }
    }

    public void returnToWaiting() {
        if (state == EVState.CHARGING)
            state = EVState.WAITING;
    }

    /**
     * Mark this EV as abandoned at the simulation horizon.
     * Called when the simulation ends before this EV completes charging.
     * No actual departure time is recorded (actualDepartureTime remains -1).
     * This is distinct from DEPARTED_NOT_ACHIEVED, which requires an actual
     * EV_DEPARTURE event to have occurred at expectedDepartureTime.
     */
    public void abandonAtHorizon() {
        if (state != EVState.COMPLETED && actualDepartureTime < 0) {
            state = EVState.HORIZON_ABANDONED;
        }
    }

    public boolean isCompleted() {
        return state == EVState.COMPLETED;
    }

    public boolean isWaiting() {
        return state == EVState.WAITING;
    }

    public boolean isActive() {
        return state == EVState.CHARGING;
    }

    public int getRemainingTimeUntilDeparture(int currentTime) {
        return expectedDepartureTime - currentTime;
    }

    public int getRemainingTimeUntilDeparture() {
        return expectedDepartureTime - arrivalTime - waitingTimeMinutes - chargingTimeMinutes;
    }

    public void updateWaitingTime(int minutes) {
        waitingTimeMinutes += Math.max(0, minutes);
    }

    public int getId() {
        return id;
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public int getExpectedDepartureTime() {
        return expectedDepartureTime;
    }

    public double getBatteryCapacityKWh() {
        return batteryCapacityKWh;
    }

    public double getInitialSOC() {
        return initialSOC;
    }

    public double getCurrentSOC() {
        return currentSOC;
    }

    public double getTargetSOC() {
        return targetSOC;
    }

    public double getMaxChargingPowerKW() {
        return maxChargingPowerKW;
    }

    public double getOriginalEnergyRequirementKWh() {
        return originalEnergyRequirementKWh;
    }

    public double getOriginalEnergyRequirement() {
        return originalEnergyRequirementKWh;
    }

    public double getRemainingEnergyRequiredKWh() {
        return remainingEnergyRequiredKWh;
    }

    public double getRemainingEnergyRequired() {
        return remainingEnergyRequiredKWh;
    }

    public int getChargingStartTime() {
        return chargingStartTime;
    }

    public int getActualDepartureTime() {
        return actualDepartureTime;
    }

    public int getWaitingTimeMinutes() {
        return waitingTimeMinutes;
    }

    public int getWaitingTime() {
        return waitingTimeMinutes;
    }

    public int getChargingTimeMinutes() {
        return chargingTimeMinutes;
    }

    public int getChargingTime() {
        return chargingTimeMinutes;
    }

    public int getCompletionTime() {
        return completionTime;
    }

    public boolean isDeadlineViolated() {
        return deadlineViolated;
    }

    public EVState getState() {
        return state;
    }

    public void setState(EVState state) {
        this.state = state;
    }
}