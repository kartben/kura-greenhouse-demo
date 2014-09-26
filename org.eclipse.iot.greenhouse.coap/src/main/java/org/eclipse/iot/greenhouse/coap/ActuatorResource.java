package org.eclipse.iot.greenhouse.coap;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.iot.greenhouse.sensors.SensorService;
import org.eclipse.iot.greenhouse.sensors.SensorService.NoSuchSensorOrActuatorException;

public class ActuatorResource extends CoapResource {
	private SensorService _greenhouseSensorService;
	// an actuator might also allow its status to be GET, in that case it has an
	// associated SensorResource
	private SensorResource _sensorResource;

	public ActuatorResource(String name,
			SensorService sensorService) {
		super(name);
		_greenhouseSensorService = sensorService;
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

		try {
			_greenhouseSensorService.setActuatorValue("light", command);
		} catch (NoSuchSensorOrActuatorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
