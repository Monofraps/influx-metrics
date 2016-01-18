package net.monofraps.influxmetrics.jvm;

import com.google.common.collect.Lists;
import net.monofraps.influxmetrics.InfluxSeriesRegistry;
import net.monofraps.influxmetrics.fields.Gauge;
import net.monofraps.influxmetrics.fields.IMeasurementField;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Creates a measurement series for JVM thread states.
 * <p>
 * Measurement: `measurementName` (defaults to jvm_threads)
 * Tags: None
 * Fields:
 * new, runnable, blocked, waiting, timed_waiting, terminated, daemon, deadlock
 *
 * @author monofraps
 */
public class ThreadStateMetrics {
    private final ThreadMXBean threads;
    private final String measurementName;

    /**
     * Creates a new measurement series for jvm thread states using the default MX bean and measurement name 'jvm_threads'.
     */
    public ThreadStateMetrics() {
        this("jvm_threads");
    }

    /**
     * Creates a new measurement series for jvm thread states using the default thread MX bean and the provided measurement name.
     *
     * @param measurementName The measurement name to use.
     */
    public ThreadStateMetrics(String measurementName) {
        this(ManagementFactory.getThreadMXBean(), measurementName);
    }

    /**
     * Creates a new measurement series for jvm thread states using the provided thread MX bean and measurement name.
     *
     * @param measurementName The measurement name to use.
     * @param threads         The thread MV bean to use.
     */
    public ThreadStateMetrics(ThreadMXBean threads, String measurementName) {
        this.threads = threads;
        this.measurementName = measurementName;
    }

    public void registerSeries(InfluxSeriesRegistry registry) {

        final List<IMeasurementField> fields = new ArrayList<>();

        for (final Thread.State state : Thread.State.values()) {
            fields.add(new Gauge<>(state.toString().toLowerCase(), () -> getThreadCount(state)));
        }

        fields.add(new Gauge<>("daemon", threads::getDaemonThreadCount));
        fields.add(new Gauge<>("deadlock", () -> {
            final long[] deadlockedThreads = threads.findDeadlockedThreads();
            if (deadlockedThreads != null) {
                return deadlockedThreads.length;
            }

            return 0;
        }));

        registry.timeSeries(measurementName, Lists.newArrayList(), fields);
    }

    private long getThreadCount(Thread.State state) {
        final ThreadInfo[] allThreads = getThreadInfo();
        return Arrays.stream(allThreads).filter(t -> t != null && t.getThreadState() == state).count();
    }

    ThreadInfo[] getThreadInfo() {
        return threads.getThreadInfo(threads.getAllThreadIds(), 0);
    }
}
