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

package android.firmwareupdate;

import java.io.IOException;

import android.content.Context;
import android.firmwareupdate.IFirmwareUpdateManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

/**
 * This class provides access to the firmware update service.
 *
 * <p>This manager allows applications to access the firmware update service
 * in order to perform firmware update related operations.</p>
 *
 * <p>Unless noted, all firmware update API methods require the
 * {@code com.digi.android.permission.FIRMWARE_UPDATE} permission. If your
 * application does not have this permission it will not  have access to any
 * firmware update service feature.</p>
 *
 * @hide
 */
public class FirmwareUpdateHandler {

    // Constants.
    /** @hide */
    public static final String CACHE_PREFIX = "/cache/";
    /** @hide */
    public static final String UPDATE_FILE = "/cache/fw_update/update.zip";
    /** @hide */
    public static final String ID_EXCEPTION = "exception";
    /** @hide */
    public static final String TAG = "FirmwareUpdateHandler";
    /** @hide */
    public static final String ERROR_LISTENER_NULL = "Listener cannot be null";
    /** @hide */
    public final static String ERROR_PACKAGE_NULL = "Update package path cannot be null.";
    /** @hide */
    public final static String ERROR_APPLICATION_NULL = "Application path cannot be null.";

    // Variables.
    private IFirmwareUpdateManager service;

    /**
     * @hide
     */
    public FirmwareUpdateHandler(Context context, IFirmwareUpdateManager service) {
        this.service = service;
    }

    /**
     * Installs the given update package.
     *
     * <p>This method returns immediately. All the update process is executed
     * on a new thread using the given listener as the communication interface.
     * </p>
     *
     * <p>If the update package is not located in the '/data' partition, the
     * file is copied there before performing the update.</p>
     *
     * <p>If the verification flag is set, the update package is verified before
     * performing the update using the given certificates file or the system
     * default ones.</p>
     *
     * </p>A reboot is performed in order to install the update package.</p>
     *
     * @param packagePath Full path of the update package to install.
     * @param listener An object implementing {@code FirmwareUpdateListenerImpl}
     *                 to receive firmware update process updates.
     * @param verify {@code true} to verify the update package signature,
     *               {@code false} otherwise. Package signature will be verified
     *               using the provided certificates zip file if any. If not
     *               default system certificates will be used.
     * @param deviceCertsZipPath Full path to the zip file of certificates whose
     *                           public keys will be accepted. Verification
     *                           succeeds if the package is signed by the
     *                           private key corresponding to any public key in
     *                           this file. May be null to use the system
     *                           default file, currently
     *                           "/system/etc/security/otacerts.zip".
     *
     * @throws NullPointerException if {@code packagePath == null} or
     *                              if (@code listener == null}.
     *
     * @hide
     */
    public void installUpdatePackage(String packagePath, FirmwareUpdateListenerImpl listener,
            boolean verify, String deviceCertsZipPath) {
        // Check parameter values.
        if (packagePath == null)
            throw new NullPointerException(ERROR_PACKAGE_NULL);
        if (listener == null)
            throw new NullPointerException(ERROR_LISTENER_NULL);

        // Wrap the listener class.
        FirmwareUpdateListenerTransport transport = wrapListener(listener);

        // Execute service call.
        try {
            service.installUpdatePackage(packagePath, transport, verify, deviceCertsZipPath);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in installUpdatePackage", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Wipes the cache (/cache) partition.
     *
     * </p>A reboot is performed in order to wipe the partition.</p>
     *
     * @param reason Reason for the wipe if any, may be null.
     *
     * @throws IOException if there is any error preparing for the wipe.
     *
     * @hide
     */
    public void wipeCache(String reason) throws IOException {
        // Execute service call.
        try {
            Bundle b = service.wipeCache(reason);
            if (b.containsKey(ID_EXCEPTION))
                throw new IOException(b.getString(ID_EXCEPTION));
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in wipeCache", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Wipes the user data (/data) and cache (/cache) partitions.
     *
     * </p>A reboot is performed in order to wipe the partitions.</p>
     *
     * @param reason Reason for the wipe if any, may be null.
     * @param shutdown {@code true} to shutdown the device instead of rebooting
     *                 after performing the wipe, {@code false} otherwise.
     *
     * @throws IOException if there is any error preparing for the wipe.
     *
     * @hide
     */
    public void wipeUserData(String reason, boolean shutdown) throws IOException {
        // Execute service call.
        try {
            Bundle b = service.wipeUserData(reason, shutdown);
            if (b.containsKey(ID_EXCEPTION))
                throw new IOException(b.getString(ID_EXCEPTION));
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in wipeUserData", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Installs the given application.
     *
     * @param applicationPath Full path of the application to install.
     * @param reboot {@code true} to reboot the device after installing the
     *               application, {@code false} otherwise.
     *
     * @throws NullPointerException if {@code applicationPath == null}
     * @throws IOException if there is any error installing the application.
     *
     * @hide
     */
    public void installApplication(String applicationPath, boolean reboot) throws IOException {
        // Check parameter values.
        if (applicationPath == null)
            throw new NullPointerException(ERROR_APPLICATION_NULL);

        // Execute service call.
        try {
            Bundle b = service.installApplication(applicationPath, reboot);
            if (b.containsKey(ID_EXCEPTION))
                throw new IOException(b.getString(ID_EXCEPTION));
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in installApplication", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Wraps the given firmware update listener in a firmware update listener
     * transport Object.
     *
     * @param listener Firmware update listener.
     *
     * @return The transport object containing the firmware update listener
     *         object.
     */
    private FirmwareUpdateListenerTransport wrapListener(FirmwareUpdateListenerImpl listener) {
        // Create listener wrapper.
        FirmwareUpdateListenerTransport transport = new FirmwareUpdateListenerTransport(listener, service);
        return transport;
    }
}
