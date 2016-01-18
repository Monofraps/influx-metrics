package net.monofraps.influxmetrics.jvm;

import com.google.common.collect.ImmutableList;
import net.monofraps.influxmetrics.InfluxSeriesRegistry;
import net.monofraps.influxmetrics.MetricTag;
import net.monofraps.influxmetrics.fields.Gauge;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Creates a measurement series for each available garbage collector.
 * <p>
 * Measurement: `measurementName` (defaults to 'jvm_gc')
 * Tags:
 * collectorName - the garbage collector's name
 * Fields:
 * count - number of collections occurred
 * time - total time spent for garbage collections
 */
public class GarbageCollectorMetrics {
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");

    private final String measurementName;

    /**
     * Creates new measurement series for each garbage collector in ManagementFactory.getGarbageCollectorMXBeans using the measurement name 'jvm_gc'.
     */
    public GarbageCollectorMetrics() {
        this("jvm_gc");
    }

    /**
     * Creates new measurement series for each garbage collector in ManagementFactory.getGarbageCollectorMXBeans using the given measurement name.
     *
     * @param measurementName The measurement name to use.
     */
    public GarbageCollectorMetrics(String measurementName) {
        this.measurementName = measurementName;
    }


    public void registerSeries(InfluxSeriesRegistry registry) {
        final List<GarbageCollectorMXBean> garbageCollectors = ManagementFactory.getGarbageCollectorMXBeans();

        for (final GarbageCollectorMXBean gc : garbageCollectors) {
            final String name = WHITESPACE.matcher(gc.getName()).replaceAll("-");
            registry.timeSeries(measurementName,
                    ImmutableList.of(new MetricTag("collectorName", name)),
                    ImmutableList.of(new Gauge<>("count", gc::getCollectionCount), new Gauge<>("time", gc::getCollectionTime)));
        }
    }
}
