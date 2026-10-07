package simulation;

import model.EV;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class EventGeneratorTest {

    @Test
    public void testReproducibilityWithSameSeed() {
        Scenario s1 = new Scenario("s1", 100, 20, 720, "NORMAL", "LOW", false, false, 42L);
        Scenario s2 = new Scenario("s2", 100, 20, 720, "NORMAL", "LOW", false, false, 42L);

        List<EV> evs1 = new EventGenerator(s1).generateEVs();
        List<EV> evs2 = new EventGenerator(s2).generateEVs();

        assertEquals(evs1.size(), evs2.size());
        for (int i = 0; i < evs1.size(); i++) {
            assertEquals(evs1.get(i).getArrivalTime(), evs2.get(i).getArrivalTime());
            assertEquals(evs1.get(i).getExpectedDepartureTime(), evs2.get(i).getExpectedDepartureTime());
            assertEquals(evs1.get(i).getBatteryCapacityKWh(), evs2.get(i).getBatteryCapacityKWh());
            assertEquals(evs1.get(i).getInitialSOC(), evs2.get(i).getInitialSOC());
        }
    }
}
