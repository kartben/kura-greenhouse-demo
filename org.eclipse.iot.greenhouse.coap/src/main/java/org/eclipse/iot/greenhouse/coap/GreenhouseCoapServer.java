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

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.iot.greenhouse.sensors.SensorService;
import org.eclipse.iot.greenhouse.sensors.SensorService.NoSuchSensorOrActuatorException;
import org.eclipse.iot.greenhouse.sensors.SensorChangedListener;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GreenhouseCoapServer implements SensorChangedListener {
	private static final Logger s_logger = LoggerFactory
			.getLogger(GreenhouseCoapServer.class);
	private CoapServer _coapServer;

	private CoapResource _greenhouseResource;

	private CoapResource _sensorsRootResource;

	private CoapResource _actuatorsRootResource;

	private SensorService _sensorService;

	// ----------------------------------------------------------------
	//
	// Dependencies
	//
	// ----------------------------------------------------------------

	public GreenhouseCoapServer() {
		super();

		_greenhouseResource = new CoapResource("gh");
		_sensorsRootResource = new CoapResource("sens");
		_actuatorsRootResource = new CoapResource("act");

		_greenhouseResource.add(_sensorsRootResource, _actuatorsRootResource);
	}

	protected void setGreenhouseSensorService(
			SensorService sensorService) {
		_sensorService = sensorService;
	}

	protected void unsetGreenhouseSensorService(
			SensorService sensorService) {
		_sensorService = null;
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
				_sensorService);
		SensorResource lightSensor = new SensorResource("light");
		// TODO remove line below
		lightSensor.setSensorValue("on");
		lightActuator.setSensorResource(lightSensor);
		_sensorsRootResource.add(lightSensor);
		_actuatorsRootResource.add(lightActuator);

		// add the temperature sensor
		try {
			SensorResource temperatureResource = new SensorResource(
					"temperature");
			temperatureResource.setSensorValue(""
					+ _sensorService.getSensorValue("temperature"));
			_sensorsRootResource.add(temperatureResource);
		} catch (NoSuchSensorOrActuatorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		s_logger.info("Activating GreenhouseCoapServer... Done.");
	}

	protected void deactivate(ComponentContext componentContext) {
		s_logger.debug("Deactivating GreenhouseCoapServer...");

		_coapServer.stop();

		s_logger.debug("Deactivating GreenhouseCoapServer... Done.");
	}

	@Override
	public void sensorChanged(String sensorName, Object newValue) {
		SensorResource sensorResource = (SensorResource) _sensorsRootResource
				.getChild(sensorName);
		if (sensorResource != null) {
			sensorResource.setSensorValue(newValue.toString());
		}
	}

}
