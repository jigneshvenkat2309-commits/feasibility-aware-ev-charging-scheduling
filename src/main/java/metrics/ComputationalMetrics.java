package metrics;

public class ComputationalMetrics {
    private long totalSchedulerRuntimeNanos;
    private int numberOfSchedulingCalls;

    public void addSchedulerRuntime(long nanos) {
        totalSchedulerRuntimeNanos += nanos;
        numberOfSchedulingCalls++;
    }

    public void addDecisionRuntime(long nanos) {
        addSchedulerRuntime(nanos);
    }

    public long getTotalSchedulerRuntimeNanos() {
        return totalSchedulerRuntimeNanos;
    }

    public long getTotalSchedulerRuntime() {
        return totalSchedulerRuntimeNanos;
    }

    public long getAverageSchedulerRuntimeNanos() {
        return numberOfSchedulingCalls == 0 ? 0 : totalSchedulerRuntimeNanos / numberOfSchedulingCalls;
    }

    public long getAverageDecisionRuntime() {
        return getAverageSchedulerRuntimeNanos();
    }

    public int getNumberOfSchedulingCalls() {
        return numberOfSchedulingCalls;
    }

    public double getTotalSchedulerRuntimeMillis() {
        return totalSchedulerRuntimeNanos / 1_000_000.0;
    }

    public void reset() {
        totalSchedulerRuntimeNanos = 0;
        numberOfSchedulingCalls = 0;
    }
}