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

package android.watchdog;

import android.app.PendingIntent;
import android.content.Context;
import android.os.RemoteException;
import android.watchdog.IWatchdogManager;
import android.util.Log;

/**
 * This class provides access to the Watchdog service.
 *
 * <p>This service allows applications to initialize the system watchdog and
 * take specific actions on application failure.</p>
 *
 * <p>Unless noted, all Watchdog API methods require the
 * {@code com.digi.android.permission.WATCHDOG} permission. If your application
 * does not have this permission it will not have access to any watchdog service
 * feature.</p>
 *
 * @hide
 */
public class WatchdogHandler {

    // Constants.
    /**
     * Minimum configurable timeout for the system watchdog:
     * {@value #MINIMUM_WATCHDOG_TIMEOUT} milliseconds.
     */
    /** @hide */
    public static final long MINIMUM_WATCHDOG_TIMEOUT = 500;

    private static final String TAG = "WatchdogManager";

    /**
     * Error received when the given Android context is null:
     * {@value #ERROR_CONTEXT_NULL}.
     */
    /** @hide */
    public static final String ERROR_CONTEXT_NULL = "Calling application context cannot be null";

    private static final String ERROR_INVALID_TIEMOUT = "Timeout cannot be less than " + MINIMUM_WATCHDOG_TIMEOUT + " milliseconds.";
    private static final String ERROR_SYSTEM_WATCHDOG_ALREADY_RUNNING = "System Watchdog is already running.";
    private static final String ERROR_SYSTEM_WATCHDOG_NOT_RUNNING = "System Watchdog is not running.";
    private static final String ERROR_APPLICATION_NOT_REGISTERED = "Calling application is not registered in the service";
    private static final String ERROR_APPLICATION_ALREADY_REGISTERED = "Calling application is already registered in the service";

    // Variables.
    private final IWatchdogManager service;

    /**
     * @hide
     */
    public WatchdogHandler(Context context, IWatchdogManager service) {
        this.service = service;
    }

