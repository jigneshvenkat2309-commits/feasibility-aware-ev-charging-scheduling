package metrics;

import model.EV;
import java.util.List;

public class ServiceMetrics {
    private double waitingSum;
    private int maxWaiting;
    private int queueSamples;
    private int queueSum;
    private int completed;
    private int violated;
    private int total;

    public void observeQueue(int length) {
        queueSum += length;
        queueSamples++;
    }

    public void record(List<EV> evs) {
        total = evs.size();
        waitingSum = 0;
        maxWaiting = 0;
        completed = 0;
        violated = 0;
        for (EV ev : evs) {
            waitingSum += ev.getWaitingTimeMinutes();
            maxWaiting = Math.max(maxWaiting, ev.getWaitingTimeMinutes());
            if (ev.isCompleted())
                completed++;
            if (ev.isDeadlineViolated() || (!ev.isCompleted() && ev.getActualDepartureTime() >= 0))
                violated++;
        }
    }

    public double getAverageWaitingTime() {
        return total == 0 ? 0 : waitingSum / total;
    }

    public double getMaxWaitingTime() {
        return maxWaiting;
    }

    public double getMaximumWaitingTime() {
        return maxWaiting;
    }

    public double getCompletionRate() {
        return total == 0 ? 0 : (double) completed / total;
    }

    public double getDeadlineViolationRate() {
        return total == 0 ? 0 : (double) violated / total;
    }

    public int getThroughput() {
        return completed;
    }

    public double getAverageQueueLength() {
        return queueSamples == 0 ? 0 : (double) queueSum / queueSamples;
    }

    public void addWaitingTime(double value) {
        waitingSum += value;
    }

    public void incrementTotalEVs() {
        total++;
    }

    public void updateCompletionRate(int value) {
        completed = value;
    }

    public void updateDeadlineViolationRate(int value) {
        violated = value;
    }
}