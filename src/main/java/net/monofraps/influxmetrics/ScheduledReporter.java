package net.monofraps.influxmetrics;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author monofraps
 */
public abstract class ScheduledReporter {
	private static final Logger logger = LoggerFactory.getLogger(ScheduledReporter.class);
	private static final AtomicInteger FACTORY_ID = new AtomicInteger(0);
	private final ScheduledExecutorService executor;
	private final Collection<InfluxSeriesRegistry> registries;
	private long reportIntervalInMs;
	protected ScheduledReporter(Collection<InfluxSeriesRegistry> registries, String name) {
		this(registries, Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory(name + '-' + FACTORY_ID.incrementAndGet())));
	}

	protected ScheduledReporter(Collection<InfluxSeriesRegistry> registries, ScheduledExecutorService executor) {
		this.registries = ImmutableList.copyOf(registries);
		this.executor = executor;
	}

	public void start(long period, TimeUnit unit) {
		reportIntervalInMs = TimeUnit.MILLISECONDS.convert(period, unit);

		executor.scheduleAtFixedRate((Runnable) () -> {
			try {
				report();
			} catch (RuntimeException ex) {
				logger.error("RuntimeException thrown from {}#report. Exception was suppressed.", ScheduledReporter.this.getClass().getSimpleName(), ex);
			}
		}, period, period, unit);
	}

	public void report() {
		Stopwatch stopwatch = Stopwatch.createStarted();
		synchronized (this) {
			registries.forEach(this::report);
		}
		stopwatch.stop();

		final long reportTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
		if (reportTime >= reportIntervalInMs * .95f) {
			logger.warn("Metric reporting took {} ms and reporting interval is set to {} ms", reportTime, reportIntervalInMs);
		}

		logger.debug("Metric reporting took {} ms", reportTime);
		postReport(reportTime);
	}

	protected void postReport(long reportTime) {}

	public void stop() {
		executor.shutdown();

		try {
			if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
				executor.shutdownNow();
				if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
					logger.error("ScheduledExecutorService did not terminate");
				}
			}
		} catch (InterruptedException ie) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	protected abstract void report(final InfluxSeriesRegistry registry);

	/**
	 * A simple named thread factory.
	 */
	private static class NamedThreadFactory implements ThreadFactory {
		private final ThreadGroup group;
		private final AtomicInteger threadNumber = new AtomicInteger(1);
		private final String namePrefix;

		private NamedThreadFactory(String name) {
			final SecurityManager securityManager = System.getSecurityManager();
			this.group = (securityManager != null) ? securityManager.getThreadGroup() : Thread.currentThread().getThreadGroup();
			this.namePrefix = "InfluxMetricsReporter-" + name;
		}

		@Override
		public Thread newThread(Runnable target) {
			final Thread thread = new Thread(group, target, namePrefix + threadNumber.getAndIncrement(), 0);
			thread.setDaemon(true);

			if (thread.getPriority() != Thread.NORM_PRIORITY) {
				thread.setPriority(Thread.NORM_PRIORITY);
			}
			return thread;
		}
	}
}
