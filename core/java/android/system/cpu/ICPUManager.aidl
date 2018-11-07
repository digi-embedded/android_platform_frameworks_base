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
