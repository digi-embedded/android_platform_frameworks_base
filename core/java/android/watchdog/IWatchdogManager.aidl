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

package android.watchdog;

import android.app.PendingIntent;

/** @hide */
interface IWatchdogManager
{
    /* Initializes the system watchdog. */
    long initSystemWatchdog(long timeout);

    /* Returns the system watchdog timeout. */
    long getSystemWatchdogTimeout();

    /* Returns whether the system watchdog is running or not. */
    boolean isSystemWatchdogRunning();

    /* Refreshes the system watchdog. */
    void refreshSystemWatchdog();

    /* Registers an application withinthe watchdog service. */
    void registerApplication(String packageName, long timeout, in PendingIntent pendingIntent);

    /* Returns the application configured watchdog timeout. */
    long getApplicationWatchdogTimeout(String packageName);

    /* Returns whether watchdog is running for the given application or not. */
    boolean isApplicationWatchdogRunning(String packageName);

    /* Unregisters the given application from the watchdog. */
    void unregisterApplication(String packageName);

    /* Refreshes the application watchdog. */
    void refreshApplicationWatchdog(String packageName);
}
