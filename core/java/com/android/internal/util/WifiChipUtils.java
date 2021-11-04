package com.android.internal.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.InterruptedException;
import java.lang.NumberFormatException;
import java.lang.Process;
import java.lang.Runtime;
import java.lang.StringBuilder;

public class WifiChipUtils {

    // Constants.
    private static final String TAG = "WifiChipUtils";

    private static final int DEVICE_NUM_ATH6K = 0x0301;
    private static final int DEVICE_NUM_QCA6564 = 0x050A;

    private static final String SDIO_DEVICES_FOLDER = "/sys/bus/sdio/devices";
    private static final String SDIO_DEVICE_ID_FILE = "device";

    // Native methods.
    private static native void nativeLoadQCAModule();
    private static native void nativeUnloadQCAModule();

    /**
     * Returns wether the board Wi-Fi chip is an Atheros 6K or not.
     *
     * @return {@code true} if the Wi-Fi chip of the board is an Atheros 6k,
     *         {@code false} otherwise.
     */
    public static boolean isATH6KChip() {
        return anySDIODeviceMatchesID(DEVICE_NUM_ATH6K);
    }

    /**
     * Returns wether the Wi-Fi chip of the board is a QCA6564 or not.
     *
     * @return {@code true} if the Wi-Fi chip of the board is a QCA6564,
     *         {@code false} otherwise.
     */
    public static boolean isQCA6564Chip() {
        return anySDIODeviceMatchesID(DEVICE_NUM_QCA6564);
    }

    /**
     * Loads the QCA kernel module in the system.
     */
    public static void loadQCAModule() throws IOException {
        nativeLoadQCAModule();
    }

    /**
     * Unloads the QCA kernel module from the system.
     */
    public static void unloadQCAModule() throws IOException {
        nativeUnloadQCAModule();
    }

    /**
     * Returns wether any SDIO bus device matches the given device ID.
     *
     * @param deviceID The device ID to match.
     *
     * @return {@code true} if any SDIO bus device matches the given ID,
     *         {@code false} otherwise.
     */
    private static boolean anySDIODeviceMatchesID(int deviceID) {
        File[] sdioDevices = getSDIODevices();
        if (sdioDevices == null)
            return false;

        for (File sdioDevice:sdioDevices) {
            File deviceIDFile = new File(sdioDevice, SDIO_DEVICE_ID_FILE);
            try {
                int value = Integer.decode(readFile(deviceIDFile.toString()).trim());
                Log.d(TAG, "SDIO device ID read: " + value);
                if (value == deviceID)
                    return true;
            } catch (IOException except) {
                Log.e(TAG, "Could not read '" + sdioDevice + "': " + except.toString());
            } catch (NumberFormatException except2) {
                Log.e(TAG, "Error parsing device ID: " + except2.toString());
            }
        }

        return false;
    }

    /**
     * Returns a list of available SDIO devices.
     *
     * @return List of available SDIO devices as a File array,
     *         {@code null} if none is found.
     */
    private static File[] getSDIODevices() {
        File sdioDeviceFolder = new File(SDIO_DEVICES_FOLDER);
        if (!sdioDeviceFolder.isDirectory())
            return null;

        return sdioDeviceFolder.listFiles();
    }

    /**
     * Reads and returns the contents of the given file.
     *
     * @param filePath Path of the file to read.
     *
     * @return Contents of the file as String.
     *
     * @throws IOException if there is any error reading the file.
     */
    private static String readFile(String filePath) throws IOException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filePath));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
            }
            return sb.toString();
        } finally {
            br.close();
        }
    }
}
