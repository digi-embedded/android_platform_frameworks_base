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
