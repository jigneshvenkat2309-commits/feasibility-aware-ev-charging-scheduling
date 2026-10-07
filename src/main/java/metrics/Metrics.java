package metrics;

public class Metrics {
    private final ServiceMetrics serviceMetrics = new ServiceMetrics();
    private final EnergyMetrics energyMetrics = new EnergyMetrics();
    private final ComputationalMetrics computationalMetrics = new ComputationalMetrics();

    public ServiceMetrics getServiceMetrics() {
        return serviceMetrics;
    }

    public EnergyMetrics getEnergyMetrics() {
        return energyMetrics;
    }

    public ComputationalMetrics getComputationalMetrics() {
        return computationalMetrics;
    }
}