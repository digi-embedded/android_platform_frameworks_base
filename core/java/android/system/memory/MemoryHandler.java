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
