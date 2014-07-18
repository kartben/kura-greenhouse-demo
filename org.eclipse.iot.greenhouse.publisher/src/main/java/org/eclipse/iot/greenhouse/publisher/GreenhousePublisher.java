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
package org.eclipse.iot.greenhouse.publisher;

import java.util.Map;

import org.eclipse.iot.greenhouse.sensors.GreenhouseSensorService;
import org.eclipse.iot.greenhouse.sensors.GreenhouseSensorService.NoSuchSensorOrActuatorException;
import org.eclipse.iot.greenhouse.sensors.GreenhouseSensorService.SensorChangedListener;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraNotConnectedException;
import org.eclipse.kura.KuraTimeoutException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.DataServiceListener;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GreenhousePublisher implements ConfigurableComponent,
		DataServiceListener, SensorChangedListener {
	private static final Logger s_logger = LoggerFactory
			.getLogger(GreenhousePublisher.class);

	private static final String PUBLISH_TOPICPREFIX_PROP_NAME = "publish.appTopicPrefix";
	private static final String PUBLISH_QOS_PROP_NAME = "publish.qos";
	private static final String PUBLISH_RETAIN_PROP_NAME = "publish.retain";

	private DataService _dataService;
	private GreenhouseSensorService _greenhouseSensorService;

	private Map<String, Object> _properties;

	// ----------------------------------------------------------------
	//
	// Dependencies
	//
	// ----------------------------------------------------------------

	public GreenhousePublisher() {
		super();
	}

	protected void setGreenhouseSensorService(
			GreenhouseSensorService greenhouseSensorService) {
		_greenhouseSensorService = greenhouseSensorService;
		_greenhouseSensorService.addSensorChangedListener(this);
	}

	protected void unsetGreenhouseSensorService(
			GreenhouseSensorService greenhouseSensorService) {
		_greenhouseSensorService.removeSensorChangedListener(this);
		_greenhouseSensorService = null;
	}

	public void setDataService(DataService dataService) {
		_dataService = dataService;
	}

	public void unsetDataService(DataService dataService) {
		_dataService = null;
	}

	// ----------------------------------------------------------------
	//
	// Activation APIs
	//
	// ----------------------------------------------------------------

	protected void activate(ComponentContext componentContext,
			Map<String, Object> properties) {
		_properties = properties;
		s_logger.info("Activating GreenhousePublisher... Done.");
	}

	protected void deactivate(ComponentContext componentContext) {
		s_logger.debug("Deactivating GreenhousePublisher... Done.");
	}

	public void updated(Map<String, Object> properties) {
		_properties = properties;
	}

	@Override
	public void onConnectionEstablished() {
		try {
			String prefix = (String) _properties
					.get(PUBLISH_TOPICPREFIX_PROP_NAME);

			_dataService.subscribe(prefix + "#", 0);
		} catch (KuraTimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KuraNotConnectedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KuraException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void onDisconnecting() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onConnectionLost(Throwable cause) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMessageArrived(String topic, byte[] payload, int qos,
			boolean retained) {
		String prefix = (String) _properties.get(PUBLISH_TOPICPREFIX_PROP_NAME);

		if (!topic.startsWith(prefix)) {
			return;
		}

		String[] topicFragments = topic.split("/");
		// topicFragments[0] == {appSetting.topic_prefix}
		// topicFragments[1] == {unique_id}
		// topicFragments[2] == "actuators"
		// topicFragments[3] == {actuatorName} (e.g. light)

		if (topicFragments.length != 4)
			return;

		if (topicFragments[2].equals("actuators")
				&& topicFragments[3].equals("light")) {
			try {
				_greenhouseSensorService.setActuatorValue("light", new String(
						payload));
			} catch (NoSuchSensorOrActuatorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onMessagePublished(int messageId, String topic) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMessageConfirmed(int messageId, String topic) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sensorChanged(String sensorName, Object newValue) {
		// Publish the message
		String prefix = (String) _properties.get(PUBLISH_TOPICPREFIX_PROP_NAME);
		Integer qos = (Integer) _properties.get(PUBLISH_QOS_PROP_NAME);
		Boolean retain = (Boolean) _properties.get(PUBLISH_RETAIN_PROP_NAME);

		String topic = prefix + "sensors/" + sensorName;
		String payload = newValue.toString();

		try {

			int messageId = _dataService.publish(topic, payload.getBytes(),
					qos, retain, 2);
			s_logger.info("Published to {} message: {} with ID: {}",
					new Object[] { topic, payload, messageId });
		} catch (Exception e) {
			s_logger.error("Cannot publish topic: " + topic, e);
		}
	}
}
