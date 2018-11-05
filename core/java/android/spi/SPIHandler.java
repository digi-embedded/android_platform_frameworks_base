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

package android.spi;

import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import android.util.NoSuchInterfaceExceptionImpl;

/**
 * This class provides access to the SPI devices.
 *
 * <p>This handler allows applications to list the available SPI interfaces and
 * open SPI devices to read/write.</p>
 *
 * <p>Unless noted, all SPI API methods require the {@code com.digi.android.permission.SPI} 
 * permission. If your application does not have this permission it will not 
 * have access to any SPI service feature.</p>
 *
 * @hide
 */
public class SPIHandler {

    // Constants.
    /** @hide */
    public static final String ID_EXCEPTION = "ex";
    /** @hide */
    public static final String ID_FD = "pfd";
    /** @hide */
    public static final String ID_DATA = "data";

    private static final String TAG = "SPIHandler";

    // Variables.
    private final ISPIManager service;

    /**
     * @hide
     */
    public SPIHandler(Context context, ISPIManager service) {
        this.service = service;
    }

    /**
     * Lists all the available SPI connections.
     *
     * @return Array with the available SPI connections.
     */
    public String[] listInterfaces() {
        try {
            return service.listInterfaces();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in listInterfaces", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Opens the given SPI interface number.
     *
     * @param interfaceNumber Number of the SPI interface adaptor to use.
     * @param slaveDevice SPI slave device to communicate with.
     *
     * @return The file descriptor number associated to the SPI interface.
     *
     * @throws IOException If there is an error opening the interface.
     * @throws NoSuchInterfaceExceptionImpl If the given interface does not
     *                                      exist.
     *
     * @hide
     */
    public int openSPI(int interfaceNumber, int slaveDevice)
            throws IOException, NoSuchInterfaceExceptionImpl {
        try {
            Bundle b = service.openSPI(interfaceNumber, slaveDevice);
            if (b.containsKey(ID_EXCEPTION))
                throw new NoSuchInterfaceExceptionImpl(b.getString(ID_EXCEPTION));
            try {
                return b.getInt(ID_FD);
            } catch (Exception e) {
                throw new IOException("Could not open SPI interface " + interfaceNumber);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in openSPI", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Closes the given SPI interface file descriptor.
     *
     * @param fd File descriptor to close.
     *
     * @throws IOException If the given interface does not exist
     *                     or if there is an error closing the interface.
     *
     * @hide
     */
    public void closeSPI(int fd) throws IOException {
        try {
            Bundle b = service.closeSPI(fd);
            if (b.containsKey(ID_EXCEPTION))
                throw new IOException(b.getString(ID_EXCEPTION));
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in closeSPI", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads the specified number of bytes from the SPI slave device.
     *
     * @param fd File descriptor to read data from.
     * @param numBytes Amount of bytes to read.
     *
     * @return The read bytes, {@code null} if error.
     *
     * @throws IOException If there is an error reading from the SPI slave device.
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
     * Writes the given bytes in the SPI slave device.
     *
     * @param fd File descriptor to write data to.
     * @param txData Bytes to write.
     *
     * @throws IOException If there is an error writing in the SPI slave device.
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
     * Simultaneous write (of the given bytes) and read (of the same number of
     * bytes) using the given clock frequency and word length parameters (these
     * parameters are only used for this transfer, but their default values
     * remain the same).
     *
     * @param fd File descriptor to transfer data to/from.
     * @param txData Bytes to write.
     * @param clockFrequency The clock frequency in Hz.
     * @param wordLength Number of bits per word.
     *
     * @return The read bytes, {@code null} if error.
     *
     * @throws IOException If there is an error transferring data.
     *
     * @hide
     */
    public byte[] transferData(int fd, byte[] txData, int clockFrequency, int wordLength) throws IOException {
        try {
            Bundle b = service.transferData(fd, txData, clockFrequency, wordLength);
            if (b.containsKey(ID_EXCEPTION))
                throw new IOException(b.getString(ID_EXCEPTION));

            byte[] rxData = b.getByteArray(ID_DATA);
            if (rxData != null)
                return rxData;
            throw new IOException("Could not transfer data");
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in transferData", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the SPI mode.
     *
     * <p>There are two different ways to define the SPI mode:</p>
     * <ol>
     * <li>Using the Freescale nomenclature by means of the corresponding local
     * constants MODE_x:
     * <ul>
     * <li>MODE_0 (0x00) -> Clock polarity = 0 / Clock phase = 0</li>
     * <li>MODE_1 (0x01) -> Clock polarity = 0 / Clock phase = 1</li>
     * <li>MODE_2 (0x02) -> Clock polarity = 1 / Clock phase = 0</li>
     * <li>MODE_3 (0x03) -> Clock polarity = 1 / Clock phase = 1</li>
     * </ul></li>
     *
     * <li>Defining the mode as an OR of the desired mode bits (also available
     * as local constants). This option allows to configure more settings than
     * the previous one, which is limited to the clock line:
     * <ul>
     * <li>CPHA_CONFIG        (0x01) -> Clock line phase</li>
     * <li>CPOL_CONFIG        (0x02) -> Clock line polarity</li>
     * <li>CPOL_CONFIG        (0x02) -> Clock line polarity</li>
     * <li>CS_LOW_CONFIG      (0x00) -> Chip select line active low</li>
     * <li>CS_HIGH_CONFIG     (0x04) -> Chip select line active high</li>
     * <li>MSB_FIRST_CONFIG   (0x00) -> Message bit order (MSB first)</li>
     * <li>LSB_FIRST_CONFIG   (0x08) -> Message bit order (LSB first)</li>
     * <li>THREE_WIRE_CONFIG  (0x10) -> 3 wire mode</li>
     * <li>LOOP_CONFIG        (0x20) -> Loopback mode</li>
     * <li>NO_CS_CONFIG       (0x40) -> No chip select mode</li>
     * <li>READY_CONFIG       (0x80) -> Ready mode (slave pulls low to pause)</li>
     * </ul></li>
     * </ol>
     *
     * @param fd File descriptor to set SPI mode to.
     * @param mode The new SPI transfer mode.
     *
     * @throws IOException If there is an error setting the mode.
     *
     * @see hide
     */
    public void setTransferMode(int fd, int mode) throws IOException {
        try {
            Bundle b = service.setTransferMode(fd, mode);
            if (b.containsKey(ID_EXCEPTION))
                throw new IOException(b.getString(ID_EXCEPTION));
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in setTransferMode", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the word length in bits.
     *
     * @param fd File descriptor to set word length to.
     * @param length Number of bits per word.
     *
     * @throws IOException If there is an error setting the word length.
     *
     * @hide
     */
    public void setBitsPerWord(int fd, int length) throws IOException {
        try {
            Bundle b = service.setBitsPerWord(fd, length);
            if (b.containsKey(ID_EXCEPTION))
                throw new IOException(b.getString(ID_EXCEPTION));
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in setBitsPerWord", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the maximum speed in Hz.
     *
     * @param fd File descriptor to set frequency to.
     * @param frequency The clock frequency in Hz.
     *
     * @throws IOException If there is an error setting the clock frequency.
     *
     * @hide
     */
    public void setMaxSpeed(int fd, int frequency) throws IOException {
        try {
            Bundle b = service.setMaxSpeed(fd, frequency);
            if (b.containsKey(ID_EXCEPTION))
                throw new IOException(b.getString(ID_EXCEPTION));
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in setMaxSpeed", e);
            throw new RuntimeException(e);
        }
    }
}
