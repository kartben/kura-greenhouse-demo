package org.eclipse.iot.greenhouse.sensors;

public interface SensorChangedListener {
	/**
	 * Callback called when the sensor value has been updated do to an
	 * external event
	 * 
	 * @param newValue
	 *            new sensor value
	 */
	void sensorChanged(String sensorName, Object newValue);
}