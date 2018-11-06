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

package android.system.gpu;

import java.io.IOException;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

/**
 * This class provides access to the GPU.
 *
 * <p>Unless noted, all GPU API methods require the
 * {@code com.digi.android.permission.GPU}
 * permission. If your application does not have this permission it will not
 * have access to any GPU service feature.</p>
 *
 * @hide
 */
public class GPUHandler {

    // Constants.
    public static final String ID_IO_EXCEPTION = "io_ex";
    public static final String ID_ILLEGAL_EXCEPTION = "illegal_ex";
    public static final String ID_MULT = "mult";

    private static final String TAG = "GPUHandler";

    // Variables.
    private final IGPUManager service;

    public GPUHandler(Context context, IGPUManager service) {
        this.service = service;
    }

    /**
     * Returns the current GPU multiplier.
     *
     * <p>The value of the multiplier is between 1 and 64. When set to 1,
     * the GPU is configured with the minimum frequency, while when set to 64
     * it is configured with the maximum one.</p>
     *
     * @return The current GPU multiplier.
     *
     * @throws IOException If an I/O error occurs.
     *
     * @see #setMultiplier(int)
     */
    public int getMultiplier() throws IOException {
        try {
            Bundle b = service.getMultiplier();
            if (b.containsKey(ID_IO_EXCEPTION))
                throw new IOException(b.getString(ID_IO_EXCEPTION));
            return b.getInt(ID_MULT);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getMultiplier", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Configures the new GPU multiplier.
     *
     * <p>The value of the multiplier must be between 1 and 64. When set to 1,
     * the GPU is configured with the minimum frequency, while when set to 64
     * it is configured with the maximum one.</p>
     *
     * <p>Note that in some modules the minimum allowed value of the GPU
     * multiplier may be more than 1, so it is recommended to check the returned
     * value.</p>
     *
     * @param multiplier The new GPU multiplier.
     *
     * @return The configured GPU multiplier.
     *
     * @throws IllegalArgumentException If {@code multiplier < 1} or
     *                                  {@code multiplier > 64}.
     * @throws IOException If an I/O error occurs.
     *
     * @see #getMultiplier()
     */
    public int setMultiplier(int multiplier) throws IOException {
        try {
            Bundle b = service.setMultiplier(multiplier);
            if (b.containsKey(ID_ILLEGAL_EXCEPTION))
                throw new IllegalArgumentException(b.getString(ID_ILLEGAL_EXCEPTION));
            if (b.containsKey(ID_IO_EXCEPTION))
                throw new IOException(b.getString(ID_IO_EXCEPTION));
            return b.getInt(ID_MULT);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in setMultiplier", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the the GPU multiplier that will be set when the module's
     * temperature reaches the hot trip point.
     *
     * <p>The value of the multiplier is between 1 and 64. When set to 1,
     * the GPU is configured with the minimum frequency, while when set to 64
     * it is configured with the maximum one.</p>
     *
     * <p>When the module's temperature drops 10 degrees from the hot trip
     * point, the previous configured multiplier is restored.</p>
     *
     * @return The current GPU multiplier that will be set when the module's
     *         temperature reaches the hot trip point.
     *
     * @throws IOException If an I/O error occurs.
     *
     * @see #setMinMultiplier(int)
     */
    public int getMinMultiplier() throws IOException {
        try {
            Bundle b = service.getMinMultiplier();
            if (b.containsKey(ID_IO_EXCEPTION))
                throw new IOException(b.getString(ID_IO_EXCEPTION));
            return b.getInt(ID_MULT);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getMinMultiplier", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Configures the GPU multiplier that will be set when the module's
     * temperature reaches the hot trip point.
     *
     * <p>The value of the multiplier must be between 1 and 64. When set to 1,
     * the GPU is configured with the minimum frequency, while when set to 64
     * it is configured with the maximum one.</p>
     *
     * <p>When the module's temperature drops 10 degrees from the hot trip
     * point, the previous configured multiplier is restored.</p>
     *
     * <p>Note that in some modules the minimum allowed value of the GPU
     * multiplier may be more than 1, so it is recommended to check the returned
     * value.</p>
     *
     * @param multiplier The GPU multiplier that will be set when the module's
     *                   temperature reaches the hot trip point.
     *
     * @return The configured GPU multiplier that will be set when the module's
     *         temperature reaches the hot trip point.
     *
     * @throws IllegalArgumentException If {@code multiplier < 1} or
     *                                  {@code multiplier > 64}.
     * @throws IOException If an I/O error occurs.
     *
     * @see #getMinMultiplier()
     */
    public int setMinMultiplier(int multiplier) throws IOException {
        try {
            Bundle b = service.setMinMultiplier(multiplier);
            if (b.containsKey(ID_ILLEGAL_EXCEPTION))
                throw new IllegalArgumentException(b.getString(ID_ILLEGAL_EXCEPTION));
            if (b.containsKey(ID_IO_EXCEPTION))
                throw new IOException(b.getString(ID_IO_EXCEPTION));
            return b.getInt(ID_MULT);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in setMinMultiplier", e);
            throw new RuntimeException(e);
        }
    }
}
