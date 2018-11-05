/*
 * Copyright (C) 2016-2018 Digi International Inc., All Rights Reserved
 *
 * This software contains proprietary and confidential information of Digi,
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

package android.adc;

import java.io.IOException;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

/**
 * This class provides access to the ADC devices.
 *
 * <p>Unless noted, all ADC API methods require the
 * {@code com.digi.android.permission.ADC}
 * permission. If your application does not have this permission it will not
 * have access to any ADC service feature.</p>
 *
 * @hide
 */
public class ADCHandler {

	// Constants.
	/** @hide */
	public static final String ID_EXCEPTION = "ex";
	/** @hide */
	public static final String ID_SAMPLE = "sample";
	/** @hide */
	public final static String ERROR_CHANNEL_NUMBER = "ADC channel number must be greater than -1";

	private static final String TAG = "ADCHandler";

	// Variables.
	private final IADCManager service;

	/**
	 * @hide
	 */
	public ADCHandler(Context context, IADCManager service) {
		this.service = service;
	}

	/**
	 * @hide
	 */
	public int readSample(int channel) throws IOException {
		try {
			Bundle b = service.readSample(channel);
			if (b.containsKey(ID_EXCEPTION))
				throw new IOException(b.getString(ID_EXCEPTION));
			return b.getInt(ID_SAMPLE);
		} catch (RemoteException e) {
			Log.e(TAG, "RemoteException in readSample", e);
			throw new RuntimeException(e);
		}
	}
}
