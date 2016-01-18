package net.monofraps.influxmetrics.internal;

import net.monofraps.influxmetrics.*;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class InfluxdbHttpReporter extends ScheduledReporter {
    private final String database;
    private final Map<String, String> tags;
    private final String retentionPolicy;
    private final Logger logger = LoggerFactory.getLogger(InfluxdbHttpReporter.class);
    private InfluxDB influxDB;

    protected InfluxdbHttpReporter(final InfluxSeriesRegistry registry, final String httpConnection, final String username, final String password, final String database, final List<MetricTag> tags, String retentionPolicy, boolean reportOwnStatistics) {
        super(registry, "InfluxDBReporter", reportOwnStatistics);

        logger.debug("Connecting to InfluxDB at '{}' as user '{}'", httpConnection, username);
        influxDB = InfluxDBFactory.connect(httpConnection, username, password);

        this.database = database;
        this.tags = tags.stream().collect(Collectors.toMap(MetricTag::getTagName, MetricTag::getTagValue));
        this.retentionPolicy = retentionPolicy;
    }

    public static Builder forRegistry(InfluxSeriesRegistry registry) {
        return new Builder(registry);
    }

    @Override
    public void start(final long period, final TimeUnit unit) {
        if (!influxDB.describeDatabases().contains(database)) {
            influxDB.createDatabase(database);
        }

        final int batchFlushPeriod = Math.round(unit.toMillis(period) / 10f);
        logger.debug("Influx batch flush period is {}ms", batchFlushPeriod);
        influxDB.enableBatch(100, batchFlushPeriod, TimeUnit.MILLISECONDS);

        super.start(period, unit);
    }

    @Override
    protected void report(final Collection<InfluxSeries> allSeries) {
        for (final InfluxSeries series : allSeries) {
            for (DataPoint dataPoint : series.getValueSets()) {
                final Point.Builder pointBuilder = Point.measurement(series.getMeasurementName()).time(dataPoint.getTime(), dataPoint.getPrecision()).tag(series.getTags());

                pointBuilder.fields(dataPoint.getFields());
                pointBuilder.tag(tags);

                final Point point = pointBuilder.build();
                logger.trace("Writing point '{}' into '{}' using retention policy '{}'", point, database, retentionPolicy);
                influxDB.write(database, retentionPolicy, point);
            }
        }
    }

    public static class Builder {
        private final InfluxSeriesRegistry registry;
        private String httpConnection = "http://localhost:8086";
        private String username = "admin";
        private String password = "admin";
        private String database = "influx_metrics";
        private List<MetricTag> tags = new ArrayList<>();
        private String retentionPolicy = "default";
        private boolean reportOwnStatistics = true;

        public Builder(final InfluxSeriesRegistry registry) {
            this.registry = registry;
        }

        public Builder withHttpConnection(String hostname, String port) {
            httpConnection = String.format("http://%s:%s", hostname, port);
            return this;
        }

        public Builder withRetentionPolicy(final String retentionPolicy) {
            this.retentionPolicy = retentionPolicy;
            return this;
        }

        public Builder withUser(String username, String password) {
            this.username = username;
            this.password = password;

            return this;
        }

        public Builder tag(String key, String value) {
            tags.add(new MetricTag(key, value));
            return this;
        }

        public Builder onDatabase(String database) {
            this.database = database;
            return this;
        }

        public Builder reportOwnStatistics(boolean reportOwnStatistics) {
            this.reportOwnStatistics = reportOwnStatistics;
            return this;
        }

        public InfluxdbHttpReporter build() {
            return new InfluxdbHttpReporter(registry, httpConnection, username, password, database, tags, retentionPolicy, reportOwnStatistics);
        }
    }
}
