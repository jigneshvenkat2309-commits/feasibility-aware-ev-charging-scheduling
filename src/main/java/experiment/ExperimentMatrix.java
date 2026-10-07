package experiment;

import java.util.ArrayList;
import java.util.List;
import simulation.Scenario;

/** Research matrix definition and scenario generator. */
public class ExperimentMatrix {
    private final List<Integer> evCounts;
    private final List<String> demandLevels;
    private final List<String> uncertaintyLevels;
    private final List<Boolean> failureConfigs;
    private final int chargers;
    private final int duration;
    private final long baseSeed;
    private final int trials;

    public ExperimentMatrix(List<Integer> evCounts, List<String> demandLevels, List<String> uncertaintyLevels,
            List<Boolean> failureConfigs, int chargers, int duration, long baseSeed, int trials) {
        this.evCounts = List.copyOf(evCounts);
        this.demandLevels = List.copyOf(demandLevels);
        this.uncertaintyLevels = List.copyOf(uncertaintyLevels);
        this.failureConfigs = List.copyOf(failureConfigs);
        this.chargers = chargers;
        this.duration = duration;
        this.baseSeed = baseSeed;
        this.trials = trials;
    }

    public record MatrixCell(int cellIndex, int evCount, String demand, String uncertainty, boolean failure) {}

    public List<MatrixCell> getCells() {
        List<MatrixCell> cells = new ArrayList<>();
        int index = 0;
        for (int ev : evCounts) {
            for (String demand : demandLevels) {
                for (String uncertainty : uncertaintyLevels) {
                    for (boolean failure : failureConfigs) {
                        cells.add(new MatrixCell(index++, ev, demand, uncertainty, failure));
                    }
                }
            }
        }
        return cells;
    }

    public List<ExperimentTask> generateTasks() {
        List<ExperimentTask> tasks = new ArrayList<>();
        for (MatrixCell cell : getCells()) {
            for (int t = 0; t < trials; t++) {
                long derivedSeed = baseSeed + ((long) cell.cellIndex() * trials) + t;
                String name = String.format("%dev-%s-%s-%s", cell.evCount(), cell.demand(), cell.uncertainty(),
                        cell.failure() ? "fail" : "nofail");
                int scaledChargers = cell.evCount() / 5;
                boolean burst = "HIGH".equalsIgnoreCase(cell.demand());
                Scenario scenario = new Scenario(name, cell.evCount(), scaledChargers, duration, cell.demand(),
                        cell.uncertainty(), burst, cell.failure(), derivedSeed);
                tasks.add(new ExperimentTask(scenario, baseSeed, derivedSeed, t, cell.cellIndex()));
            }
        }
        return tasks;
    }
}
