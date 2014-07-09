package org.eclipse.iot.greenhouse.coap;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.server.resources.CoapExchange;

public class SensorResource extends CoapResource {

	private String sensorValue;

	public SensorResource(String name) {
		super(name);
		setObservable(true);
	}

	public String getSensorValue() {
		return sensorValue;
	}

	public void setSensorValue(String sensorValue) {
		this.sensorValue = sensorValue;
		this.changed();
	}

	@Override
	public void handleGET(CoapExchange exchange) {
		exchange.respond(sensorValue);
	}

}
