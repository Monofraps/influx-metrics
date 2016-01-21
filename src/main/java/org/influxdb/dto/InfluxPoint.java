package org.influxdb.dto;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Faster implementation of org.influxdb.dto.Point
 */
public class InfluxPoint extends Point {
    private static final Escaper FIELD_ESCAPER = Escapers.builder().addEscape('"', "\\\"").build();
    private static final Escaper KEY_ESCAPER = Escapers.builder().addEscape(' ', "\\ ").addEscape(',', "\\,").addEscape('=', "\\=").build();
    private static final NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);

    static {
        numberFormat.setMaximumFractionDigits(340);
        numberFormat.setGroupingUsed(false);
        numberFormat.setMinimumFractionDigits(1);
    }

    private final String measurement;
    private final Map<String, String> tags;
    private final Long time;
    private final TimeUnit precision;
    private final Map<String, Object> fields;

    InfluxPoint(String measurement, Map<String, String> tags, Long time, TimeUnit precision, Map<String, Object> fields) {
        this.measurement = measurement;
        this.tags = tags;
        this.time = time;
        this.precision = precision;
        this.fields = fields;
    }

    public static InfluxPointBuilder forMeasurement(final String measurement) {
        return new InfluxPointBuilder(measurement);
    }

    @Override
    public String lineProtocol() {
        return KEY_ESCAPER.escape(measurement) + concatenateTags() + concatenateFields() + formatTime();
    }

    private StringBuilder concatenateTags() {
        final StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> tag : this.tags.entrySet()) {
            sb.append(",");
            sb.append(KEY_ESCAPER.escape(tag.getKey())).append("=").append(KEY_ESCAPER.escape(tag.getValue()));
        }
        sb.append(" ");
        return sb;
    }

    private StringBuilder concatenateFields() {
        final StringBuilder sb = new StringBuilder();
        final int fieldCount = fields.size();
        int loops = 0;

        for (Map.Entry<String, Object> field : this.fields.entrySet()) {
            loops++;
            Object value = field.getValue();
            if (value == null) {
                continue;
            }

            sb.append(KEY_ESCAPER.escape(field.getKey())).append("=");
            if (value instanceof String) {
                String stringValue = (String) value;
                sb.append("\"").append(FIELD_ESCAPER.escape(stringValue)).append("\"");
            } else if (value instanceof Integer || value instanceof Long || value instanceof BigInteger) {
                sb.append(value).append("i");
            } else if (value instanceof Number) {
                sb.append(numberFormat.format(value));
            } else {
                sb.append(value);
            }

            if (loops < fieldCount) {
                sb.append(",");
            }
        }

        return sb;
    }

    private StringBuilder formatTime() {
        final StringBuilder sb = new StringBuilder();
        sb.append(" ").append(TimeUnit.NANOSECONDS.convert(this.time, this.precision));
        return sb;
    }

    @Override
    public String toString() {
        return "Point [name=" + measurement + ", time=" + time + ", tags=" + tags + ", precision=" + precision + ", fields=" + fields + "]";
    }

    Map<String, String> getTags() {
        return this.tags;
    }

    public static final class InfluxPointBuilder {
        private final String measurement;
        private final Map<String, String> tags = new HashMap<>(4, .9f);
        private final Map<String, Object> fields = new HashMap<>();
        private Long time;
        private TimeUnit precision = TimeUnit.NANOSECONDS;

        InfluxPointBuilder(final String measurement) {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(measurement), "Point name must not be null or empty.");
            this.measurement = measurement;
        }

        public Point build() {
            Preconditions.checkArgument(this.fields.size() > 0, "Point must have at least one field specified.");

            if (time == null) {
                time = System.currentTimeMillis();
                precision = TimeUnit.MILLISECONDS;
            }

            return new InfluxPoint(measurement, tags, time, precision, fields);
        }

        public InfluxPointBuilder fields(final Map<String, Object> fieldsToAdd) {
            this.fields.putAll(fieldsToAdd);
            return this;
        }

        public InfluxPointBuilder tag(final Map<String, String> tagsToAdd) {
            this.tags.putAll(tagsToAdd);
            return this;
        }

        public InfluxPointBuilder time(final long timeToSet, final TimeUnit precisionToSet) {
            Preconditions.checkNotNull(precisionToSet, "Precision must be not null!");
            this.time = timeToSet;
            this.precision = precisionToSet;
            return this;
        }
    }
}
