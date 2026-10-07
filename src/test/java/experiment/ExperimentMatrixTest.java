package experiment;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

public class ExperimentMatrixTest {

    @Test
    public void testMatrixSizeAndDeterministicOrdering() {
        ExperimentMatrix matrix = new ExperimentMatrix(
                List.of(50, 100, 250, 500),
                List.of("LOW", "NORMAL", "HIGH"),
                List.of("LOW", "HIGH"),
                List.of(false, true),
                20, 720, 42L, 5);

        List<ExperimentMatrix.MatrixCell> cells = matrix.getCells();
        assertEquals(48, cells.size());
        
        // Deterministic ordering: the first cell should be 50, LOW, LOW, false
        ExperimentMatrix.MatrixCell first = cells.get(0);
        assertEquals(0, first.cellIndex());
        assertEquals(50, first.evCount());
        assertEquals("LOW", first.demand());
        assertEquals("LOW", first.uncertainty());
        assertFalse(first.failure());
    }

    @Test
    public void testSeedDerivation() {
        ExperimentMatrix matrix = new ExperimentMatrix(
                List.of(50, 100),
                List.of("NORMAL"),
                List.of("LOW"),
                List.of(false),
                20, 720, 100L, 3);
        
        List<ExperimentTask> tasks = matrix.generateTasks();
        assertEquals(6, tasks.size()); // 2 cells * 3 trials
        
        Set<Long> seeds = new HashSet<>();
        for (ExperimentTask task : tasks) {
            long expectedDerivedSeed = 100L + (task.cellIndex() * 3) + task.trialNumber();
            assertEquals(expectedDerivedSeed, task.derivedSeed());
            assertEquals(expectedDerivedSeed, task.scenario().getRandomSeed());
            seeds.add(task.derivedSeed());
        }
        assertEquals(6, seeds.size(), "Seeds should be unique");
    }
}
