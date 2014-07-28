package org.eclipse.iot.greenhouse.sensors.raspberrypi;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.iot.greenhouse.sensors.GreenhouseSensorService;
import org.eclipse.iot.greenhouse.sensors.SensorChangedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalMultipurpose;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

public class Pi4JGreenhouseSensorService implements GreenhouseSensorService {
	private static final Logger s_logger = LoggerFactory
			.getLogger(Pi4JGreenhouseSensorService.class);

	private List<SensorChangedListener> _listeners = new CopyOnWriteArrayList<SensorChangedListener>();
	private I2CBus _i2cbus;
	private I2CDevice _temperatureSensor;
	private GpioController _gpioController;
	private GpioPinDigitalMultipurpose _lightActuator;

	private float _temperatureRef = Float.MIN_VALUE;

	private ScheduledThreadPoolExecutor _scheduledThreadPoolExecutor;
	private ScheduledFuture<?> _handle;

	protected void activate() {
		try {
			_gpioController = GpioFactory.getInstance();
			_i2cbus = I2CFactory.getInstance(I2CBus.BUS_1);

			_temperatureSensor = _i2cbus.getDevice(0x40);
			_lightActuator = _gpioController.provisionDigitalMultipurposePin(
					RaspiPin.GPIO_00, "led", PinMode.DIGITAL_OUTPUT);
			_lightActuator.setShutdownOptions(true); // unexport on shutdown

			// monitor temperature changes
			// every change of more than 0.1C will notify SensorChangedListeners
			_scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
			_handle = _scheduledThreadPoolExecutor.scheduleAtFixedRate(
					new Runnable() {
						@Override
						public void run() {
							try {
								float newTemperature = readTemperature();
								if (Math.abs(_temperatureRef - newTemperature) > .1f) {
									notifyListeners("temperature",
											newTemperature);
									_temperatureRef = newTemperature;
								}
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}, 0, 100, TimeUnit.MILLISECONDS);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void deactivate() {
		s_logger.info("Deactivating Pi4JGreenhouseSensorService...");

		if (_gpioController != null) {
			s_logger.info("... unexport all GPIOs");
			_gpioController.unexportAll();
			s_logger.info("... shutdown");
			_gpioController.shutdown();
			s_logger.info("... DONE.");
		}

		if (_handle != null) {
			_handle.cancel(true);
		}

		s_logger.info("Deactivating Pi4JGreenhouseSensorService... Done.");
	}

	@Override
	public Object getSensorValue(String sensorName)
			throws NoSuchSensorOrActuatorException {
		if ("temperature".equals(sensorName)) {
			try {
				return readTemperature();
			} catch (IOException e) {
				return new NoSuchSensorOrActuatorException();
			}
		} else if ("light".equals(sensorName)) {
			return readLightState();
		} else
			throw new GreenhouseSensorService.NoSuchSensorOrActuatorException();
	}

	/*
	 * See sensor documentation here:
	 * http://www.hoperf.cn/upload/sensor/TH02_V1.1.pdf
	 */
	private synchronized float readTemperature() throws IOException {
		float temperature;
		// Set START (D0) and TEMP (D4) in CONFIG (register 0x03) to begin a
		// new conversion, i.e., write CONFIG with 0x11
		_temperatureSensor.write(0x03, (byte) 0x11);

		try {
			// waiting a bit to try to avoid getting erroneous readings once in
			// a while
			Thread.sleep(50);
		} catch (InterruptedException e) {
		}

		// Poll RDY (D0) in STATUS (register 0) until it is low (=0)
		int status = -1;
		while ((status & 0x01) != 0) {
			status = _temperatureSensor.read(0x00);
		}

		// Read the upper and lower bytes of the temperature value from
		// DATAh and DATAl (registers 0x01 and 0x02), respectively
		byte[] buffer = new byte[3];
		_temperatureSensor.read(buffer, 0, 3);

		int dataH = buffer[1];
		int dataL = buffer[2];

		temperature = ((dataH << 8) + dataL) >> 2;
		temperature = (temperature / 32f) - 50f;

		// truncate to 2 decimals
		DecimalFormat twoDForm = new DecimalFormat("#.##");
		return Float.valueOf(twoDForm.format(temperature));
	}

	private boolean readLightState() {
		return _lightActuator.getState().isHigh();
	}

	@Override
	public void setActuatorValue(String actuatorName, Object value)
			throws NoSuchSensorOrActuatorException {
		if ("light".equals(actuatorName)) {
			_lightActuator.setState("on".equals(value));
			notifyListeners("light", value);
		} else {
			throw new GreenhouseSensorService.NoSuchSensorOrActuatorException();
		}
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
