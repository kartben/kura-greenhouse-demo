package org.eclipse.iot.greenhouse.sensors;

public interface GreenhouseSensorService {
	class NoSuchSensorOrActuatorException extends Exception {
		private static final long serialVersionUID = 2612352095893222404L;
	};

	/**
	 * @return current sensor value
	 */
	Object getSensorValue(String sensorName)
			throws NoSuchSensorOrActuatorException;

	void setActuatorValue(String actuatorName, Object value)
			throws NoSuchSensorOrActuatorException;

}
