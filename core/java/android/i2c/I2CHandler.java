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

package android.i2c;

import java.io.IOException;

import android.content.Context;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import android.util.NoSuchInterfaceExceptionImpl;

/**
 * This class provides access to the I2C devices.
 * 
 * <p>This manager allows applications to list the available I2C interfaces and
 * open I2C devices to read/write.</p>
 *
 * <p>Unless noted, all I2C API methods require the
 * {@code com.digi.android.permission.I2C} permission. If your application does
 * not have this permission it will not have access to any I2C service feature.</p>
 * 
 * @hide
 */
public class I2CHandler {

    // Constants.
    /** @hide */
    public static final String ID_EXCEPTION = "ex";
    /** @hide */
    public static final String ID_FD = "pf";
    /** @hide */
    public static final String ID_DATA = "data";

    private static final String TAG = "I2CHandler";

    private final static String ERROR_I2C_INTERFACE_NUMBER = "I2C interface must be greater than -1";

    // Variables.
    private final II2CManager service;

    /**
     * @hide
     */
    public I2CHandler(Context context, II2CManager service) {
        this.service = service;
    }

    /**
     * Lists all available I2C interface numbers in the device.
     *
     * @return List with all available I2C interface numbers.
     */
    public int[] listInterfaces() {
        try {
            return service.listInterfaces();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in listInterfaces", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Opens the given I2C interface number.
     *
     * @param interfaceNumber Number of the I2C interface adaptor to use.
     *
     * @return The file descriptor associated to the I2C interface.
     *
     * @throws IllegalArgumentException If {@code interfaceNumber < 0}.
     * @throws IOException If there is an error opening the interface.
     * @throws NoSuchInterfaceExceptionImpl If the given interface does not exist.
     *
     * @hide
     */
    public int openI2C(int interfaceNumber) 
            throws IOException, NoSuchInterfaceExceptionImpl {
        // Check parameter values.
        if (interfaceNumber < 0) {
            Log.e(TAG, ERROR_I2C_INTERFACE_NUMBER);
            throw new IllegalArgumentException(ERROR_I2C_INTERFACE_NUMBER);
        }
        try {
            Bundle b = service.openI2C(interfaceNumber);
            if (b.containsKey(ID_EXCEPTION))
                throw new NoSuchInterfaceExceptionImpl(b.getString(ID_EXCEPTION));
            try {
                return b.getInt(ID_FD);
            } catch (Exception e) {
                throw new IOException("Could not open I2C interface " + interfaceNumber);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in openI2C", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Closes the given I2C interface file descriptor.
     *
     * @param fd File descriptor to close.
     *
     * @throws IOException If the given file descriptor does not exist
     *                     or if there is an error closing the file descriptor.
     *
     * @hide
     */
    public void closeI2C(int fd) throws IOException {
        try {
            Bundle b = service.closeI2C(fd);
            if (b.containsKey(ID_EXCEPTION))
                throw new IOException(b.getString(ID_EXCEPTION));
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in closeI2C", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads the specified number of bytes from the I2C interface.
     *
     * @param fd File descriptor to read data from.
     * @param numBytes Amount of bytes to read.
     *
     * @return The read bytes, {@code null} if error.
     *
     * @throws IOException If there is an error reading from the I2C interface.
     *
     * @hide
     */
    public byte[] readData(int fd, int numBytes) throws IOException {
        try {
            Bundle b = service.readData(fd, numBytes);
            if (b.containsKey(ID_EXCEPTION))
                throw new IOException(b.getString(ID_EXCEPTION));

            byte[] rxData = b.getByteArray(ID_DATA);
            if (rxData != null)
                return rxData;
            throw new IOException("Could not read data");
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in readData", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes the given bytes in the I2C interface.
     *
     * @param fd File descriptor to write data to.
     * @param txData Bytes to write.
     *
     * @throws IOException If there is an error writing in the I2C interface.
     *
     * @hide
     */
    public void writeData(int fd, byte[] txData) throws IOException {
        try {
            Bundle b = service.writeData(fd, txData);
            if (b.containsKey(ID_EXCEPTION))
                throw new IOException(b.getString(ID_EXCEPTION));
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in writeData", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the I2C address.
     *
     * @param fd File descriptor to set the address to.
     * @param address The address to set.
     *
     * @throws IOException If there is an error setting the address.
     *
     * @hide
     */
    public void setAddress(int fd, int address) throws IOException {
        try {
            Bundle b = service.setAddress(fd, address);
            if (b.containsKey(ID_EXCEPTION))
                throw new IOException(b.getString(ID_EXCEPTION));
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in setAddress", e);
            throw new RuntimeException(e);
        }
    }
}
