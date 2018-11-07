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

package android.system.cpu;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.system.cpu.ICPUManager;
import android.system.cpu.ICPUTemperatureListener;
import android.util.Log;

/**
 * This class provides access to CPU service.
 *
 * <p>This includes access to the system CPU temperature to obtain periodic
 * updates of the device's CPU temperature.</p>
 *
 * <p>Unless noted, all CPU API methods require the
 * {@code com.digi.android.permission.CPU} permission.</p>
 * <p>The deprecated CPU Temperature API methods require the
 * {@code com.digi.android.permission.CPU_TEMPERATURE} permission.</p>
 *
 * <p>If your application does not have the right permission it will not have
 * access to the required CPU service functionality.</p>
 *
 * @hide
 */
public class CPUHandler {

    // Constants.
    public static final String ID_EXCEPTION_IO = "io_ex";
    public static final String ID_EXCEPTION_FILE = "file_ex";
    public static final String ID_READ_VALUE = "value";
    public static final String ID_TEMPERATURE = "temp";

    public static final String ERROR_LISTENER_NULL = "Listener cannot be null";
    public static final String ERROR_INVALID_HOT_TEMPERATURE = "Invalid hot temperature value, "
            + "cannot be equal or greater than the critical temperature";
    public static final String ERROR_INVALID_CRITICAL_TEMPERATURE = "Invalid critical temperature "
            + "value, cannot be equal or less than the hot temperature";

    private static final String TAG = "CPUHandler";

    // Variables.
    private final ICPUManager service;

    // Map from CPUTemperatureListener to their associated ListenerTransport objects
    private HashMap<CPUTemperatureListenerImpl, ListenerTransport> tempListeners = new HashMap<CPUTemperatureListenerImpl, ListenerTransport>();

    /**
     * Class constructor for the CPUHandler.
     */
    public CPUHandler(Context context, ICPUManager service) {
        this.service = service;
    }

