package net.monofraps.influxmetrics.fields;

import org.slf4j.LoggerFactory;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * @author monofraps
 */
public class JmxAttributeGauge extends Gauge<Object>
{
    public JmxAttributeGauge(final String name, final MBeanServer mBeanServer, final ObjectName objectName, final String attribute)
    {
        super(name, () -> {
            try
            {
                return mBeanServer.getAttribute(objectName, attribute);
            }
            catch (JMException e)
            {
                LoggerFactory.getLogger(JmxAttributeGauge.class).error("Failed to get JMX attribute", e);
            }

            return 0;
        });
    }
}
