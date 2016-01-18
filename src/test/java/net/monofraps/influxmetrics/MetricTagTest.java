package net.monofraps.influxmetrics;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author monofraps
 */
public class MetricTagTest {
    @Test
    public void constructorStoresName() throws Exception {
        final String testTagName = RandomStringUtils.random(10);

        final MetricTag metricTag = new MetricTag(testTagName, "");
        assertEquals(testTagName, metricTag.getTagName());
    }

    @Test
    public void constructorStoresValue() throws Exception {
        final String testTagValue = RandomStringUtils.random(10);

        final MetricTag metricTag = new MetricTag("", testTagValue);
        assertEquals(testTagValue, metricTag.getTagValue());
    }

    @Test
    public void comparesEqualIfNameAndValueAreEqual() throws Exception {
        final String testTagName = RandomStringUtils.random(10);
        final String testTagValue = RandomStringUtils.random(10);

        assertTrue((new MetricTag(testTagName, testTagValue)).equals(new MetricTag(testTagName, testTagValue)));
    }

    @Test
    public void comparesNotEqualIfNameDiffers() throws Exception {
        final String testTagName = RandomStringUtils.random(10);
        final String testTagValue = RandomStringUtils.random(10);

        assertFalse((new MetricTag(testTagName, testTagValue)).equals(new MetricTag(RandomStringUtils.random(9), testTagValue)));
    }

    @Test
    public void comparesNotEqualIfLhsNameIsNull() throws Exception {
        final String testTagName = RandomStringUtils.random(10);
        final String testTagValue = RandomStringUtils.random(10);

        assertFalse((new MetricTag(null, testTagValue)).equals(new MetricTag(testTagName, testTagValue)));
    }

    @Test
    public void comparesNotEqualIfRhsNameIsNull() throws Exception {
        final String testTagName = RandomStringUtils.random(10);
        final String testTagValue = RandomStringUtils.random(10);

        assertFalse((new MetricTag(testTagName, testTagValue)).equals(new MetricTag(null, testTagValue)));
    }

    @Test
    public void comparesNotEqualIfValueDiffers() throws Exception {
        final String testTagName = RandomStringUtils.random(10);
        final String testTagValue = RandomStringUtils.random(10);

        assertFalse((new MetricTag(testTagName, testTagValue)).equals(new MetricTag(testTagName, RandomStringUtils.random(9))));
    }

    @Test
    public void comparesNotEqualIfLhsValueIsNull() throws Exception {
        final String testTagName = RandomStringUtils.random(10);
        final String testTagValue = RandomStringUtils.random(10);

        assertFalse((new MetricTag(testTagName, null)).equals(new MetricTag(testTagName, testTagValue)));
    }

    @Test
    public void comparesNotEqualIfRhsValueIsNull() throws Exception {
        final String testTagName = RandomStringUtils.random(10);
        final String testTagValue = RandomStringUtils.random(10);

        assertFalse((new MetricTag(testTagName, testTagValue)).equals(new MetricTag(testTagName, null)));
    }

    @Test
    public void toStringMergesKeyAndValueWithEqualsSign() throws Exception {
        final String testTagName = RandomStringUtils.random(10);
        final String testTagValue = RandomStringUtils.random(10);
        final MetricTag metricTag = new MetricTag(testTagName, testTagValue);

        assertEquals(String.join("=", testTagName, testTagValue), metricTag.toString());
    }
}
