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

package android.cloudconnector;

import java.util.HashMap;
import java.util.List;

import android.cloudconnector.ICloudConnectorManager;
import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

/**
 * This class provides access to the system Cloud Connector service.
 *
 * <p>Unless noted, all Cloud Connector API methods require the
 * {@code com.digi.android.permission.CLOUD_CONNECTOR} permission. If your
 * application does not have this permission it will not have access to any
 * Cloud Connector service method.</p>
 *
 * @hide
 */
public class CloudConnectorHandler {

    // Constants.
    static final String TAG = "CloudConnectorHandler";

    /** The maximum size of the samples buffer */
    public static final int MAXIMUM_SAMPLES_BUFFER_SIZE = 10000;
    /** The maximum number of samples to store for each channel before uploading */
    public static final int MAXIMUM_UPLOAD_SAMPLES_SIZE = 250;
    /** The minimum sample rate in seconds */
    public static final int MINIMUM_SAMPLE_RATE = 5;

    /** Error message when the given listener is null */
    public static final String ERROR_LISTENER_NULL = "Listener cannot be null";
    /** Error message when the given preference is null */
    public static final String ERROR_PREFERENCE_NULL = "Preference cannot be null";
    /** Error message when the given preference value is null */
    public static final String ERROR_VALUE_NULL = "Preference value cannot be null";
    /** Error message when the given target is null */
    public static final String ERROR_TARGET_NULL = "Target cannot be null";
    /** Error message when the given data points are null */
    public static final String ERROR_DATAPOINTS_NULL = "Data points cannot be null";
    /** Error message when the given system monitor sample rate is invalid */
    public static final String ERROR_INVALID_SYSTEM_MONITOR_SAMPLE_RATE = "Sample rate must be equal or greater than " + MINIMUM_SAMPLE_RATE + " seconds";
    /** Error message when the given system monitor maximum samples size is invalid */
    public static final String ERROR_INVALID_SYSTEM_MONITOR_SAMPLES_BUFFER_SIZE = "Samples buffer size must be greater than 0 and lower than " + MAXIMUM_SAMPLES_BUFFER_SIZE;
    /** Error message when the given system monitor minimum upload samples size is invalid */
    public static final String ERROR_INVALID_SYSTEM_MONITOR_UPLOAD_SAMPLES_SIZE = "The number of samples to store before uploading must be greater than 0 and lower than " + MAXIMUM_UPLOAD_SAMPLES_SIZE;

    // Variables.
    private ICloudConnectorManager service;

    // Map from CloudConnectorEventListenerImpl to their associated EventListenerTransport objects
    private HashMap<CloudConnectorEventListenerImpl, EventListenerTransport> eventListeners;

    // Map from DeviceRequestListenerImpl to their associated DeviceRequestListenerTransport objects
    private HashMap<DeviceRequestListenerImpl, DeviceRequestListenerTransport> deviceRequestListeners;

    /**
     * Class constructor. Instantiates a new {@code CloudConnectorHandler}
     * object with the given parameters.
     *
     * @param context The Android context.
     * @param service The Cloud Connector service.
     */
    public CloudConnectorHandler(Context context, ICloudConnectorManager service) {
        this.service = service;
        eventListeners = new HashMap<CloudConnectorEventListenerImpl, EventListenerTransport>();
        deviceRequestListeners = new HashMap<DeviceRequestListenerImpl, DeviceRequestListenerTransport>();
    }

