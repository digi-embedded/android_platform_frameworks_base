/*
 * Copyright (C) 2016-2018 Digi International Inc., All Rights Reserved
 *
 * This software contains proprietary and confidential information of Digi.
 * International Inc. By accepting transfer of this copy, Recipient agrees 
 * to retain this software in confidence, to prevent disclosure to others, 
 * and to make no use of this software other than that for which it was 
 * delivered. This is an unpublished copyrighted work of Digi International
 * Inc. Except as permitted by federal law, 17 USC 117, copying is strictly 
 * prohibited.
 *
 * Restricted Rights Legend
 *
 * Use, duplication, or disclosure by the Government is subject to restrictions
 * set forth in sub-paragraph (c)(1)(ii) of The Rights in Technical Data and
 * Computer Software clause at DFARS 252.227-7031 or subparagraphs (c)(1) and
 * (2) of the Commercial Computer Software - Restricted Rights at 48 CFR
 * 52.227-19, as applicable.
 * 
 * Digi International Inc. 11001 Bren Road East, Minnetonka, MN 55343
 */

package android.gpio;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

/**
 * This class provides access to GPIOs.
 * 
 * <p>This handler allows applications to create {@code GPIO} objects.</p>
 *
 * <p>Unless noted, all GPIO API methods require the {@code com.digi.android.permission.GPIO} 
 * permission. If your application does not have this permission it will not 
 * have access to any GPIO service feature.</p>
 * 
 * @hide
 */
public class GPIOHandler {
	
	// Constants.
	/** @hide */
	public static final String ID_EXCEPTION = "ex";
	/** @hide */
	public static final String ID_DIRECTION = "dir";
	/** @hide */
	public static final String ID_EDGE = "ed";
	/** @hide */
	public static final String ID_VALUE = "val";
	
	private static final String TAG = "GPIOHandler";
	
	/** @hide */
	public final static String ERROR_KERNEL_NUMBER = "GPIO kernel number must be greater than -1";
	/** @hide */
	public final static String ERROR_PORT_INDEX = "GPIO base port index must be greater than -1";
	/** @hide */
	public final static String ERROR_PIN_INDEX = "GPIO pin index must be greater than -1";
	
	private static final int GPIO_PINS = 32;
	
	// Variables.
	private final IGPIOManager service;
	
	/**
	 * @hide
	 */
	public GPIOHandler(Context context, IGPIOManager service) {
		this.service = service;
	}
	
