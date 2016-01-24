package net.monofraps.influxmetrics.internal;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import net.monofraps.influxmetrics.*;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.InfluxPoint;
import org.influxdb.dto.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class InfluxdbHttpReporter extends ScheduledReporter {
    private final Map<String, String> tags;
    private final String retentionPolicy;
    private final Logger logger = LoggerFactory.getLogger(InfluxdbHttpReporter.class);
    private final Map<InfluxSeriesRegistry, String> registryToDatabaseMapping;
    private final InfluxSeriesRegistry reporterStatsRegistry;
    private final AtomicInteger currentPointCount = new AtomicInteger(0);
    private final AtomicInteger currentBatchCount = new AtomicInteger(0);
    private InfluxDB influxDB;

    protected InfluxdbHttpReporter(final Map<InfluxSeriesRegistry, String> registries, final String httpConnection, final String username, final String password, final List<MetricTag> tags, String retentionPolicy) {
        this(registries, httpConnection, username, password, tags, retentionPolicy, null);
    }

    protected InfluxdbHttpReporter(final Map<InfluxSeriesRegistry, String> registries, final String httpConnection, final String username, final String password, final List<MetricTag> tags, String retentionPolicy, final InfluxSeriesRegistry reporterStatsRegistry) {
        super(registries.keySet(), "InfluxDbHttpReporter");
        this.registryToDatabaseMapping = registries;
        this.reporterStatsRegistry = reporterStatsRegistry;

        logger.debug("Connecting to InfluxDB at '{}' as user '{}'", httpConnection, username);
        influxDB = InfluxDBFactory.connect(httpConnection, username, password);

        this.tags = tags.stream().collect(Collectors.toMap(MetricTag::getTagName, MetricTag::getTagValue));
        this.retentionPolicy = retentionPolicy;
    }

    public static Builder forRegistry(InfluxSeriesRegistry registry, String database) {
        return new Builder(registry, database);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void start(final long period, final TimeUnit unit) {
        registryToDatabaseMapping.values().stream().filter(database -> !influxDB.describeDatabases().contains(database)).forEach(database -> {
            influxDB.createDatabase(database);
        });

        super.start(period, unit);
    }

    @Override
    protected void report(final InfluxSeriesRegistry registry) {
        Stopwatch timer = Stopwatch.createStarted();

        final String database = registryToDatabaseMapping.get(registry);

        BatchPoints batchPoints = BatchPoints.database(database).retentionPolicy(retentionPolicy).build();

        for (final InfluxSeries series : registry.getSeries()) {
            for (DataPoint dataPoint : series.getValueSets()) {
                final InfluxPoint.InfluxPointBuilder pointBuilder = InfluxPoint.forMeasurement(series.getMeasurementName()).time(dataPoint.getTime(), dataPoint.getPrecision()).tag(series.getTags());

                pointBuilder.fields(dataPoint.getFields());
                pointBuilder.tag(tags);

                final Point point = pointBuilder.build();
                logger.trace("Writing point '{}' into '{}' using retention policy '{}'", point, database, retentionPolicy);
                batchPoints.point(point);
            }
        }

        influxDB.write(batchPoints);
        timer.stop();

        currentBatchCount.incrementAndGet();
        currentPointCount.addAndGet(batchPoints.getPoints().size());

        onRegistryReported(database, timer.elapsed(TimeUnit.MILLISECONDS), batchPoints.getPoints().size());
    }

    private void onRegistryReported(String database, long reportTime, int pointCount) {
        if (reporterStatsRegistry == null) {
            return;
        }

        getRegistryReportedSeries(database).commitEvent(new MetricsRegistryReportedEvent(reportTime, pointCount));
    }

    private EventSeries<MetricsRegistryReportedEvent> getRegistryReportedSeries(final String targetDatabaseName) {
        Preconditions.checkState(reporterStatsRegistry != null);
        return reporterStatsRegistry.eventSeries("reporter_registry_committed", ImmutableList.of(new MetricTag("targetDb", targetDatabaseName)), MetricsRegistryReportedEvent.class);
    }

    @Override
    protected void postReport(long reportTime) {
        if (reporterStatsRegistry != null) {
            getAllRegistriesCommittedEventSeries().commitEvent(new MetricsReportedEvent(currentBatchCount.getAndSet(0), currentPointCount.getAndSet(0), reportTime));
        }
    }

    private EventSeries<MetricsReportedEvent> getAllRegistriesCommittedEventSeries() {
        Preconditions.checkState(reporterStatsRegistry != null);
        return reporterStatsRegistry.eventSeries("reporter_committed", Collections.emptyList(), MetricsReportedEvent.class);
    }

    public static class Builder {
        private final Map<InfluxSeriesRegistry, String> registries = new HashMap<>(5);
        private String httpConnection = "http://localhost:8086";
        private String username = "admin";
        private String password = "admin";
        private List<MetricTag> tags = new ArrayList<>();
        private String retentionPolicy = "default";
        private InfluxSeriesRegistry ownStatsRegistry;

        public Builder(final InfluxSeriesRegistry registry, final String database) {
            withAdditionalRegistry(registry, database);
        }

        public Builder withAdditionalRegistry(final InfluxSeriesRegistry registry, final String database) {
            registries.put(registry, database);
            return this;
        }

        public Builder() {
        }

        public Builder withHttpConnection(String hostname, String port) {
            httpConnection = String.format("http://%s:%s", hostname, port);
            return this;
        }

        public Builder withRetentionPolicy(final String retentionPolicy) {
            this.retentionPolicy = retentionPolicy;
            return this;
        }

        public Builder withUser(final String username, final String password) {
            this.username = username;
            this.password = password;

            return this;
        }

        public Builder tag(final String key, final String value) {
            tags.add(new MetricTag(key, value));
            return this;
        }

        public Builder reportOwnStatistics(final InfluxSeriesRegistry ownStatsRegistry) {
            this.ownStatsRegistry = ownStatsRegistry;
            return this;
        }

        public InfluxdbHttpReporter build() {
            Preconditions.checkState(registries.size() >= 1, "Need to specify at least one registry");
            return new InfluxdbHttpReporter(registries, httpConnection, username, password, tags, retentionPolicy, ownStatsRegistry);
        }
    }
}