    /**
     * Opens the Cloud Connector connection.
     *
     * @see #disconnect()
     * @see #isConnected()
     */
    public void connect() {
        try {
            service.connect();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in connect", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Closes the Cloud Connector connection.
     *
     * @see #connect()
     * @see #isConnected()
     */
    public void disconnect() {
        try {
            service.disconnect();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in disconnect", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the Cloud Connector connection status.
     *
     * @return {@code true} if Cloud Connector is connected, {@code false}
     *         otherwise.
     *
     * @see #connect()
     * @see #disconnect()
     */
    public boolean isConnected() {
        try {
            return service.isConnected();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in isConnected", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds the given listener to the list of listeners that will be
     * notified on Cloud Connector events.
     *
     * @param listener The {@code CloudConnectorEventListenerImpl} object to
     *                 register.
     *
     * @throws NullPointerException if {@code listener == null}
     *
     * @see #unregisterEventListener(CloudConnectorEventListenerImpl)
     */
    public void registerEventListener(CloudConnectorEventListenerImpl listener) {
        // Sanity checks.
        if (listener == null)
            throw new NullPointerException(ERROR_LISTENER_NULL);

        // Wrap the listener class.
        EventListenerTransport transport = wrapEventListener(listener);

        // Register the listener in the service.
        try {
            service.registerEventListener(transport);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in registerEventListener", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes the given listener from the Cloud Connector events listeners list.
     *
     * @param listener The {@code CloudConnectorEventListenerImpl} object to
     *                 unregister.
     *
     * @throws NullPointerException if {@code listener == null}
     *
     * @see #registerEventListener(CloudConnectorEventListenerImpl)
     */
    public void unregisterEventListener(CloudConnectorEventListenerImpl listener) {
        // Sanity checks.
        if (listener == null)
            throw new NullPointerException(ERROR_LISTENER_NULL);

        // Remove listener transport from the list.
        EventListenerTransport transport;
        synchronized (eventListeners) {
            transport = eventListeners.remove(listener);
        }
        // If listener transport was not in the list, return.
        if (transport == null)
            return;

        // Request service to remove listener.
        try {
            service.unregisterEventListener(transport);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in unregisterEventListener", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Wraps the given Cloud Connector Event Listener in a Event Listener
     * Transport Object.
     *
     * @param listener Cloud Connector Event Listener.
     *
     * @return The transport object containing the Events listener object.
     *
     * @throws NullPointerException if {@code listener == null}
     */
    private EventListenerTransport wrapEventListener(CloudConnectorEventListenerImpl listener) {
        // Sanity checks.
        if (listener == null)
            throw new NullPointerException(ERROR_LISTENER_NULL);

        // Create listener wrapper and add it to the list.
        synchronized (eventListeners) {
            EventListenerTransport transport = eventListeners.get(listener);
            if (transport == null)
                transport = new EventListenerTransport(listener, service);
            eventListeners.put(listener, transport);
            return transport;
        }
    }

    /**
     * Adds the given listener to the list of listeners that will be
     * notified on device requests for the given target.
     *
     * @param target Target for which the listener will be registered.
     * @param listener The {@code DeviceRequestListenerImpl} object to register.
     *
     * @throws NullPointerException if {@code listener == null}
     *
     * @see #unregisterDeviceRequestListener(DeviceRequestListenerImpl)
     */
    public void registerDeviceRequestListener(String target, DeviceRequestListenerImpl listener) {
        // Sanity checks.
        if (target == null)
            throw new NullPointerException(ERROR_TARGET_NULL);
        if (listener == null)
            throw new NullPointerException(ERROR_LISTENER_NULL);

        // Wrap the listener class.
        DeviceRequestListenerTransport transport = wrapDeviceRequestListener(listener);

        // Register the listener in the service.
        try {
            service.registerDeviceRequestListener(target, transport);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in registerDeviceRequestListener", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes the given listener from the Cloud Connector device request
     * listeners list.
     *
     * @param listener The {@code DeviceRequestListenerImpl} object to
     *                 unregister.
     *
     * @throws NullPointerException if {@code listener == null}
     *
     * @see #registerDeviceRequestListener(String, DeviceRequestListenerImpl)
     */
    public void unregisterDeviceRequestListener(DeviceRequestListenerImpl listener) {
        // Sanity checks.
        if (listener == null)
            throw new NullPointerException(ERROR_LISTENER_NULL);

        // Remove listener transport from the list.
        DeviceRequestListenerTransport transport;
        synchronized (deviceRequestListeners) {
            transport = deviceRequestListeners.remove(listener);
        }
        // If listener transport was not in the list, return.
        if (transport == null)
            return;

        // Request service to remove listener.
        try {
            service.unregisterDeviceRequestListener(transport);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in unregisterDeviceRequestListener", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Wraps the given Device Request Listener in a Device Request Listener
     * Transport Object.
     *
     * @param listener Device Request Listener.
     *
     * @return The transport object containing the Device Request listener
     *         object.
     *
     * @throws NullPointerException if {@code listener == null}
     */
    private DeviceRequestListenerTransport wrapDeviceRequestListener(DeviceRequestListenerImpl listener) {
        // Sanity checks.
        if (listener == null)
            throw new NullPointerException(ERROR_LISTENER_NULL);

        // Create listener wrapper and add it to the list.
        synchronized (deviceRequestListeners) {
            DeviceRequestListenerTransport transport = deviceRequestListeners.get(listener);
            if (transport == null)
                transport = new DeviceRequestListenerTransport(listener, service);
            deviceRequestListeners.put(listener, transport);
            return transport;
        }
    }

    /**
     * Retrieves the device's Device ID.
     *
     * @return The device's Device ID.
     */
    public String getDeviceID() {
        try {
            return service.getDeviceID();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getDeviceID", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads the given preference from the Cloud Connector service.
     *
     * @param preference The preference to read.
     *
     * @throws NullPointerException if {@code preference == null}
     *
     * @see #writePreference(String, String)
     */
    public String readPreference(String preference) {
        // Sanity checks.
        if (preference == null)
            throw new NullPointerException(ERROR_PREFERENCE_NULL);

        try {
            return service.readPreference(preference);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in readPreference", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads the given preference from the Cloud Connector service.
     *
     * @param preference The preference to write.
     * @param value The preference value.
     *
     * @throws NullPointerException if {@code preference == null} or
     *                              if {@code value == null} or
     *
     * @see #readPreference(String)
     */
    public void writePreference(String preference, String value) {
        // Sanity checks.
        if (preference == null)
            throw new NullPointerException(ERROR_PREFERENCE_NULL);
        if (value == null)
            throw new NullPointerException(ERROR_VALUE_NULL);

        try {
            service.writePreference(preference, value);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in writePreference", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sends the given list of data points to Device Cloud.
     *
     * @param datapoints The data points list to send.
     *
     * @throws NullPointerException if {@code datapoints == null}
     */
    public void sendDataPoints(List<DataPointImpl> dataPoints) {
        // Sanity checks.
        if (dataPoints == null)
            throw new NullPointerException(ERROR_DATAPOINTS_NULL);

        try {
            service.sendDataPoints(dataPoints);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in sendDataPoints", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sends the given list of data points to Device Cloud.
     *
     * @param datapoints The data points list to send.
     *
     * @throws NullPointerException if {@code datapoints == null}
     */
    public void sendBinaryDataPoint(BinaryDataPointImpl dataPoint) {
        // Sanity checks.
        if (dataPoint == null)
            throw new NullPointerException(ERROR_DATAPOINTS_NULL);

        try {
            service.sendBinaryDataPoint(dataPoint);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in sendBinaryDataPoint", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Enables/disables the system monitor.
     *
     * @param enable {@code true} to enable the system monitor, {@code false}
     *               to disable it.
     */
    public void enableSystemMonitor(boolean enable) {
        // Request the service to enable/disable the system monitor.
        try {
            service.enableSystemMonitor(enable);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in enableSystemMonitor", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the current state of the system monitor.
     *
     * @return {@code true} if the system monitor is enabled, {@code false}
     *         otherwise.
     */
    public boolean isSystemMonitorEnabled() {
        // Request the service to get the status of the system monitor.
        try {
            return service.isSystemMonitorEnabled();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in isSystemMonitorEnabled", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the system monitor sample rate.
     *
     * @param sampleRate The new system monitor sample rate (in seconds).
     *
     * @throws IllegalArgumentException if {@code sampleRate < }
     *                                  {@value #MINIMUM_SAMPLE_RATE}
     *
     * @see #MINIMUM_SAMPLE_RATE
     */
    public void setSystemMonitorSampleRate(int sampleRate) {
        // Sanity checks.
        if (sampleRate < MINIMUM_SAMPLE_RATE)
            throw new IllegalArgumentException(ERROR_INVALID_SYSTEM_MONITOR_SAMPLE_RATE);

        // Request the service to set the system monitor sample rate.
        try {
            service.setSystemMonitorSampleRate(sampleRate);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in setSystemMonitorSampleRate", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the system monitor sample rate.
     *
     * @return The system monitor sample rate. (in seconds)
     */
    public int getSystemMonitorSampleRate() {
        // Request the service to get the value of the system monitor sample rate.
        try {
            return service.getSystemMonitorSampleRate();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getSystemMonitorSampleRate", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the system monitor samples buffer size.
     *
     * @param size The system monitor samples buffer size.
     *
     * @throws IllegalArgumentException if {@code size < 1} or
     *                                  if {@code size > }
     *                                  {@value #MAXIMUM_SAMPLES_BUFFER_SIZE}
     *
     * @see #MAXIMUM_SAMPLES_BUFFER_SIZE
     */
    public void setSystemMonitorSamplesBufferSize(int size) {
        // Sanity checks.
        if (size < 1 || size > MAXIMUM_SAMPLES_BUFFER_SIZE)
            throw new IllegalArgumentException(ERROR_INVALID_SYSTEM_MONITOR_SAMPLES_BUFFER_SIZE);

        // Request the service to set the value of the system monitor maximum sample size.
        try {
            service.setSystemMonitorSamplesBufferSize(size);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in setSystemMonitorSamplesBufferSize", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the system monitor samples buffer size.
     *
     * @return The system monitor samples buffer size.
     */
    public int getSystemMonitorSamplesBufferSize() {
        // Request the service to get the value of the system monitor maximum sample size.
        try {
            return service.getSystemMonitorSamplesBufferSize();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getSystemMonitorSamplesBufferSize", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the system monitor number of samples to store for each channel
     * before uploading to Device Cloud.
     *
     * @param size The system monitor number of samples to store in the buffer
     *             for each channel before uploading to Device Cloud.
     *
     * @throws IllegalArgumentException if {@code size < 1} or
     *                                  if {@code size > }
     *                                  {@value #MAXIMUM_UPLOAD_SAMPLES_SIZE}
     *
     * @see #MAXIMUM_UPLOAD_SAMPLES_SIZE
     */
    public void setSystemMonitorUploadSamplesSize(int size) {
        try {
            // Sanity checks.
            if (size < 1 || size > MAXIMUM_UPLOAD_SAMPLES_SIZE)
                throw new IllegalArgumentException(ERROR_INVALID_SYSTEM_MONITOR_UPLOAD_SAMPLES_SIZE);

            // Request the service to set the value of the system monitor minimum upload samples size.
            service.setSystemMonitorUploadSamplesSize(size);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in setSystemMonitorUploadSamplesSize", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the system monitor number of samples to store for each channel
     * before uploading to Device Cloud.
     *
     * @return The system monitor number of samples to store in the buffer
     *         for each channel before uploading to Device Cloud.
     */
    public int getSystemMonitorUploadSamplesSize() {
        // Request the service to get the value of the system monitor minimum upload samples size.
        try {
            return service.getSystemMonitorUploadSamplesSize();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getSystemMonitorUploadSamplesSize", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Enables/disables the system monitor memory sampling.
     *
     * @param enable {@code true} to enable the system monitor memory sampling,
     *               {@code false} to disable it.
     */
    public void enableSystemMonitorMemorySampling(boolean enable) {
        // Request the service to enable/disable the system monitor memory sampling.
        try {
            service.enableSystemMonitorMemorySampling(enable);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in enableSystemMonitorMemorySampling", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns whether the system monitor memory sampling is enabled or not.
     *
     * @return {@code true} if the system monitor memory sampling is enabled,
     *         {@code false} otherwise.
     */
    public boolean isSystemMonitorMemorySamplingEnabled() {
        // Request the service the status of the system monitor memory sampling.
        try {
            return service.isSystemMonitorMemorySamplingEnabled();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in isSystemMonitorMemorySamplingEnabled", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Enables/disables the system monitor CPU load sampling.
     *
     * @param enable {@code true} to enable the system monitor CPU load sampling,
     *               {@code false} to disable it.
     */
    public void enableSystemMonitorCPULoadSampling(boolean enable) {
        // Request the service to enable/disable the system monitor CPU load sampling.
        try {
            service.enableSystemMonitorCPULoadSampling(enable);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in enableSystemMonitorCPULoadSampling", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns whether the system monitor CPU load sampling is enabled or not.
     *
     * @return {@code true} if the system monitor CPU load sampling is enabled,
     *         {@code false} otherwise.
     */
    public boolean isSystemMonitorCPULoadSamplingEnabled() {
        // Request the service the status of the system monitor CPU load sampling.
        try {
            return service.isSystemMonitorCPULoadSamplingEnabled();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in isSystemMonitorCPULoadSamplingEnabled", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Enables/disables the system monitor CPU temperature sampling.
     *
     * @param enable {@code true} to enable the system monitor CPU temperature
     *               sampling, {@code false} to disable it.
     */
    public void enableSystemMonitorCPUTemperatureSampling(boolean enable) {
        // Request the service to enable/disable the system monitor CPU temperature sampling.
        try {
            service.enableSystemMonitorCPUTemperatureSampling(enable);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in enableSystemMonitorCPUTemperatureSampling", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns whether the system monitor CPU temperature sampling is enabled
     * or not.
     *
     * @return {@code true} if the system monitor CPU temperature sampling is
     *         enabled, {@code false} otherwise.
     */
    public boolean isSystemMonitorCPUTemperatureSamplingEnabled() {
        // Request the service the status of the system monitor CPU temperature sampling.
        try {
            return service.isSystemMonitorCPUTemperatureSamplingEnabled();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in isSystemMonitorCPUTemperatureSamplingEnabled", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the directory used to hold app updates.
     *
     * @param dir The directory Cloud Connector will use to look for update packages
     */
    public void setAppUpdateDirectory(String dir) {
        // Request service to set applications directory setting.
        try {
            service.setAppUpdateDirectory(dir);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in setAppUpdateDirectory", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the directory used to hold app updates.
     *
     * @return The directory used to hold app updates.
     */
    public String getAppUpdateDirectory() {
        // Request service to get app update directory setting.
        try {
            return service.getAppUpdateDirectory();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getAppUpdateDirectory", e);
            throw new RuntimeException(e);
        }
    }
}
