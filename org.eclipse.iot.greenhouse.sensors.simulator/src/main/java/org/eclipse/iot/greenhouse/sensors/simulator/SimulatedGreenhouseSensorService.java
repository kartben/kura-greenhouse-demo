package org.eclipse.iot.greenhouse.sensors.simulator;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.iot.greenhouse.sensors.SensorService;
import org.eclipse.iot.greenhouse.sensors.SensorChangedListener;

public class SimulatedGreenhouseSensorService implements
		SensorService {

	private ScheduledThreadPoolExecutor _scheduledThreadPoolExecutor;
	private ScheduledFuture<?> _handle;

	private List<SensorChangedListener> _listeners = new CopyOnWriteArrayList<SensorChangedListener>();

	float _temperature = 20;
	String _lightState = "on";

	public SimulatedGreenhouseSensorService() {
		super();
		_scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);

		_handle = _scheduledThreadPoolExecutor.scheduleAtFixedRate(
				new Runnable() {
					@Override
					public void run() {
						_temperature = 20 + new Random().nextFloat() * 10;
						notifyListeners("temperature", _temperature);

						_lightState = (new Random().nextInt(10) < 8) ? "on"
								: "off";
						notifyListeners("light", _lightState);
					}
				}, 0, 1000, TimeUnit.MILLISECONDS);

	}

	@Override
	public Object getSensorValue(String sensorName)
			throws NoSuchSensorOrActuatorException {
		if ("temperature".equals(sensorName))
			return _temperature;
		else if ("light".equals(sensorName))
			return _lightState;
		else
			throw new SensorService.NoSuchSensorOrActuatorException();
	}

	@Override
	public void setActuatorValue(String actuatorName, Object value)
			throws NoSuchSensorOrActuatorException {
		if ("light".equals(actuatorName)) {
			_lightState = (String) value;
			notifyListeners("light", value);
		} else
			throw new SensorService.NoSuchSensorOrActuatorException();

	}

	public void addSensorChangedListener(SensorChangedListener listener) {
		_listeners.add(listener);
	}

	public void removeSensorChangedListener(SensorChangedListener listener) {
		_listeners.remove(listener);
	}

	private void notifyListeners(String sensorName, Object newValue) {
		for (SensorChangedListener listener : _listeners) {
			listener.sensorChanged(sensorName, newValue);
		}
	}

}
