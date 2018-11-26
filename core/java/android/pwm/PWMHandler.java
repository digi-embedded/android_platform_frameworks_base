/*
 * Copyright 2018, Digi International Inc.
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
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
