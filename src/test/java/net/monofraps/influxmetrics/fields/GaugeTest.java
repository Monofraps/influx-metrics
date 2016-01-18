package net.monofraps.influxmetrics.fields;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author monofraps
 */
public class GaugeTest {
    @Test
    public void constructorStoresName() throws Exception {
        final String name = RandomStringUtils.random(5, true, true);
        final Gauge<Integer> gauge = new Gauge<>(name, () -> 0);

        assertEquals(name, gauge.getName());
    }
}
