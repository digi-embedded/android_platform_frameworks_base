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

package android.system.cpu;

import android.system.cpu.ICPUTemperatureListener;

import android.os.Bundle;

/** @hide */
interface ICPUManager
{
    /* Returns the number of Cores available in the device. */
    int getNumberOfCores();

    /* Returns the value read from the file. {@code null} if it could not be read. */
    Bundle readFile(String filePath);

    /* Returns whether the provided file could be written or not. */
    Bundle writeFile(String filePath, String value);

    /* Registers a CPU temperature listener to retrieve the temperature every 'interval' ms. */
    void requestTemperatureUpdates(in ICPUTemperatureListener listener, long interval, boolean useDeprecatedPermission);

    /* Unregister the given CPU temperature listener. */
    void removeUpdates(in ICPUTemperatureListener listener, boolean useDeprecatedPermission);

    /* Returns the current CPU temperature. */
    Bundle getCurrentTemperature(boolean useDeprecatedPermission);

    /* Returns the hot temperature. */
    Bundle getHotTemperature(boolean useDeprecatedPermission);

    /* Configures the hot temperature. */
    Bundle setHotTemperature(float temp, boolean useDeprecatedPermission);

    /* Returns the critical temperature. */
    Bundle getCriticalTemperature(boolean useDeprecatedPermission);

    /* Configures the critical temperature. */
    Bundle setCriticalTemperature(float temp, boolean useDeprecatedPermission);

    // For reporting callback completion.
    void temperatureCallbackFinished(ICPUTemperatureListener listener, boolean useDeprecatedPermission);
}
