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

package android.pwm;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

/**
 * This class provides access to PWM.
 *
 * <p>Unless noted, all PWM API methods require the
 * {@code com.digi.android.permission.PWM} permission. If your application
 * does not have this permission it will not have access to any PWM service
 * feature.</p>
 *
 * @hide
 */
public class PWMHandler {

	// Constants.
	/** @hide */
	public static final String ID_EXCEPTION = "ex";
	/** @hide */
	public static final String ID_DUTY_CYCLE = "duty";

	private static final String TAG = "PWMHandler";

	// Variables.
	private final IPWMManager service;

	/**
	 * @hide
	 */
	public PWMHandler(Context context, IPWMManager service) {
		this.service = service;
	}

	/**
	 * Lists all available PWM channels in the device.
	 *
	 * @return List with all available PWM channels in the device.
	 */
	public int[] listChannels() {
		try {
			return service.listChannels();
		} catch (RemoteException e) {
			Log.e(TAG, "RemoteException in listChannels", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * @hide
	 */
	public void setDutyCycle(int channel, double dutyCycle) throws PWMExceptionImpl {
		try {
			Bundle b = service.setDutyCycle(channel, dutyCycle);
			if (b.containsKey(ID_EXCEPTION))
				throw (PWMExceptionImpl) b.getParcelable(ID_EXCEPTION);
		} catch (RemoteException e) {
			Log.e(TAG, "RemoteException in setDutyCycle", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * @hide
	 */
	public double getDutyCycle(int channel) throws PWMExceptionImpl {
		try {
			Bundle b = service.getDutyCycle(channel);
			if (b.containsKey(ID_EXCEPTION))
				throw (PWMExceptionImpl) b.getParcelable(ID_EXCEPTION);
			return b.getDouble(ID_DUTY_CYCLE);
		} catch (RemoteException e) {
			Log.e(TAG, "RemoteException in getDutyCycle", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * @hide
	 */
	public boolean isValidChannel(int channel) {
		try {
			return service.isValidChannel(channel);
		} catch (RemoteException e) {
			Log.e(TAG, "RemoteException in isValidChannel", e);
			throw new RuntimeException(e);
		}
	}
}
