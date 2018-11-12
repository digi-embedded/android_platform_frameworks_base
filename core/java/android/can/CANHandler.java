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

package android.can;

import java.io.IOException;

import android.content.Context;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import android.util.NoSuchInterfaceExceptionImpl;

/**
 * This class provides access to the CAN devices.
 *
 * <p>This manager allows applications to access CAN interfaces to read/write.</p>
 *
 * <p>Unless noted, all CAN API methods require the {@code com.digi.android.permission.CAN}
 * permission. If your application does not have this permission it will not
 * have access to any CAN service feature.</p>
 *
 * @hide
 */
public class CANHandler {

    // Constants.
    /** @hide */
    public static final String ID_INTERFACE_EXCEPTION = "interface_exception";
    /** @hide */
    public static final String ID_EXECUTION_EXCEPTION = "execution_exception";
    /** @hide */
    public static final String ID_FD = "fd";
    /** @hide */
    public static final String ID_FRAME_DATA = "frame_data";

    private static final String TAG = "CANHandler";

    // Variables.
    private ICANManager service;

    /**
     * @hide
     */
    public CANHandler(Context context, ICANManager service) {
        this.service = service;
    }

    /**
     * Opens the given CAN interface number.
     *
     * @param interfaceNumber Number of the CAN interface to open.
     *
     * @return The file descriptor associated to the CAN interface.
     *
     * @throws IOException If there is an error opening the interface.
     * @throws NoSuchInterfaceExceptionImpl If the given interface does not exist.
     *
     * @hide
     */
    public int openInterface(int interfaceNumber) throws NoSuchInterfaceExceptionImpl, IOException {
        try {
            Bundle b = service.openInterface(interfaceNumber);
            if (b.containsKey(ID_INTERFACE_EXCEPTION))
                throw new NoSuchInterfaceExceptionImpl(b.getString(ID_INTERFACE_EXCEPTION));
            try {
                return b.getInt(ID_FD);
            } catch (Exception e) {
                throw new IOException("Could not open CAN interface " + interfaceNumber);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in openInterface", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Closes the given CAN file descriptor.
     *
     * @param fd File descriptor to close.
     *
     * @throws IOException If the given file descriptor does not exist or if
     *                     there is an error closing the file descriptor.
     *
     * @hide
     */
    public void closeInterface(int fd) throws IOException {
        try {
            Bundle b = service.closeInterface(fd);
            if (b.containsKey(ID_INTERFACE_EXCEPTION))
                throw new IOException(b.getString(ID_INTERFACE_EXCEPTION));
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in closeInterface", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Stops reading from the given file descriptor.
     *
     * @param fd File descriptor to stop reading from.
     *
     * @hide
     */
    public boolean stopReading(int fd) {
        try {
            return service.stopReading(fd);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in stopReading", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Performs a frame read from the CAN interface.
     *
     * @param fd File descriptor to read data from.
     * @param filterIDs Array of frame IDs to filter.
     * @param extIDs Array indicating whether frame IDs are extended or not.
     * @param masks Array with filter masks.
     * @param frameInfo Byte array to store frame information.
     *
     * @return The read bytes, {@code null} if error.
     *
     * @throws IOException If there is an error reading from the CAN interface.
     *
     * @hide
     */
    public byte[] readData(int fd, int[] filterIDs, boolean[] extIDs, int[] masks, int[] frameInfo) throws IOException {
        try {
            Bundle b = service.readData(fd, filterIDs, extIDs, masks, frameInfo);
            if (b.containsKey(ID_EXECUTION_EXCEPTION))
                throw new IOException(b.getString(ID_EXECUTION_EXCEPTION));

            byte[] frameData = b.getByteArray(ID_FRAME_DATA);
            if (frameData != null)
                return frameData;
            throw new IOException("Could not read data");
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in readData", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes the given frame data in the CAN interface.
     *
     * @param fd File descriptor to write frame data to.
     * @param frameID ID of the frame to write.
     * @param isExtended {@code true} if the frame ID is extended, 
     *                   {@code false} otherwise.
     * @param isRTR {@code true} if the frame is RTR, 
     *              {@code false} otherwise.
     * @param frameData Data frame bytes to write.
     *
     * @throws IOException If there is an error writing in the CAN interface.
     *
     * @hide
     */
    public void writeData(int fd, int frameID, boolean isExtended, boolean isRTR, byte[] frameData) throws IOException {
        try {
            Bundle b = service.writeData(fd, frameID, isExtended, isRTR, frameData);
            if (b.containsKey(ID_EXECUTION_EXCEPTION))
                throw new IOException(b.getString(ID_EXECUTION_EXCEPTION));
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in writeData", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Changes the bitrate of the given CAN interface.
     *
     * @param interfaceNumber Number of the CAN interface whose bitrate will be
     *                        changed.
     * @param bitrate New interface bitrate in Hz.
     *
     * @throws IOException If there is an error setting the bitrate.
     * @throws NoSuchInterfaceExceptionImpl If the given interface does not
     *                                      exist.
     *
     * @hide
     */
    public void setBitrate(int interfaceNumber, int bitrate) throws NoSuchInterfaceExceptionImpl, IOException {
        try {
            Bundle b = service.setBitrate(interfaceNumber, bitrate);
            if (b.containsKey(ID_INTERFACE_EXCEPTION))
                throw new NoSuchInterfaceExceptionImpl(b.getString(ID_INTERFACE_EXCEPTION));
            if (b.containsKey(ID_EXECUTION_EXCEPTION))
                throw new IOException(b.getString(ID_EXECUTION_EXCEPTION));
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in setBitrate", e);
            throw new RuntimeException(e);
        }
    }
}