    /**
     * Initializes the system watchdog with the given timeout (in milliseconds).
     *
     * <p>This method can be called only once per device boot. Once system
     * watchdog is started, it can't be stopped. Any Android application can
     * refresh the system watchdog. If the system watchdog is not refreshed
     * within the configured timeout after initialization, the system will
     * reboot.</p>
     *
     * <p>If this method is called again when the system watchdog is already
     * running, an {@link UnsupportedOperationException} will be thrown.</p>
     *
     * <p>Note the possibility that system driver may not be able to set the
     * desired watchdog timeout. The real timeout value that is set is returned
     * in this method after initialization. You can also use {@link #getSystemWatchdogTimeout()}
     * to read the real driver assigned watchdog timeout after a successfully
     * initialization.</p>
     *
     * @param timeout System watchdog timeout in milliseconds (must be greater
     *                than {@link #MINIMUM_WATCHDOG_TIMEOUT}).
     *
     * @return The timeout value that could be set in the system watchdog.
     *
     * @throws IllegalArgumentException If {@code timeout < }{@value #MINIMUM_WATCHDOG_TIMEOUT}.
     * @throws UnsupportedOperationException If system watchdog is already
     *                                       running.
     *
     * @see #MINIMUM_WATCHDOG_TIMEOUT
     * @see #getSystemWatchdogTimeout()
     * @see #isSystemWatchdogRunning()
     *
     * @hide
     */
    public long initSystemWatchdog(long timeout) {
        // Check parameter values.
        checkTimeout(timeout);

        if (isSystemWatchdogRunning())
            throw new UnsupportedOperationException(ERROR_SYSTEM_WATCHDOG_ALREADY_RUNNING);

        // Initialize system watchdog from the service.
        try {
            long realTimeout = service.initSystemWatchdog(timeout);
            if (realTimeout == 0)
                throw new RuntimeException("Error initializing system watchdog");
            return realTimeout;
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in initSystemWatchdog", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves the configured system watchdog timeout.
     *
     * <p>This method can only be called once system watchdog is running,
     * otherwise an {@link UnsupportedOperationException} will be thrown.</p>
     *
     * @return The configured system watchdog timeout in milliseconds.
     *
     * @throws UnsupportedOperationException If the system watchdog is not yet
     *                                       running.
     *
     * @see #initSystemWatchdog(long)
     * @see #isSystemWatchdogRunning()
     * 
     * @hide
     */
    public long getSystemWatchdogTimeout() {
        // Check if system watchdog is running.
        if (!isSystemWatchdogRunning())
            throw new UnsupportedOperationException(ERROR_SYSTEM_WATCHDOG_NOT_RUNNING);

        // Read timeout from the service.
        try {
            return service.getSystemWatchdogTimeout();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getSystemWatchdogTimeout", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves whether the system watchdog is running or not.
     *
     * @return {@code true} if the system watchdog is running,
     *         {@code false} otherwise.
     *
     * @see #initSystemWatchdog(long)
     * @see #getSystemWatchdogTimeout()
     *
     * @hide
     */
    public boolean isSystemWatchdogRunning() {
        // Request service the system watchdog status.
        try {
            return service.isSystemWatchdogRunning();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in isSystemWatchdogRunning", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Refreshes the system watchdog.
     *
     * @throws UnsupportedOperationException If the system watchdog is not yet
     *                                       running.
     *
     * @see #initSystemWatchdog(long)
     * @see #getSystemWatchdogTimeout()
     * @see #isSystemWatchdogRunning()
     *
     * @hide
     */
    public void refreshSystemWatchdog() {
        // Check if system watchdog is running.
        if (!isSystemWatchdogRunning())
            throw new UnsupportedOperationException(ERROR_SYSTEM_WATCHDOG_NOT_RUNNING);

        // Request service to refresh the system watchdog.
        try {
            service.refreshSystemWatchdog();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in refreshSystemWatchdog", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Registers an application to the watchdog service.
     *
     * <p>This method registers the calling application to the watchdog service
     * in order to take specific actions on application failure. If the
     * application fails to refresh the watchdog in the specified timeout, the
     * application will be shut down and the given pending intent will be
     * executed. This pending intent can be used to restart application when it
     * fails.</p>
     *
     * <p>Registered applications will be automatically removed from the
     * service once they are shut down due to failure. It is responsibility of
     * the user to unregister application from the watchdog service if
     * application is manually terminated. Applications must be registered again
     * to the watchdog service once they are shut down and restarted.</p>
     *
     * <p>Applications can be unregistered from the watchdog service using the
     * {@link #unregisterApplication(Context)} method providing the application
     * context.</p>
     *
     * @param context Context of the application to register to the application
     *                watchdog service.
     * @param timeout Watchdog timeout.
     * @param pendingIntent Pending intent to execute when application is shut
     *                      down due to watchdog timeout. Can be {@code null}.
     *
     * @throws NullPointerException If {@code context == null}
     * @throws IllegalArgumentException If {@code timeout} is not valid.
     * @throws UnsupportedOperationException If the application is already
     *                                       registered in the watchdog service.
     *
     * @see #getApplicationWatchdogTimeout(Context)
     * @see #isApplicationWatchdogRunning(Context)
     * @see #unregisterApplication(Context)
     * @see #refreshApplicationWatchdog(Context)
     *
     * @hide
     */
    public void registerApplication(Context context, long timeout, PendingIntent pendingIntent) {
        // Sanity checks.
        checkContext(context);
        checkTimeout(timeout);

        // Check if the application is registered.
        if (isApplicationWatchdogRunning(context))
            throw new UnsupportedOperationException(ERROR_APPLICATION_ALREADY_REGISTERED);

        // Request service to register the application.
        try {
            service.registerApplication(context.getPackageName(), timeout, pendingIntent);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in registerApplication", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the configured watchdog timeout of the given application.
     *
     * @param context Context of the application to retrieve its configured
     *                watchdog timeout.
     *
     * @return The configured watchdog timeout of the given application.
     *
     * @throws NullPointerException If {@code context == null}
     * @throws UnsupportedOperationException If the application is not
     *                                       registered in the watchdog service.
     *
     * @see #registerApplication(Context, long, PendingIntent)
     * @see #isApplicationWatchdogRunning(Context)
     * @see #unregisterApplication(Context)
     * @see #refreshApplicationWatchdog(Context)
     *
     * @hide
     */
    public long getApplicationWatchdogTimeout(Context context) {
        // Sanity checks.
        checkContext(context);

        // Check if the application is registered.
        if (!isApplicationWatchdogRunning(context))
            throw new UnsupportedOperationException(ERROR_APPLICATION_NOT_REGISTERED);

        long timeout = -1;
        // Request service the timeout of the registered application.
        try {
            timeout = service.getApplicationWatchdogTimeout(context.getPackageName());
            if (timeout == -1)
                throw new UnsupportedOperationException(ERROR_APPLICATION_NOT_REGISTERED);
            return timeout;
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getApplicationWatchdogTimeout", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns whether the given application is registered in the service or
     * not.
     *
     * @param context The application context to retrieve its registered status.
     *
     * @return {@code true} if the given application is registered in the
     *         service, {@code false} otherwise.
     *
     * @throws NullPointerException If {@code context == null}
     *
     * @see #registerApplication(Context, long, PendingIntent)
     * @see #getApplicationWatchdogTimeout(Context)
     * @see #unregisterApplication(Context)
     * @see #refreshApplicationWatchdog(Context)
     *
     * @hide
     */
    public boolean isApplicationWatchdogRunning(Context context) {
        // Sanity checks.
        checkContext(context);

        // Request application running status to the service.
        try {
            return service.isApplicationWatchdogRunning(context.getPackageName());
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in isApplicationWatchdogRunning", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Unregisters the given application from the watchdog service.
     *
     * <p>Following this call, the application is no longer required to send
     * refresh requests to the watchdog service.</p>
     *
     * @param context The application context to unregister from the watchdog
     *                service.
     *
     * @throws NullPointerException If {@code context == null}.
     * @throws UnsupportedOperationException If the application is not
     *                                       registered in the watchdog service.
     *
     * @see #registerApplication(Context, long, PendingIntent)
     * @see #getApplicationWatchdogTimeout(Context)
     * @see #isApplicationWatchdogRunning(Context)
     * @see #refreshApplicationWatchdog(Context)
     *
     * @hide
     */
    public void unregisterApplication(Context context) {
        // Sanity checks.
        checkContext(context);

        // Check if the application is registered.
        if (!isApplicationWatchdogRunning(context))
            throw new UnsupportedOperationException(ERROR_APPLICATION_NOT_REGISTERED);

        // Request service to unregister application.
        try {
            service.unregisterApplication(context.getPackageName());
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Refreshes the given application watchdog.
     *
     * @throws NullPointerException If {@code context == null}.
     * @throws UnsupportedOperationException If the application is not
     *                                       registered in the watchdog service.
     *
     * @see #registerApplication(Context, long, PendingIntent)
     * @see #getApplicationWatchdogTimeout(Context)
     * @see #isApplicationWatchdogRunning(Context)
     * @see #unregisterApplication(Context)
     *
     * @hide
     */
    public void refreshApplicationWatchdog(Context context) {
        // Sanity checks.
        checkContext(context);

        // Check if the application is registered.
        if (!isApplicationWatchdogRunning(context))
            throw new UnsupportedOperationException(ERROR_APPLICATION_NOT_REGISTERED);

        // Request service to refresh the application watchdog.
        try {
            service.refreshApplicationWatchdog(context.getPackageName());
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in refreshApplicationWatchdog", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks whether the given context is valid or not.
     *
     * @param context Context to check.
     *
     * @throws NullPointerException If {@code context == null}.
     */
    private void checkContext(Context context) {
        if (context == null)
            throw new NullPointerException(ERROR_CONTEXT_NULL);
    }

    /**
     * Checks whether the given timeout is valid or not.
     *
     * @param timeout Timeout time to check in milliseconds.
     *
     * @throws IllegalArgumentException If timeout is not valid.
     */
    private void checkTimeout(long timeout) {
        if (timeout < MINIMUM_WATCHDOG_TIMEOUT)
            throw new IllegalArgumentException(ERROR_INVALID_TIEMOUT);
    }
}
