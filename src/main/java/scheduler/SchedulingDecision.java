package scheduler;

import model.Charger;
import model.EV;
public record SchedulingDecision(EV ev, Charger charger, double allocatedPowerKW) {
    public SchedulingDecision { if (ev == null || charger == null || !Double.isFinite(allocatedPowerKW) || allocatedPowerKW <= 0 || allocatedPowerKW > ev.getMaxChargingPowerKW() + 1e-9 || allocatedPowerKW > charger.getMaxPowerKW() + 1e-9) throw new IllegalArgumentException("Invalid scheduling decision"); }
    public EV getEV() { return ev; } public Charger getCharger() { return charger; } public double getAllocatedPowerKW() { return allocatedPowerKW; }
}