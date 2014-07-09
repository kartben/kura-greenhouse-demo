/**
 * Copyright (c) 2014 Eclipse Foundation
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Benjamin Cabé, Eclipse Foundation
 */
package org.eclipse.iot.greenhouse.sensors;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GreenhouseSensorsController implements ConfigurableComponent {
	private static final Logger s_logger = LoggerFactory
			.getLogger(GreenhouseSensorsController.class);

	// Publishing Property Names
	private static final String PUBLISH_RATE_PROP_NAME = "publish.rate";

	private Map<String, Object> _properties;

	private ScheduledThreadPoolExecutor _scheduledThreadPoolExecutor;
	private ScheduledFuture<?> _handle;

	private EventAdmin _eventAdmin;

	// ----------------------------------------------------------------
	//
	// Dependencies
	//
	// ----------------------------------------------------------------

	public GreenhouseSensorsController() {
		super();
		_scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
	}

	protected void setEventAdmin(EventAdmin eventAdmin) {
		_eventAdmin = eventAdmin;
	}

	protected void unsetEventAdmin(EventAdmin eventAdmin) {
		_eventAdmin = null;
	}

	// ----------------------------------------------------------------
	//
	// Activation APIs
	//
	// ----------------------------------------------------------------

	protected void activate(ComponentContext componentContext,
			Map<String, Object> properties) {
		s_logger.info("Activating GreenhouseSensorsController...");

		_properties = properties;
		for (String s : properties.keySet()) {
			s_logger.info("Activate - " + s + ": " + properties.get(s));
		}

		rescheduleWorker();

		s_logger.info("Activating GreenhouseSensorsController... Done.");
	}

	private void rescheduleWorker() {
		// cancel any ongoing handler
		if (_handle != null)
			_handle.cancel(true);

		int pubrate = (Integer) _properties.get(PUBLISH_RATE_PROP_NAME);
		_handle = _scheduledThreadPoolExecutor.scheduleAtFixedRate(
				new Runnable() {
					@Override
					public void run() {
						doReadSensors();
					}
				}, 0, pubrate, TimeUnit.MILLISECONDS);
	}

	protected void deactivate(ComponentContext componentContext) {
		s_logger.debug("Deactivating GreenhouseSensorsController...");

		_scheduledThreadPoolExecutor.shutdown();

		s_logger.debug("Deactivating GreenhouseSensorsController... Done.");
	}

	public void updated(Map<String, Object> properties) {
		s_logger.info("Updated GreenhouseSensorsController...");

		// store the properties received
		_properties = properties;
		for (String s : properties.keySet()) {
			s_logger.info("Update - " + s + ": " + properties.get(s));
		}

		// if pubrate has changed, reschedule timer
		if (properties.containsKey(PUBLISH_RATE_PROP_NAME)) {
			rescheduleWorker();
		}

		s_logger.info("Updated GreenhouseSensorsController... Done.");
	}

	/**
	 * Called at the configured rate to publish the next temperature
	 * measurement.
	 */
	private void doReadSensors() {
		float temperature = 25.0f + new Random().nextFloat() * 5;
		
		Map<String, Object> dict = new HashMap<String, Object>();
		dict.put("sensorvalue", temperature);	
		
		_eventAdmin.postEvent(new Event("greenhouse-control/sensors/temperature", dict));
	}
}
