package net.monofraps.influxmetrics.jvm;

import com.google.common.collect.ImmutableList;
import net.monofraps.influxmetrics.InfluxSeriesRegistry;
import net.monofraps.influxmetrics.MetricTag;
import net.monofraps.influxmetrics.fields.IMeasurementField;
import net.monofraps.influxmetrics.fields.JmxAttributeGauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates jvm_buffer measurement series tagged with the buffer pool type with fields Count, MemoryUsed and TotalCapacity.
 *
 * Measurement: `measurementName` (defaults to 'jvm_buffer')
 * Tags:
 *  pool - The buffer pool's name
 * Fields:
 *  count, used, capacity
 */
public class BufferPoolMetrics {
    private static final String[] ATTRIBUTES = {"Count", "MemoryUsed", "TotalCapacity"};
    private static final String[] NAMES = {"count", "used", "capacity"};
    private static final String[] POOLS = {"direct", "mapped"};
    private static final Logger LOGGER = LoggerFactory.getLogger(BufferPoolMetrics.class);

    private final String measurementName;
    private final MBeanServer mBeanServer;

    /**
     * Creates a new buffer pool measurement series using the default platform MBean server and the default measurement name.
     */
    public BufferPoolMetrics() {
        this(ManagementFactory.getPlatformMBeanServer());
    }

    /**
     * Create a new buffer pool measurement series using the provided MBean server and the default measurement name.
     * @param mBeanServer The MBean server to use when querying for buffer pools.
     */
    public BufferPoolMetrics(final MBeanServer mBeanServer) {
        this(mBeanServer, "jvm_buffer");
    }

    /**
     * Create a new buffer pool measurement series using the provided MBean server and the provided measurement name.
     * @param mBeanServer The MBean server to use when querying for buffer pools.
     * @param measurementName The measurement name.
     */
    public BufferPoolMetrics(final MBeanServer mBeanServer, final String measurementName) {
        this.mBeanServer = mBeanServer;
        this.measurementName = measurementName;
    }

    public void registerSeries(final InfluxSeriesRegistry registry) {
        for (final String pool : POOLS) {
            registerSeriesForPool(registry, pool);
        }
    }

    private void registerSeriesForPool(final InfluxSeriesRegistry registry, final String pool) {
        try {
            final ObjectName poolBeanObjectName = new ObjectName("java.nio:type=BufferPool,name=" + pool);
            final List<IMeasurementField> measurementFields = new ArrayList<>();
            for (int i = 0; i < ATTRIBUTES.length; i++) {
                final String attribute = ATTRIBUTES[i];
                final String name = NAMES[i];

                measurementFields.add(new JmxAttributeGauge(name, mBeanServer, poolBeanObjectName, attribute));
            }
            registry.timeSeries(measurementName, ImmutableList.of(new MetricTag("pool", pool)), measurementFields);
        } catch (JMException ignored) {
            LOGGER.debug("Unable to load buffer pool MBeans, possibly running on Java 6");
        }
    }
}
