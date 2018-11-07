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

package android.system.memory;

import java.io.IOException;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

/**
 * This class provides access to the system memory.
 *
 * <p>Unless noted, all memory API methods require the
 * {@code com.digi.android.permission.MEMORY} permission. If your application
 * does not have this permission it will not have access to any memory service
 * feature.</p>
 *
 * @hide
 */
public class MemoryHandler {

    // Constants.
    public static final String ID_EX = "ex";
    public static final String ID_MEM = "memory";

    private static final String TAG = "MemoryHandler";

    // Variables.
    private final IMemoryManager service;

    public MemoryHandler(Context context, IMemoryManager service) {
        this.service = service;
    }

    /**
     * Returns the system total memory in kB.
     *
     * @return The system total memory in kB.
     *
     * @throws IOException If an I/O error occurs.
     *
     * @see #getAvailableMemory()
     * @see #getCachedMemory()
     * @see #getFreeMemory()
     */
    public long getTotalMemory() throws IOException {
        try {
            Bundle b = service.getTotalMemory();
            if (b.containsKey(ID_EX))
                throw new IOException(b.getString(ID_EX));
            return b.getLong(ID_MEM);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getTotalMemory", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the current system free memory in kB.
     *
     * @return The current system free memory in kB.
     *
     * @throws IOException If an I/O error occurs.
     *
     * @see #getAvailableMemory()
     * @see #getCachedMemory()
     * @see #getTotalMemory()
     */
    public long getFreeMemory() throws IOException {
        try {
            Bundle b = service.getFreeMemory();
            if (b.containsKey(ID_EX))
                throw new IOException(b.getString(ID_EX));
            return b.getLong(ID_MEM);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getFreeMemory", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the current system cached memory in kB.
     *
     * @return The current system cached memory in kB.
     *
     * @throws IOException If an I/O error occurs.
     *
     * @see #getAvailableMemory()
     * @see #getFreeMemory()
     * @see #getTotalMemory()
     */
    public long getCachedMemory() throws IOException {
        try {
            Bundle b = service.getCachedMemory();
            if (b.containsKey(ID_EX))
                throw new IOException(b.getString(ID_EX));
            return b.getLong(ID_MEM);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getCachedMemory", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the current system available memory in kB.
     *
     * @return The current system available memory in kB.
     *
     * @throws IOException If an I/O error occurs.
     *
     * @see #getCachedMemory()
     * @see #getFreeMemory()
     * @see #getTotalMemory()
     */
    public long getAvailableMemory() throws IOException {
        try {
            Bundle b = service.getAvailableMemory();
            if (b.containsKey(ID_EX))
                throw new IOException(b.getString(ID_EX));
            return b.getLong(ID_MEM);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getAvailableMemory", e);
            throw new RuntimeException(e);
        }
    }
}
