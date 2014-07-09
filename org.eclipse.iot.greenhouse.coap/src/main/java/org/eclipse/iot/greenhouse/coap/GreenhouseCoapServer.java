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
package org.eclipse.iot.greenhouse.coap;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GreenhouseCoapServer implements ConfigurableComponent,
		EventHandler {
	private static final Logger s_logger = LoggerFactory
			.getLogger(GreenhouseCoapServer.class);

	private Map<String, Object> _properties;

	private CoapServer _coapServer;

	private CoapResource _greenhouseResource;

	private CoapResource _sensorsResource;
	private Map<String, SensorResource> _sensorsResources = new HashMap<String, SensorResource>();

	private CoapResource _actuatorsResource;
	private Map<String, ActuatorResource> _actuatorsResources = new HashMap<String, ActuatorResource>();

	private EventAdmin _eventAdmin;

	// ----------------------------------------------------------------
	//
	// Dependencies
	//
	// ----------------------------------------------------------------

	public GreenhouseCoapServer() {
		super();

		_greenhouseResource = new CoapResource("greenhouse");
		_sensorsResource = new CoapResource("sensors");
		_actuatorsResource = new CoapResource("actuators");

		_greenhouseResource.add(_sensorsResource, _actuatorsResource);
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

	protected void activate(ComponentContext componentContext) {
		s_logger.info("Activating GreenhouseCoapServer...");

		_coapServer = new CoapServer();
		_coapServer.add(_greenhouseResource);
		_coapServer.start();

		// create the actuator/sensor combo for the light
		ActuatorResource lightActuator = new ActuatorResource("light",
				_eventAdmin);
		SensorResource lightSensor = new SensorResource("light");
		// TODO remove line below
		lightSensor.setSensorValue("on");
		lightActuator.setSensorResource(lightSensor);
		_sensorsResource.add(lightSensor);
		_sensorsResources.put("light",lightSensor);
		
		_actuatorsResource.add(lightActuator);
		_actuatorsResources.put("light", lightActuator);

		s_logger.info("Activating GreenhouseCoapServer... Done.");
	}

	protected void deactivate(ComponentContext componentContext) {
		s_logger.debug("Deactivating GreenhouseCoapServer...");

		_coapServer.stop();

		s_logger.debug("Deactivating GreenhouseCoapServer... Done.");
	}

	@Override
	public void handleEvent(Event event) {
		// topic looks like "greenhouse-control/sensors/{sensor-name}"
		String topic = event.getTopic();
		String sensorName = topic.substring(topic.lastIndexOf("/") + 1);

		if (!_sensorsResources.containsKey(sensorName)) {
			SensorResource s = new SensorResource(sensorName);
			_sensorsResource.add(s);
			_sensorsResources.put(sensorName, s);
		}

		_sensorsResources.get(sensorName).setSensorValue(
				event.getProperty("sensorvalue").toString());

	}

}