    /**
     * Returns the number of cores available in the device.
     *
     * @return The number of cores available in the device.
     */
    public int getNumberOfCores() {
        try {
            return service.getNumberOfCores();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getNumberOfCores", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the value read from the file read. {@code null} if it could not
     * be read.
     *
     * @param filePath The path of the file to read the value from.
     *
     * @return The value read from the file. {@code null} if the file could not
     *         be read.
     *
     * @throws FileNotFoundException if the file to read does not exist.
     * @throws IOException if there is a problem reading the file.
     */
    public String readFile(String filePath) throws FileNotFoundException, IOException {
        try {
            Bundle b = service.readFile(filePath);
            if (b.containsKey(ID_EXCEPTION_FILE))
                throw new FileNotFoundException(b.getString(ID_EXCEPTION_FILE));
            if (b.containsKey(ID_EXCEPTION_IO))
                throw new IOException(b.getString(ID_EXCEPTION_IO));
            if (b.containsKey(ID_READ_VALUE))
                return b.getString(ID_READ_VALUE);
            return null;
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in readFile", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes the given value in the provided file.
     *
     * @param filePath The path of the file where the value will be written.
     * @param value The value to be written in the file.
     *
     * @throws FileNotFoundException if the file to write does not exist.
     * @throws IOException if there is a problem writing the file.
     */
    public void writeFile(String filePath, String value) throws FileNotFoundException, IOException {
        try {
            Bundle b = service.writeFile(filePath, value);
            if (b.containsKey(ID_EXCEPTION_FILE))
                throw new FileNotFoundException(b.getString(ID_EXCEPTION_FILE));
            if (b.containsKey(ID_EXCEPTION_IO))
                throw new IOException(b.getString(ID_EXCEPTION_IO));
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in writeFile", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves the current CPU temperature.
     *
     * @param useDeprecatedPermission {@code true} if need to check the
     *                                deprecated permission
     *                                ({@code android.Manifest.permission.CPU_TEMPERATURE}),
     *                                {@code false} to check the new one
     *                                ({@code android.Manifest.permission.CPU}).
     *
     * @return The current CPU temperature.
     *
     * @throws IOException If there is an error reading the current temperature.
     */
    public float getCurrentTemperature(boolean useDeprecatedPermission) throws IOException {
        try {
            Bundle b = service.getCurrentTemperature(useDeprecatedPermission);
            if (b.containsKey(ID_EXCEPTION_FILE))
                throw new FileNotFoundException(b.getString(ID_EXCEPTION_FILE));
            if (b.containsKey(ID_EXCEPTION_IO))
                throw new IOException(b.getString(ID_EXCEPTION_IO));
            return b.getFloat(ID_TEMPERATURE);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getCurrentTemperature", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves the hot CPU temperature value.
     *
     * <p>Hot CPU temperature is the temperature limit at which system will
     * reduce CPU and GPU frequency to avoid system overheating.</p>
     *
     * @param useDeprecatedPermission {@code true} if need to check the
     *                                deprecated permission
     *                                ({@code android.Manifest.permission.CPU_TEMPERATURE}),
     *                                {@code false} to check the new one
     *                                ({@code android.Manifest.permission.CPU}).
     *
     * @return The hot CPU temperature value.
     *
     * @throws IOException If there is an error reading the hot temperature.
     *
     * @see #setHotTemperature(float, boolean)
     */
    public float getHotTemperature(boolean useDeprecatedPermission) throws IOException {
        try {
            Bundle b = service.getHotTemperature(useDeprecatedPermission);
            if (b.containsKey(ID_EXCEPTION_FILE))
                throw new FileNotFoundException(b.getString(ID_EXCEPTION_FILE));
            if (b.containsKey(ID_EXCEPTION_IO))
                throw new IOException(b.getString(ID_EXCEPTION_IO));
            return b.getFloat(ID_TEMPERATURE);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getHotTemperature", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the hot CPU temperature value.
     *
     * <p>Hot CPU temperature is the temperature limit at which system will
     * reduce CPU and GPU frequency to avoid system overheating.</p>
     *
     * <p>Note that in some scenarios there might be problems setting the
     * requested temperature. Use the returned value to verify the temperature
     * that was finally set.</p>
     *
     * @param temp New hot CPU temperature value.
     * @param useDeprecatedPermission {@code true} if need to check the
     *                                deprecated permission
     *                                ({@code android.Manifest.permission.CPU_TEMPERATURE}),
     *                                {@code false} to check the new one
     *                                ({@code android.Manifest.permission.CPU}).
     *
     * @return The temperature value that was finally set.
     *
     * @throws IllegalArgumentException If the given temperature is equal or
     *                                  greater than the critical temperature.
     * @throws IOException If there is an error setting the hot temperature.
     *
     * @see #getHotTemperature(boolean)
     * @see #getCriticalTemperature(boolean)
     */
    public float setHotTemperature(float temp, boolean useDeprecatedPermission) throws IOException {
        // Sanity check.
        if (temp >= getCriticalTemperature(useDeprecatedPermission)) {
            Log.e(TAG, ERROR_INVALID_HOT_TEMPERATURE);
            throw new IllegalArgumentException(ERROR_INVALID_HOT_TEMPERATURE);
        }
        try {
            Bundle b = service.setHotTemperature(temp, useDeprecatedPermission);
            if (b.containsKey(ID_EXCEPTION_FILE))
                throw new FileNotFoundException(b.getString(ID_EXCEPTION_FILE));
            if (b.containsKey(ID_EXCEPTION_IO))
                throw new IOException(b.getString(ID_EXCEPTION_IO));
            return b.getFloat(ID_TEMPERATURE);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in setHotTemperature", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves the critical CPU temperature value.
     *
     * <p>Critical CPU temperature is the temperature limit at which system
     * will halt to avoid system damage caused by overheating. This always
     * occurs after reaching hot temperature.</p>
     *
     * @param useDeprecatedPermission {@code true} if need to check the
     *                                deprecated permission
     *                                ({@code android.Manifest.permission.CPU_TEMPERATURE}),
     *                                {@code false} to check the new one
     *                                ({@code android.Manifest.permission.CPU}).
     *
     * @return The critical CPU temperature value.
     *
     * @throws IOException If there is an error reading the critical
     *                     temperature.
     *
     * @see #setCriticalTemperature(float, boolean)
     */
    public float getCriticalTemperature(boolean useDeprecatedPermission) throws IOException {
        try {
            Bundle b = service.getCriticalTemperature(useDeprecatedPermission);
            if (b.containsKey(ID_EXCEPTION_FILE))
                throw new FileNotFoundException(b.getString(ID_EXCEPTION_FILE));
            if (b.containsKey(ID_EXCEPTION_IO))
                throw new IOException(b.getString(ID_EXCEPTION_IO));
            return b.getFloat(ID_TEMPERATURE);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getCriticalTemperature", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the critical CPU temperature value.
     *
     * <p>Critical CPU temperature is the temperature limit at which system
     * will halt to avoid system damage caused by overheating. This always
     * occurs after reaching hot temperature.</p>
     *
     * <p>Note that in some scenarios there might be problems setting the
     * requested temperature. Use the returned value to verify the temperature
     * that was finally set.</p>
     *
     * @param temp New critical CPU temperature value.
     * @param useDeprecatedPermission {@code true} if need to check the
     *                                deprecated permission
     *                                ({@code android.Manifest.permission.CPU_TEMPERATURE}),
     *                                {@code false} to check the new one
     *                                ({@code android.Manifest.permission.CPU}).
     *
     * @return The temperature value that was finally set.
     *
     * @throws IllegalArgumentException If the given temperature is equal or
     *                                  lesser than the hot temperature.
     * @throws IOException If there is an error setting the critical temperature.
     *
     * @see #getCriticalTemperature(boolean)
     * @see #getHotTemperature(boolean)
     */
    public float setCriticalTemperature(float temp, boolean useDeprecatedPermission) throws IOException {
        // Sanity check.
        if (temp <= getHotTemperature(useDeprecatedPermission)) {
            Log.e(TAG, ERROR_INVALID_CRITICAL_TEMPERATURE);
            throw new IllegalArgumentException(ERROR_INVALID_CRITICAL_TEMPERATURE);
        }
        try {
            Bundle b = service.setCriticalTemperature(temp, useDeprecatedPermission);
            if (b.containsKey(ID_EXCEPTION_FILE))
                throw new FileNotFoundException(b.getString(ID_EXCEPTION_FILE));
            if (b.containsKey(ID_EXCEPTION_IO))
                throw new IOException(b.getString(ID_EXCEPTION_IO));
            return b.getFloat(ID_TEMPERATURE);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in setCriticalTemperature", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Registers for temperature updates using the given time interval.
     *
     * @param listener A {@link CPUTemperatureListenerImpl} whose
     *                 {@link CPUTemperatureListenerImpl#onTemperatureUpdate(float)}
     *                 method will be called for each temperature update.
     * @param interval Time interval between temperature updates, in
     *                 milliseconds.
     * @param useDeprecatedPermission {@code true} if need to check the
     *                                deprecated permission
     *                                ({@code android.Manifest.permission.CPU_TEMPERATURE}),
     *                                {@code false} to check the new one
     *                                ({@code android.Manifest.permission.CPU}).
     *
     * @throws NullPointerException If {@code listener == null}.
     *
     * @see #getCurrentTemperature(boolean)
     * @see #unregisterListener(CPUTemperatureListenerImpl, boolean)
     * @see CPUTemperatureListenerImpl
     */
    public void registerListener(CPUTemperatureListenerImpl listener, long interval, boolean useDeprecatedPermission) {
        if (listener == null)
            throw new NullPointerException(ERROR_LISTENER_NULL);

        // Wrap the listener class.
        ListenerTransport transport = wrapListener(listener, useDeprecatedPermission);

        // Request updates to the service.
        try {
            service.requestTemperatureUpdates(transport, interval, useDeprecatedPermission);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in registerListener", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes all temperature updates for the specified listener.
     *
     * <p>Following this call, updates will no longer occur for this listener.</p>
     *
     * @param listener Listener object that no longer needs temperature updates.
     * @param useDeprecatedPermission {@code true} if need to check the
     *                                deprecated permission
     *                                ({@code android.Manifest.permission.CPU_TEMPERATURE}),
     *                                {@code false} to check the new one
     *                                ({@code android.Manifest.permission.CPU}).
     *
     * @throws NullPointerException If {@code listener == null}.
     *
     * @see #registerListener(CPUTemperatureListenerImpl, long, boolean)
     * @see CPUTemperatureListenerImpl
     */
    public void unregisterListener(CPUTemperatureListenerImpl listener, boolean useDeprecatedPermission) {
        if (listener == null)
            throw new NullPointerException(ERROR_LISTENER_NULL);

        // Remove listener transport from the list.
        ListenerTransport transport;
        synchronized (tempListeners) {
            transport = tempListeners.remove(listener);
        }
        // If listener transport was not in the list, return.
        if (transport == null)
            return;

        // Request service to remove listener.
        try {
            service.removeUpdates(transport, useDeprecatedPermission);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in unregisterListener", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Wraps the given CPU Temperature Listener in a Listener Transport Object.
     *
     * @param listener CPU Temperature Listener.
     * @param useDeprecatedPermission {@code true} if need to check the
     *                                deprecated permission
     *                                ({@code android.Manifest.permission.CPU_TEMPERATURE}),
     *                                {@code false} to check the new one
     *                                ({@code android.Manifest.permission.CPU}).
     *
     * @return The Listener Transport object containing the CPU listener object.
     */
    private ListenerTransport wrapListener(CPUTemperatureListenerImpl listener, boolean useDeprecatedPermission) {
        // Sanity checks.
        if (listener == null)
            return null;

        // Create listener wrapper and add it to the list.
        synchronized (tempListeners) {
            ListenerTransport transport = tempListeners.get(listener);
            if (transport == null)
                transport = new ListenerTransport(listener, useDeprecatedPermission);
            tempListeners.put(listener, transport);
            return transport;
        }
    }

    /**
     * Helper class used to wrap a CPU Temperature Listener object.
     */
    private class ListenerTransport extends ICPUTemperatureListener.Stub {
        // Constants.
        private static final int TYPE_TEMPERATURE_CHANGED = 1;

        // Variables.
        private CPUTemperatureListenerImpl mListener;

        private boolean useDeprecatedPermission;

        private final Handler mListenerHandler;

        /**
         * Class constructor. Instantiates a new Listener Transport object
         * using the given parameters.
         *
         * @param listener CPU Temperature Listener.
         * @param useDeprecatedPermission {@code true} if need to check the
         *                                deprecated permission
         *                                ({@code android.Manifest.permission.CPU_TEMPERATURE}),
         *                                {@code false} to check the new one
         *                                ({@code android.Manifest.permission.CPU}).
         *
         */
        ListenerTransport(CPUTemperatureListenerImpl listener, boolean useDeprecatedPermission) {
            mListener = listener;
            this.useDeprecatedPermission = useDeprecatedPermission;

            mListenerHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    _handleMessage(msg);
                }
            };
        }

        @Override
        public void onTemperatureUpdate(float temperature) {
            Message msg = Message.obtain();
            msg.what = TYPE_TEMPERATURE_CHANGED;
            msg.obj = temperature;
            mListenerHandler.sendMessage(msg);
        }

        /**
         * Handles the given message.
         *
         * @param msg Message to handle.
         */
        private void _handleMessage(Message msg) {
            switch (msg.what) {
            case TYPE_TEMPERATURE_CHANGED:
                float temperature = (Float)msg.obj;
                mListener.onTemperatureUpdate(temperature);
                break;
            }
            try {
                service.temperatureCallbackFinished(this, useDeprecatedPermission);
            } catch (RemoteException e) {
                Log.e(TAG, "temperatureCallbackFinished: RemoteException", e);
            }
        }
    }
}
