package net.monofraps.influxmetrics;

/**
 * @author monofraps
 */
public interface RegistryEventListener {
    void onSeriesRegistered(final InfluxSeries series);

    void onSeriesRemoved(final InfluxSeries series);
}
