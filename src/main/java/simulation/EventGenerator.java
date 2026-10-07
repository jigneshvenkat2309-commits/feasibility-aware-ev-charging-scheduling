package simulation;

import model.Charger;
import model.EV;
import model.Event;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Creates a deterministic base dataset from a scenario seed. */
public class EventGenerator {
    private final Scenario scenario;

    public EventGenerator(Scenario scenario) {
        this.scenario = scenario;
    }

    public List<EV> generateEVs() {
        Random random = new Random(scenario.getRandomSeed());
        List<EV> result = new ArrayList<>();
        int arrivalWindow = 540; // NORMAL
        if ("HIGH".equalsIgnoreCase(scenario.getDemandLevel())) {
            arrivalWindow = 360;
        } else if ("LOW".equalsIgnoreCase(scenario.getDemandLevel())) {
            arrivalWindow = 720;
        }
        for (int id = 0; id < scenario.getNumberOfEVs(); id++) {
            int arrival;
            if (scenario.isBurstArrivalEnabled() && id < scenario.getNumberOfEVs() / 4) {
                arrival = random.nextInt(Math.max(1, arrivalWindow / 4)); // first 25% of EVs in first 25% of window
            } else {
                arrival = random.nextInt(Math.max(1, arrivalWindow));
            }
            double battery = between(random, scenario.getMinBatteryCapacityKWh(), scenario.getMaxBatteryCapacityKWh());
            double power = between(random, scenario.getMinChargingPowerKW(), scenario.getMaxChargingPowerKW());
            double initial = 0.20 + random.nextDouble() * 0.35;
            double target;
            if ("HIGH".equalsIgnoreCase(scenario.getUncertaintyLevel())) {
                target = initial + 0.10 + random.nextDouble() * 0.50; // delta 0.10 to 0.60
            } else {
                target = initial + 0.20 + random.nextDouble() * 0.10; // delta 0.20 to 0.30
            }
            target = Math.min(0.90, target);
            int serviceMinutes = (int) Math
                    .ceil(battery * (target - initial) / Math.min(power, scenario.getChargerPowerKW()) * 60);
            int slack = 60;
            if ("HIGH".equalsIgnoreCase(scenario.getUncertaintyLevel())) {
                slack = 15 + random.nextInt(76); // 15 to 90
            }
            int departure = Math.max(arrival + 1,
                    Math.min(scenario.getSimulationDurationMinutes() + 120, arrival + serviceMinutes + slack));
            result.add(new EV(id, arrival, departure, battery, initial, target, power));
        }
        result.sort(Comparator.comparingInt(EV::getArrivalTime).thenComparingInt(EV::getId));
        return result;
    }

    public List<Event> generateEvents() {
        List<Event> events = new ArrayList<>();
        for (EV ev : generateEVs())
            events.add(new Event(ev.getArrivalTime(), Event.EventType.EV_ARRIVAL, ev, null));
        return events;
    }

    public List<Charger> generateChargers() {
        List<Charger> chargers = new ArrayList<>();
        for (int i = 0; i < scenario.getNumberOfChargers(); i++)
            chargers.add(new Charger(i, scenario.getChargerPowerKW()));
        return chargers;
    }

    private double between(Random random, double min, double max) {
        return min + random.nextDouble() * (max - min);
    }
}