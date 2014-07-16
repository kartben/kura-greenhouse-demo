package org.eclipse.iot.greenhouse.sensors;

public interface GreenhouseSensorService {
	class NoSuchSensorOrActuatorException extends Exception {
		private static final long serialVersionUID = 2612352095893222404L;
	};

	interface SensorChangedListener {
		/**
		 * Callback called when the sensor value has been updated do to an
		 * external event
		 * 
		 * @param newValue
		 *            new sensor value
		 */
		void sensorChanged(String sensorName, Object newValue);
	}

	/**
	 * @return current sensor value
	 */
	Object getSensorValue(String sensorName) throws NoSuchSensorOrActuatorException;

	void setActuatorValue(String actuatorName, Object value) throws NoSuchSensorOrActuatorException;

	/**
	 * Register a new listener that will be notified of sensor updates
	 * 
	 * @param listener
	 *            the listener to be added
	 */
	void addSensorChangedListener(SensorChangedListener listener);

	/**
	 * Remove a sensor changed listener
	 * 
	 * @param listener
	 *            the listener to remove
	 */
	void removeSensorChangedListener(SensorChangedListener listener);

}
