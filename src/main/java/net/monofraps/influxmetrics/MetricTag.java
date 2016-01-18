package net.monofraps.influxmetrics;

import com.google.common.base.Objects;

public class MetricTag {
	private final String tagName;
	private final String tagValue;

	public MetricTag(String tagName, String tagValue) {
		this.tagName = tagName;
		this.tagValue = tagValue;
	}

	public String getTagName() {
		return tagName;
	}

	public String getTagValue() {
		return tagValue;
	}

	@Override
	public String toString() {
		return tagName + "=" + tagValue;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MetricTag metricTag = (MetricTag) o;
		return Objects.equal(tagName, metricTag.tagName) &&
				Objects.equal(tagValue, metricTag.tagValue);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(tagName, tagValue);
	}
}
