package net.monofraps.influxmetrics.fields;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import java.security.SecureRandom;

import static org.junit.Assert.*;

/**
 * @author monofraps
 */
public class PassiveGaugeTest {
    @Test
    public void constructorStoresName() throws Exception {
        final String name = RandomStringUtils.random(5, true, true);
        final PassiveGauge<Integer> gauge = new PassiveGauge<>(name, 0);

        assertEquals(name, gauge.getName());
    }

    @Test
    public void constructorStoresInitialValue() throws Exception {
        final Integer value = SecureRandom.getInstanceStrong().nextInt();
        final PassiveGauge<Integer> gauge = new PassiveGauge<>("", value);

        assertEquals(value, gauge.getValue());
    }

    @Test
    public void canSetValue() throws Exception {
        final Integer value = SecureRandom.getInstanceStrong().nextInt();
        final PassiveGauge<Integer> gauge = new PassiveGauge<>("", 0);

        gauge.setValue(value);
        assertEquals(value, gauge.getValue());
    }
}
