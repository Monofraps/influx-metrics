package net.monofraps.influxmetrics.internal;

import net.monofraps.influxmetrics.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.util.Collection;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Registers a dynamic MBean for each time series in a series registry.
 * @author monofraps
 */
public class JmxReporter implements RegistryEventListener {
    private static final String MBEAN_DOMAIN = "net.monofraps.metrics";
    private final MBeanServer mBeanServer;
    private final InfluxSeriesRegistry seriesRegistry;
    private final String mBeanDomain;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public JmxReporter(MBeanServer mBeanServer, InfluxSeriesRegistry seriesRegistry) {
        this(mBeanServer, seriesRegistry, MBEAN_DOMAIN);
    }

    public JmxReporter(MBeanServer mBeanServer, InfluxSeriesRegistry seriesRegistry, String mBeanDomain) {
        this.mBeanServer = mBeanServer;
        this.seriesRegistry = seriesRegistry;
        this.mBeanDomain = mBeanDomain;
    }

    public void start() {
        seriesRegistry.registerEventListener(this);
        seriesRegistry.getSeries().forEach(this::registerSeries);
    }

    public void stop() {
        seriesRegistry.getSeries().forEach(this::unregisterSeries);
        seriesRegistry.removeEventListener(this);

    }

    private void registerSeries(final InfluxSeries influxSeries) {
        try {
            if(influxSeries instanceof EventSeries) {
                return;
            }

            final ObjectName objectName = createObjectName(influxSeries);
            if (mBeanServer.isRegistered(objectName)) {
                logger.info("Skipping registration of {} since the series is already registered with the MBean server", influxSeries.getMeasurementName());
                return;
            }

            final InfluxSeriesMBean bean = new InfluxSeriesMBean(influxSeries);
            mBeanServer.registerMBean(bean, objectName);
        } catch (MalformedObjectNameException | NotCompliantMBeanException | InstanceAlreadyExistsException | MBeanRegistrationException e) {
            logger.error("Failed to register influx series with JMX reporter", e);
        }
    }

    private ObjectName createObjectName(final InfluxSeries influxSeries) throws MalformedObjectNameException {
        final Hashtable<String, String> keyBeanProperties = new Hashtable<>();

        keyBeanProperties.put("_name", influxSeries.getMeasurementName());
        keyBeanProperties.putAll(influxSeries.getTags());

        return new ObjectName(mBeanDomain, keyBeanProperties);
    }

    @Override
    public void onSeriesRegistered(InfluxSeries series) {
        registerSeries(series);
    }

    @Override
    public void onSeriesRemoved(InfluxSeries series) {
        unregisterSeries(series);
    }

    private void unregisterSeries(InfluxSeries series) {
        try {
            final ObjectName objectName = createObjectName(series);
            if(!mBeanServer.isRegistered(objectName)) {
                logger.info("Skipping un-registration of {} since the series is not registered with the MBean server", series.getMeasurementName());
            }

            mBeanServer.unregisterMBean(objectName);
        } catch (MalformedObjectNameException | InstanceNotFoundException | MBeanRegistrationException e) {
            logger.error("Failed to un-register influx series", e);
        }
    }

    public class InfluxSeriesMBean implements DynamicMBean {
        private final InfluxSeries timeSeries;
        private final Logger logger = LoggerFactory.getLogger(getClass());

        public InfluxSeriesMBean(InfluxSeries timeSeries) {
            this.timeSeries = timeSeries;
        }

        @Override
        public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
            final Collection<DataPoint> valueSets = timeSeries.getValueSets();
            if (valueSets.isEmpty()) {
                throw new RuntimeOperationsException(new RuntimeException("No datapoints available"));
            }

            return valueSets.iterator().next().getFields().get(attribute);
        }

        @Override
        public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
            throw new MBeanException(new OperationsException("Cannot set attribute"));
        }

        @Override
        public AttributeList getAttributes(String[] attributes) {
            final AttributeList attributeList = new AttributeList();
            for (String attribute : attributes) {
                try {
                    attributeList.add(getAttribute(attribute));
                } catch (AttributeNotFoundException | MBeanException | ReflectionException e) {
                    logger.error(String.format("Failed to retrieve attribute value for %s", attribute), e);
                }
            }

            return attributeList;
        }

        @Override
        public AttributeList setAttributes(AttributeList attributes) {
            return null;
        }

        @Override
        public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
            return null;
        }

        @Override
        public MBeanInfo getMBeanInfo() {
            final List<MBeanAttributeInfo> mBeanAttributes = timeSeries.getFieldNames().stream().map(fieldName -> new MBeanAttributeInfo(fieldName, Object.class.getCanonicalName(), "", true, false, false)).collect(Collectors.toCollection(LinkedList::new));
            return new MBeanInfo(InfluxSeriesMBean.class.getCanonicalName(), "", mBeanAttributes.toArray(new MBeanAttributeInfo[0]), null, null, null);
        }
    }
}
