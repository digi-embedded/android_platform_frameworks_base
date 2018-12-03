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

import java.util.List;

import android.cloudconnector.BinaryDataPointImpl;
import android.cloudconnector.DataPointImpl;
import android.cloudconnector.ICloudConnectorEventListener;
import android.cloudconnector.IDeviceRequestListener;

import android.os.Bundle;

/** @hide */
interface ICloudConnectorManager
{
    /* Connects the Cloud Connector service. */
    void connect();

    /* Disconnects the Cloud Connector service. */
    void disconnect();

    /* Returns the Cloud Connection status. */
    boolean isConnected();

    /* Registers for Cloud Connector service events. */
    void registerEventListener(in ICloudConnectorEventListener listener);

    /* Unregisters from Cloud Connector service events. */
    void unregisterEventListener(in ICloudConnectorEventListener listener);

    /* For reporting event callback completion */
    void eventCallbackFinished(ICloudConnectorEventListener listener);

    /* Registers for Cloud Connector device requests. */
    void registerDeviceRequestListener(in String target, in IDeviceRequestListener listener);

    /* Unregisters from Cloud Connector device requests. */
    void unregisterDeviceRequestListener(in IDeviceRequestListener listener);

    /* For reporting device request callback completion */
    void deviceRequestCallbackFinished(IDeviceRequestListener listener);

    /* Returns the Device ID */
    String getDeviceID();

    /* Reads the given Cloud Connector preference */
    String readPreference(in String preference);

    /* Writes the given Cloud Connector preference */
    void writePreference(in String preference, in String value);

    /* Sends the given list of data points */
    void sendDataPoints(in List<DataPointImpl> dataPoints);

    /* Sends the given binary data point */
    void sendBinaryDataPoint(in BinaryDataPointImpl dataPoint);

    /* Enables/disables the system monitor */
    void enableSystemMonitor(in boolean enable);

    /* Returns the current state of the system monitor */
    boolean isSystemMonitorEnabled();

    /* Sets the system monitor sample rate */
    void setSystemMonitorSampleRate(in int sampleRate);

    /* Gets the system monitor sample rate in seconds */
    int getSystemMonitorSampleRate();

    /* Sets the system monitor samples buffer size */
    void setSystemMonitorSamplesBufferSize(in int size);

    /* Gets the system monitor samples buffer size */
    int getSystemMonitorSamplesBufferSize();

    /* Sets the system monitor upload samples size */
    void setSystemMonitorUploadSamplesSize(in int size);

    /* Gets the system monitor upload samples size */
    int getSystemMonitorUploadSamplesSize();

    /* Enables/disables the system monitor memory sampling */
    void enableSystemMonitorMemorySampling(in boolean enable);

    /* Returns the state of the system monitor memory sampling */
    boolean isSystemMonitorMemorySamplingEnabled();

    /* Enables/disables the system monitor CPU load sampling */
    void enableSystemMonitorCPULoadSampling(in boolean enable);

    /* Returns the state of the system monitor CPU load sampling */
    boolean isSystemMonitorCPULoadSamplingEnabled();

    /* Enables/disables the system monitor CPU temperature sampling */
    void enableSystemMonitorCPUTemperatureSampling(in boolean enable);

    /* Returns the state of the system monitor CPU temperature sampling */
    boolean isSystemMonitorCPUTemperatureSamplingEnabled();

    /* Sets the directory used to hold app updates */
    void setAppUpdateDirectory(in String dir);

    /* Returns the directory used to hold app updates */
    String getAppUpdateDirectory();
}
