package net.monofraps.influxmetrics;

import org.junit.Test;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * @author monofraps
 */
public class DataPointTest {

    @Test
    public void constructorStoresTime() throws Exception {
        final long time = SecureRandom.getInstanceStrong().nextLong();
        final DataPoint dataPoint = new DataPoint(time, TimeUnit.MILLISECONDS, new HashMap<>());

        assertEquals(time, dataPoint.getTime());
    }

    @Test
    public void constructorStoresPrecision() throws Exception {
        final TimeUnit timeUnit = TimeUnit.values()[SecureRandom.getInstanceStrong().nextInt(TimeUnit.values().length)];
        final DataPoint dataPoint = new DataPoint(0, timeUnit, new HashMap<>());

        assertEquals(timeUnit, dataPoint.getPrecision());
    }

    @Test
    public void constructorStoresFields() throws Exception {
        final Map<String, Object> fields = new HashMap<>();

        for (int i = 0; i < SecureRandom.getInstanceStrong().nextLong(); ++i) {
            fields.put(String.valueOf(i), i);
        }

        final DataPoint dataPoint = new DataPoint(0, TimeUnit.MILLISECONDS, fields);
        assertEquals(fields, dataPoint.getFields());
    }
}
