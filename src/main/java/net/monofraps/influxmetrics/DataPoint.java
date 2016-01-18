package net.monofraps.influxmetrics;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Model of a single data point.
 * A data point has a timestamp, a corresponding precision and a map of field names and values.
 * @author monofraps
 */
public class DataPoint {
    private final long time;
    private final TimeUnit precision;
    private final Map<String, Object> fields;

    public DataPoint(long time, TimeUnit precision, Map<String, Object> fields) {
        this.time = time;
        this.precision = precision;
        this.fields = fields;
    }

    public long getTime() {
        return time;
    }

    public TimeUnit getPrecision() {
        return precision;
    }

    public Map<String, Object> getFields() {
        return fields;
    }
}
