package org.eclipse.iot.greenhouse.coap;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class ActuatorResource extends CoapResource {
	private EventAdmin _eventAdmin;
	// an actuator might also allow its status to be GET, in that case it has an
	// associated SensorResource
	private SensorResource _sensorResource;

	public ActuatorResource(String name, EventAdmin eventAdmin) {
		super(name);
		_eventAdmin = eventAdmin;
	}

	@Override
	public void handleGET(CoapExchange exchange) {
		if (_sensorResource != null) {
			_sensorResource.handleGET(exchange);
		}
	}

	@Override
	public void handlePUT(CoapExchange exchange) {
		String command = exchange.getRequestText();

		Map<String, Object> dict = new HashMap<String, Object>();
		dict.put("command", command);

		// publish an event to update the actuator
		Event event = new Event("greenhouse-control/actuators/" + getName(),
				dict);
		_eventAdmin.postEvent(event);

		// not sure we want to do this?
		if (_sensorResource != null) {
			_sensorResource.setSensorValue(command);
		}

		exchange.respond(ResponseCode.CHANGED);
	}

	public SensorResource getSensorResource() {
		return _sensorResource;
	}

	public void setSensorResource(SensorResource sensorResource) {
		this._sensorResource = sensorResource;
	}
}