	/**
	 * Creates a GPIO with the given kernel GPIO number.
	 * 
	 * @param kernelNumber Kernel number of the GPIO to be created.
	 * 
	 * @throws GPIOExceptionImpl If there is an error creating the GPIO.
	 * @throws IllegalArgumentException If {@code kernelNumber < 0}.
	 */
	public void createGPIO(int kernelNumber) throws GPIOExceptionImpl {
		// Check parameter values.
		if (kernelNumber < 0) {
			Log.e(TAG, ERROR_KERNEL_NUMBER);
			throw new IllegalArgumentException(ERROR_KERNEL_NUMBER);
		}
		
		try {
			Bundle b = service.createGpio(kernelNumber);
			if (b.containsKey(ID_EXCEPTION))
				throw (GPIOExceptionImpl) b.getParcelable(ID_EXCEPTION);
		} catch (RemoteException e) {
			Log.e(TAG, "RemoteException in createGpio(int)", e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Creates a GPIO with the given GPIO port number and pin offset.
	 * 
	 * @param portIndex GPIO base port index.
	 * @param pinIndex GPIO pin index.
	 * 
	 * @return The GPIO created with the desired mode.
	 * 
	 * @throws GPIOExceptionImpl If there is an error creating the GPIO.
	 * @throws IllegalArgumentException If {@code portIndex < 0} or
	 *                                  if {@code pinIndex < 0}.
	 */
	public void createGPIO(int portIndex, int pinIndex) throws GPIOExceptionImpl {
		// Check parameter values.
		if (portIndex < 0) {
			Log.e(TAG, ERROR_PORT_INDEX);
			throw new IllegalArgumentException(ERROR_PORT_INDEX);
		}
		if (pinIndex < 0) {
			Log.e(TAG, ERROR_PIN_INDEX);
			throw new IllegalArgumentException(ERROR_PIN_INDEX);
		}
		
		try {
			Bundle b = service.createGpio(calculateKernelNumber(portIndex, pinIndex));
			if (b.containsKey(ID_EXCEPTION))
				throw (GPIOExceptionImpl) b.getParcelable(ID_EXCEPTION);
		} catch (RemoteException e) {
			Log.e(TAG, "RemoteException in createGpio(int, int)", e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Calculates the GPIO kernel number from the given port and pin indexes.
	 * 
	 * @param portIndex Base port index.
	 * @param pinIndex Pin index inside the given port.
	 * 
	 * @return The GPIO kernel number.
	 * 
	 * @hide
	 */
	public static int calculateKernelNumber(int portIndex, int pinIndex) {
		return (portIndex - 1) * GPIO_PINS + pinIndex;
	}
	
	/**
	 * Calculates the GPIO base port index from the given kernel number.
	 * 
	 * @param kernelNumber GPIO kernel number.
	 * 
	 * @return The base port index.
	 * 
	 * @hide
	 */
	public static int calculatePortIndex(int kernelNumber) {
		return (kernelNumber / GPIO_PINS) + 1;
	}
	
	/**
	 * Calculates the GPIO pin index from the given kernel number.
	 * 
	 * @param kernelNumber GPIO kernel number.
	 * 
	 * @return The pin index.
	 * 
	 * @hide
	 */
	public static int calculatePinIndex(int kernelNumber) {
		return kernelNumber % GPIO_PINS;
	}
	
	/**
	 * @hide
	 */
	public String getDirection(int number) throws GPIOExceptionImpl {
		try {
			Bundle b = service.getDirection(number);
			if (b.containsKey(ID_EXCEPTION))
				throw (GPIOExceptionImpl) b.getParcelable(ID_EXCEPTION);
			return b.getString(ID_DIRECTION);
		} catch (RemoteException e) {
			Log.e(TAG, "RemoteException in getDirection", e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @hide
	 */
	public void setDirection(int number, String direction) throws GPIOExceptionImpl {
		try {
			Bundle b = service.setDirection(number, direction);
			if (b.containsKey(ID_EXCEPTION))
				throw (GPIOExceptionImpl) b.getParcelable(ID_EXCEPTION);
		} catch (RemoteException e) {
			Log.e(TAG, "RemoteException in setDirection", e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @hide
	 */
	public String getEdge(int number) throws GPIOExceptionImpl {
		try {
			Bundle b = service.getEdge(number);
			if (b.containsKey(ID_EXCEPTION))
				throw (GPIOExceptionImpl) b.getParcelable(ID_EXCEPTION);
			return b.getString(ID_EDGE);
		} catch (RemoteException e) {
			Log.e(TAG, "RemoteException in getEdge", e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @hide
	 */
	public void setEdge(int number, String edge) throws GPIOExceptionImpl {
		try {
			Bundle b = service.setEdge(number, edge);
			if (b.containsKey(ID_EXCEPTION))
				throw (GPIOExceptionImpl) b.getParcelable(ID_EXCEPTION);
		} catch (RemoteException e) {
			Log.e(TAG, "RemoteException in setEdge", e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @hide
	 */
	public int getValue(int number) throws GPIOExceptionImpl {
		try {
			Bundle b = service.getValue(number);
			if (b.containsKey(ID_EXCEPTION))
				throw (GPIOExceptionImpl) b.getParcelable(ID_EXCEPTION);
			return b.getInt(ID_VALUE);
		} catch (RemoteException e) {
			Log.e(TAG, "RemoteException in getValue", e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @hide
	 */
	public void setValue(int number, int value) throws GPIOExceptionImpl {
		try {
			Bundle b = service.setValue(number, value);
			if (b.containsKey(ID_EXCEPTION))
				throw (GPIOExceptionImpl) b.getParcelable(ID_EXCEPTION);
		} catch (RemoteException e) {
			Log.e(TAG, "RemoteException in setValue", e);
			throw new RuntimeException(e);
		}
	}
}
