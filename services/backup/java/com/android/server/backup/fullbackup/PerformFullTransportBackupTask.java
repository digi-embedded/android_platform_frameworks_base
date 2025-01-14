/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.server.backup.fullbackup;

import static com.android.server.backup.BackupManagerService.DEBUG;
import static com.android.server.backup.BackupManagerService.DEBUG_SCHEDULING;
import static com.android.server.backup.BackupManagerService.MORE_DEBUG;
import static com.android.server.backup.BackupManagerService.OP_PENDING;
import static com.android.server.backup.BackupManagerService.OP_TYPE_BACKUP;
import static com.android.server.backup.BackupManagerService.OP_TYPE_BACKUP_WAIT;

import android.annotation.Nullable;
import android.app.IBackupAgent;
import android.app.backup.BackupManager;
import android.app.backup.BackupManagerMonitor;
import android.app.backup.BackupProgress;
import android.app.backup.BackupTransport;
import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IBackupObserver;
import android.app.backup.IFullBackupRestoreObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;

import com.android.internal.backup.IBackupTransport;
import com.android.internal.util.Preconditions;
import com.android.server.EventLogTags;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.FullBackupJob;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.TransportManager;
import com.android.server.backup.internal.OnTaskFinishedListener;
import com.android.server.backup.internal.Operation;
import com.android.server.backup.transport.TransportClient;
import com.android.server.backup.transport.TransportNotAvailableException;
import com.android.server.backup.utils.AppBackupUtils;
import com.android.server.backup.utils.BackupManagerMonitorUtils;
import com.android.server.backup.utils.BackupObserverUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Full backup task extension used for transport-oriented operation.
 *
 * Flow:
 * For each requested package:
 *     - Spin off a new SinglePackageBackupRunner (mBackupRunner) for the current package.
 *     - Wait until preflight is complete. (mBackupRunner.getPreflightResultBlocking())
 *     - If preflight data size is within limit, start reading data from agent pipe and writing
 *       to transport pipe. While there is data to send, call transport.sendBackupData(int) to
 *       tell the transport how many bytes to expect on its pipe.
 *     - After sending all data, call transport.finishBackup() if things went well. And
 *       transport.cancelFullBackup() otherwise.
 *
 * Interactions with mCurrentOperations:
 *     - An entry for this object is added to mCurrentOperations for the entire lifetime of this
 *       object. Used to cancel the operation.
 *     - SinglePackageBackupRunner and SinglePackageBackupPreflight will put ephemeral entries
 *       to get timeouts or operation complete callbacks.
 *
 * Handling cancels:
 *     - The contract we provide is that the task won't interact with the transport after
 *       handleCancel() is done executing.
 *     - This task blocks at 3 points: 1. Preflight result check 2. Reading on agent side pipe
 *       and 3. Get backup result from mBackupRunner.
 *     - Bubbling up handleCancel to mBackupRunner handles all 3: 1. Calls handleCancel on the
 *       preflight operation which counts down on the preflight latch. 2. Tears down the agent,
 *       so read() returns -1. 3. Notifies mCurrentOpLock which unblocks
 *       mBackupRunner.getBackupResultBlocking().
 */
public class PerformFullTransportBackupTask extends FullBackupTask implements BackupRestoreTask {
    public static PerformFullTransportBackupTask newWithCurrentTransport(
            BackupManagerService backupManagerService,
            IFullBackupRestoreObserver observer,
            String[] whichPackages,
            boolean updateSchedule,
            FullBackupJob runningJob,
            CountDownLatch latch,
            IBackupObserver backupObserver,
            IBackupManagerMonitor monitor,
            boolean userInitiated,
            String caller) {
        TransportManager transportManager = backupManagerService.getTransportManager();
        TransportClient transportClient = transportManager.getCurrentTransportClient(caller);
        OnTaskFinishedListener listener =
                listenerCaller ->
                        transportManager.disposeOfTransportClient(transportClient, listenerCaller);
        return new PerformFullTransportBackupTask(
                backupManagerService,
                transportClient,
                observer,
                whichPackages,
                updateSchedule,
                runningJob,
                latch,
                backupObserver,
                monitor,
                listener,
                userInitiated);
    }

    private static final String TAG = "PFTBT";

    private BackupManagerService backupManagerService;
    private final Object mCancelLock = new Object();

    ArrayList<PackageInfo> mPackages;
    PackageInfo mCurrentPackage;
    boolean mUpdateSchedule;
    CountDownLatch mLatch;
    FullBackupJob mJob;             // if a scheduled job needs to be finished afterwards
    IBackupObserver mBackupObserver;
    IBackupManagerMonitor mMonitor;
    boolean mUserInitiated;
    SinglePackageBackupRunner mBackupRunner;
    private final int mBackupRunnerOpToken;
    private final OnTaskFinishedListener mListener;
    private final TransportClient mTransportClient;

    // This is true when a backup operation for some package is in progress.
    private volatile boolean mIsDoingBackup;
    private volatile boolean mCancelAll;
    private final int mCurrentOpToken;
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;

    public PerformFullTransportBackupTask(BackupManagerService backupManagerService,
            TransportClient transportClient,
            IFullBackupRestoreObserver observer,
            String[] whichPackages, boolean updateSchedule,
            FullBackupJob runningJob, CountDownLatch latch, IBackupObserver backupObserver,
            IBackupManagerMonitor monitor, @Nullable OnTaskFinishedListener listener,
            boolean userInitiated) {
        super(observer);
        this.backupManagerService = backupManagerService;
        mTransportClient = transportClient;
        mUpdateSchedule = updateSchedule;
        mLatch = latch;
        mJob = runningJob;
        mPackages = new ArrayList<>(whichPackages.length);
        mBackupObserver = backupObserver;
        mMonitor = monitor;
        mListener = (listener != null) ? listener : OnTaskFinishedListener.NOP;
        mUserInitiated = userInitiated;
        mCurrentOpToken = backupManagerService.generateRandomIntegerToken();
        mBackupRunnerOpToken = backupManagerService.generateRandomIntegerToken();
        mAgentTimeoutParameters = Preconditions.checkNotNull(
                backupManagerService.getAgentTimeoutParameters(),
                "Timeout parameters cannot be null");

        if (backupManagerService.isBackupOperationInProgress()) {
            if (DEBUG) {
                Slog.d(TAG, "Skipping full backup. A backup is already in progress.");
            }
            mCancelAll = true;
            return;
        }

        registerTask();

        for (String pkg : whichPackages) {
            try {
                PackageManager pm = backupManagerService.getPackageManager();
                PackageInfo info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES);
                mCurrentPackage = info;
                if (!AppBackupUtils.appIsEligibleForBackup(info.applicationInfo, pm)) {
                    // Cull any packages that have indicated that backups are not permitted,
                    // that run as system-domain uids but do not define their own backup agents,
                    // as well as any explicit mention of the 'special' shared-storage agent
                    // package (we handle that one at the end).
                    if (MORE_DEBUG) {
                        Slog.d(TAG, "Ignoring ineligible package " + pkg);
                    }
                    mMonitor = BackupManagerMonitorUtils.monitorEvent(mMonitor,
                            BackupManagerMonitor.LOG_EVENT_ID_PACKAGE_INELIGIBLE,
                            mCurrentPackage,
                            BackupManagerMonitor.LOG_EVENT_CATEGORY_BACKUP_MANAGER_POLICY,
                            null);
                    BackupObserverUtils.sendBackupOnPackageResult(mBackupObserver, pkg,
                            BackupManager.ERROR_BACKUP_NOT_ALLOWED);
                    continue;
                } else if (!AppBackupUtils.appGetsFullBackup(info)) {
                    // Cull any packages that are found in the queue but now aren't supposed
                    // to get full-data backup operations.
                    if (MORE_DEBUG) {
                        Slog.d(TAG, "Ignoring full-data backup of key/value participant "
                                + pkg);
                    }
                    mMonitor = BackupManagerMonitorUtils.monitorEvent(mMonitor,
                            BackupManagerMonitor.LOG_EVENT_ID_PACKAGE_KEY_VALUE_PARTICIPANT,
                            mCurrentPackage,
                            BackupManagerMonitor.LOG_EVENT_CATEGORY_BACKUP_MANAGER_POLICY,
                            null);
                    BackupObserverUtils.sendBackupOnPackageResult(mBackupObserver, pkg,
                            BackupManager.ERROR_BACKUP_NOT_ALLOWED);
                    continue;
                } else if (AppBackupUtils.appIsStopped(info.applicationInfo)) {
                    // Cull any packages in the 'stopped' state: they've either just been
                    // installed or have explicitly been force-stopped by the user.  In both
                    // cases we do not want to launch them for backup.
                    if (MORE_DEBUG) {
                        Slog.d(TAG, "Ignoring stopped package " + pkg);
                    }
                    mMonitor = BackupManagerMonitorUtils.monitorEvent(mMonitor,
                            BackupManagerMonitor.LOG_EVENT_ID_PACKAGE_STOPPED,
                            mCurrentPackage,
                            BackupManagerMonitor.LOG_EVENT_CATEGORY_BACKUP_MANAGER_POLICY,
                            null);
                    BackupObserverUtils.sendBackupOnPackageResult(mBackupObserver, pkg,
                            BackupManager.ERROR_BACKUP_NOT_ALLOWED);
                    continue;
                }
                mPackages.add(info);
            } catch (NameNotFoundException e) {
                Slog.i(TAG, "Requested package " + pkg + " not found; ignoring");
                mMonitor = BackupManagerMonitorUtils.monitorEvent(mMonitor,
                        BackupManagerMonitor.LOG_EVENT_ID_PACKAGE_NOT_FOUND,
                        mCurrentPackage,
                        BackupManagerMonitor.LOG_EVENT_CATEGORY_BACKUP_MANAGER_POLICY,
                        null);
            }
        }
    }

    private void registerTask() {
        synchronized (backupManagerService.getCurrentOpLock()) {
            Slog.d(TAG, "backupmanager pftbt token=" + Integer.toHexString(mCurrentOpToken));
            backupManagerService.getCurrentOperations().put(
                    mCurrentOpToken,
                    new Operation(OP_PENDING, this, OP_TYPE_BACKUP));
        }
    }

    public void unregisterTask() {
        backupManagerService.removeOperation(mCurrentOpToken);
    }

    @Override
    public void execute() {
        // Nothing to do.
    }

    @Override
    public void handleCancel(boolean cancelAll) {
        synchronized (mCancelLock) {
            // We only support 'cancelAll = true' case for this task. Cancelling of a single package

            // due to timeout is handled by SinglePackageBackupRunner and
            // SinglePackageBackupPreflight.

            if (!cancelAll) {
                Slog.wtf(TAG, "Expected cancelAll to be true.");
            }

            if (mCancelAll) {
                Slog.d(TAG, "Ignoring duplicate cancel call.");
                return;
            }

            mCancelAll = true;
            if (mIsDoingBackup) {
                backupManagerService.handleCancel(mBackupRunnerOpToken, cancelAll);
                try {
                    // If we're running a backup we should be connected to a transport
                    IBackupTransport transport =
                            mTransportClient.getConnectedTransport("PFTBT.handleCancel()");
                    transport.cancelFullBackup();
                } catch (RemoteException | TransportNotAvailableException e) {
                    Slog.w(TAG, "Error calling cancelFullBackup() on transport: " + e);
                    // Can't do much.
                }
            }
        }
    }

    @Override
    public void operationComplete(long result) {
        // Nothing to do.
    }

    @Override
    public void run() {

        // data from the app, passed to us for bridging to the transport
        ParcelFileDescriptor[] enginePipes = null;

        // Pipe through which we write data to the transport
        ParcelFileDescriptor[] transportPipes = null;

        long backoff = 0;
        int backupRunStatus = BackupManager.SUCCESS;

        try {
            if (!backupManagerService.isEnabled() || !backupManagerService.isProvisioned()) {
                // Backups are globally disabled, so don't proceed.
                if (DEBUG) {
                    Slog.i(TAG, "full backup requested but enabled=" + backupManagerService
                            .isEnabled()
                            + " provisioned=" + backupManagerService.isProvisioned()
                            + "; ignoring");
                }
                int monitoringEvent;
                if (backupManagerService.isProvisioned()) {
                    monitoringEvent = BackupManagerMonitor.LOG_EVENT_ID_BACKUP_DISABLED;
                } else {
                    monitoringEvent = BackupManagerMonitor.LOG_EVENT_ID_DEVICE_NOT_PROVISIONED;
                }
                mMonitor = BackupManagerMonitorUtils
                        .monitorEvent(mMonitor, monitoringEvent, null,
                                BackupManagerMonitor.LOG_EVENT_CATEGORY_BACKUP_MANAGER_POLICY,
                                null);
                mUpdateSchedule = false;
                backupRunStatus = BackupManager.ERROR_BACKUP_NOT_ALLOWED;
                return;
            }

            IBackupTransport transport = mTransportClient.connect("PFTBT.run()");
            if (transport == null) {
                Slog.w(TAG, "Transport not present; full data backup not performed");
                backupRunStatus = BackupManager.ERROR_TRANSPORT_ABORTED;
                mMonitor = BackupManagerMonitorUtils.monitorEvent(mMonitor,
                        BackupManagerMonitor.LOG_EVENT_ID_PACKAGE_TRANSPORT_NOT_PRESENT,
                        mCurrentPackage, BackupManagerMonitor.LOG_EVENT_CATEGORY_TRANSPORT,
                        null);
                return;
            }

            // Set up to send data to the transport
            final int N = mPackages.size();
            final byte[] buffer = new byte[8192];
            for (int i = 0; i < N; i++) {
                mBackupRunner = null;
                PackageInfo currentPackage = mPackages.get(i);
                String packageName = currentPackage.packageName;
                if (DEBUG) {
                    Slog.i(TAG, "Initiating full-data transport backup of " + packageName
                            + " token: " + mCurrentOpToken);
                }
                EventLog.writeEvent(EventLogTags.FULL_BACKUP_PACKAGE, packageName);

                transportPipes = ParcelFileDescriptor.createPipe();

                // Tell the transport the data's coming
                int flags = mUserInitiated ? BackupTransport.FLAG_USER_INITIATED : 0;
                int backupPackageStatus;
                long quota = Long.MAX_VALUE;
                synchronized (mCancelLock) {
                    if (mCancelAll) {
                        break;
                    }
                    backupPackageStatus = transport.performFullBackup(currentPackage,
                            transportPipes[0], flags);

                    if (backupPackageStatus == BackupTransport.TRANSPORT_OK) {
                        quota = transport.getBackupQuota(currentPackage.packageName,
                                true /* isFullBackup */);
                        // Now set up the backup engine / data source end of things
                        enginePipes = ParcelFileDescriptor.createPipe();
                        mBackupRunner =
                                new SinglePackageBackupRunner(enginePipes[1], currentPackage,
                                        mTransportClient, quota, mBackupRunnerOpToken,
                                        transport.getTransportFlags());
                        // The runner dup'd the pipe half, so we close it here
                        enginePipes[1].close();
                        enginePipes[1] = null;

                        mIsDoingBackup = true;
                    }
                }
                if (backupPackageStatus == BackupTransport.TRANSPORT_OK) {

                    // The transport has its own copy of the read end of the pipe,
                    // so close ours now
                    transportPipes[0].close();
                    transportPipes[0] = null;

                    // Spin off the runner to fetch the app's data and pipe it
                    // into the engine pipes
                    (new Thread(mBackupRunner, "package-backup-bridge")).start();

                    // Read data off the engine pipe and pass it to the transport
                    // pipe until we hit EOD on the input stream.  We do not take
                    // close() responsibility for these FDs into these stream wrappers.
                    FileInputStream in = new FileInputStream(
                            enginePipes[0].getFileDescriptor());
                    FileOutputStream out = new FileOutputStream(
                            transportPipes[1].getFileDescriptor());
                    long totalRead = 0;
                    final long preflightResult = mBackupRunner.getPreflightResultBlocking();
                    // Preflight result is negative if some error happened on preflight.
                    if (preflightResult < 0) {
                        if (MORE_DEBUG) {
                            Slog.d(TAG, "Backup error after preflight of package "
                                    + packageName + ": " + preflightResult
                                    + ", not running backup.");
                        }
                        mMonitor = BackupManagerMonitorUtils.monitorEvent(mMonitor,
                                BackupManagerMonitor.LOG_EVENT_ID_ERROR_PREFLIGHT,
                                mCurrentPackage,
                                BackupManagerMonitor.LOG_EVENT_CATEGORY_BACKUP_MANAGER_POLICY,
                                BackupManagerMonitorUtils.putMonitoringExtra(null,
                                        BackupManagerMonitor.EXTRA_LOG_PREFLIGHT_ERROR,
                                        preflightResult));
                        backupPackageStatus = (int) preflightResult;
                    } else {
                        int nRead = 0;
                        do {
                            nRead = in.read(buffer);
                            if (MORE_DEBUG) {
                                Slog.v(TAG, "in.read(buffer) from app: " + nRead);
                            }
                            if (nRead > 0) {
                                out.write(buffer, 0, nRead);
                                synchronized (mCancelLock) {
                                    if (!mCancelAll) {
                                        backupPackageStatus = transport.sendBackupData(nRead);
                                    }
                                }
                                totalRead += nRead;
                                if (mBackupObserver != null && preflightResult > 0) {
                                    BackupObserverUtils
                                            .sendBackupOnUpdate(mBackupObserver, packageName,
                                                    new BackupProgress(preflightResult, totalRead));
                                }
                            }
                        } while (nRead > 0
                                && backupPackageStatus == BackupTransport.TRANSPORT_OK);
                        // Despite preflight succeeded, package still can hit quota on flight.
                        if (backupPackageStatus == BackupTransport.TRANSPORT_QUOTA_EXCEEDED) {
                            Slog.w(TAG, "Package hit quota limit in-flight " + packageName
                                    + ": " + totalRead + " of " + quota);
                            mMonitor = BackupManagerMonitorUtils.monitorEvent(mMonitor,
                                    BackupManagerMonitor.LOG_EVENT_ID_QUOTA_HIT_PREFLIGHT,
                                    mCurrentPackage,
                                    BackupManagerMonitor.LOG_EVENT_CATEGORY_TRANSPORT,
                                    null);
                            mBackupRunner.sendQuotaExceeded(totalRead, quota);
                        }
                    }

                    final int backupRunnerResult = mBackupRunner.getBackupResultBlocking();

                    synchronized (mCancelLock) {
                        mIsDoingBackup = false;
                        // If mCancelCurrent is true, we have already called cancelFullBackup().
                        if (!mCancelAll) {
                            if (backupRunnerResult == BackupTransport.TRANSPORT_OK) {
                                // If we were otherwise in a good state, now interpret the final
                                // result based on what finishBackup() returns.  If we're in a
                                // failure case already, preserve that result and ignore whatever
                                // finishBackup() reports.
                                final int finishResult = transport.finishBackup();
                                if (backupPackageStatus == BackupTransport.TRANSPORT_OK) {
                                    backupPackageStatus = finishResult;
                                }
                            } else {
                                transport.cancelFullBackup();
                            }
                        }
                    }

                    // A transport-originated error here means that we've hit an error that the
                    // runner doesn't know about, so it's still moving data but we're pulling the
                    // rug out from under it.  Don't ask for its result:  we already know better
                    // and we'll hang if we block waiting for it, since it relies on us to
                    // read back the data it's writing into the engine.  Just proceed with
                    // a graceful failure.  The runner/engine mechanism will tear itself
                    // down cleanly when we close the pipes from this end.  Transport-level
                    // errors take precedence over agent/app-specific errors for purposes of
                    // determining our course of action.
                    if (backupPackageStatus == BackupTransport.TRANSPORT_OK) {
                        // We still could fail in backup runner thread.
                        if (backupRunnerResult != BackupTransport.TRANSPORT_OK) {
                            // If there was an error in runner thread and
                            // not TRANSPORT_ERROR here, overwrite it.
                            backupPackageStatus = backupRunnerResult;
                        }
                    } else {
                        if (MORE_DEBUG) {
                            Slog.i(TAG, "Transport-level failure; cancelling agent work");
                        }
                    }

                    if (MORE_DEBUG) {
                        Slog.i(TAG, "Done delivering backup data: result="
                                + backupPackageStatus);
                    }

                    if (backupPackageStatus != BackupTransport.TRANSPORT_OK) {
                        Slog.e(TAG, "Error " + backupPackageStatus + " backing up "
                                + packageName);
                    }

                    // Also ask the transport how long it wants us to wait before
                    // moving on to the next package, if any.
                    backoff = transport.requestFullBackupTime();
                    if (DEBUG_SCHEDULING) {
                        Slog.i(TAG, "Transport suggested backoff=" + backoff);
                    }

                }

                // Roll this package to the end of the backup queue if we're
                // in a queue-driven mode (regardless of success/failure)
                if (mUpdateSchedule) {
                    backupManagerService.enqueueFullBackup(packageName, System.currentTimeMillis());
                }

                if (backupPackageStatus == BackupTransport.TRANSPORT_PACKAGE_REJECTED) {
                    BackupObserverUtils
                            .sendBackupOnPackageResult(mBackupObserver, packageName,
                                    BackupManager.ERROR_TRANSPORT_PACKAGE_REJECTED);
                    if (DEBUG) {
                        Slog.i(TAG, "Transport rejected backup of " + packageName
                                + ", skipping");
                    }
                    EventLog.writeEvent(EventLogTags.FULL_BACKUP_AGENT_FAILURE, packageName,
                            "transport rejected");
                    // This failure state can come either a-priori from the transport, or
                    // from the preflight pass.  If we got as far as preflight, we now need
                    // to tear down the target process.
                    if (mBackupRunner != null) {
                        backupManagerService.tearDownAgentAndKill(currentPackage.applicationInfo);
                    }
                    // ... and continue looping.
                } else if (backupPackageStatus == BackupTransport.TRANSPORT_QUOTA_EXCEEDED) {
                    BackupObserverUtils
                            .sendBackupOnPackageResult(mBackupObserver, packageName,
                                    BackupManager.ERROR_TRANSPORT_QUOTA_EXCEEDED);
                    // add 20ms delay to make sure thread mBackupRunner execute sendQuotaExceeded in time before application is killed
                    SystemClock.sleep(20);

                    if (DEBUG) {
                        Slog.i(TAG, "Transport quota exceeded for package: " + packageName);
                        EventLog.writeEvent(EventLogTags.FULL_BACKUP_QUOTA_EXCEEDED,
                                packageName);
                    }
                    backupManagerService.tearDownAgentAndKill(currentPackage.applicationInfo);
                    // Do nothing, clean up, and continue looping.
                } else if (backupPackageStatus == BackupTransport.AGENT_ERROR) {
                    BackupObserverUtils
                            .sendBackupOnPackageResult(mBackupObserver, packageName,
                                    BackupManager.ERROR_AGENT_FAILURE);
                    Slog.w(TAG, "Application failure for package: " + packageName);
                    EventLog.writeEvent(EventLogTags.BACKUP_AGENT_FAILURE, packageName);
                    backupManagerService.tearDownAgentAndKill(currentPackage.applicationInfo);
                    // Do nothing, clean up, and continue looping.
                } else if (backupPackageStatus == BackupManager.ERROR_BACKUP_CANCELLED) {
                    BackupObserverUtils
                            .sendBackupOnPackageResult(mBackupObserver, packageName,
                                    BackupManager.ERROR_BACKUP_CANCELLED);
                    Slog.w(TAG, "Backup cancelled. package=" + packageName +
                            ", cancelAll=" + mCancelAll);
                    EventLog.writeEvent(EventLogTags.FULL_BACKUP_CANCELLED, packageName);
                    backupManagerService.tearDownAgentAndKill(currentPackage.applicationInfo);
                    // Do nothing, clean up, and continue looping.
                } else if (backupPackageStatus != BackupTransport.TRANSPORT_OK) {
                    BackupObserverUtils
                            .sendBackupOnPackageResult(mBackupObserver, packageName,
                                    BackupManager.ERROR_TRANSPORT_ABORTED);
                    Slog.w(TAG, "Transport failed; aborting backup: " + backupPackageStatus);
                    EventLog.writeEvent(EventLogTags.FULL_BACKUP_TRANSPORT_FAILURE);
                    // Abort entire backup pass.
                    backupRunStatus = BackupManager.ERROR_TRANSPORT_ABORTED;
                    backupManagerService.tearDownAgentAndKill(currentPackage.applicationInfo);
                    return;
                } else {
                    // Success!
                    BackupObserverUtils
                            .sendBackupOnPackageResult(mBackupObserver, packageName,
                                    BackupManager.SUCCESS);
                    EventLog.writeEvent(EventLogTags.FULL_BACKUP_SUCCESS, packageName);
                    backupManagerService.logBackupComplete(packageName);
                }
                cleanUpPipes(transportPipes);
                cleanUpPipes(enginePipes);
                if (currentPackage.applicationInfo != null) {
                    Slog.i(TAG, "Unbinding agent in " + packageName);
                    backupManagerService.addBackupTrace("unbinding " + packageName);
                    try {
                        backupManagerService.getActivityManager().unbindBackupAgent(
                                currentPackage.applicationInfo);
                    } catch (RemoteException e) { /* can't happen; activity manager is local */ }
                }
            }
        } catch (Exception e) {
            backupRunStatus = BackupManager.ERROR_TRANSPORT_ABORTED;
            Slog.w(TAG, "Exception trying full transport backup", e);
            mMonitor = BackupManagerMonitorUtils.monitorEvent(mMonitor,
                    BackupManagerMonitor.LOG_EVENT_ID_EXCEPTION_FULL_BACKUP,
                    mCurrentPackage,
                    BackupManagerMonitor.LOG_EVENT_CATEGORY_BACKUP_MANAGER_POLICY,
                    BackupManagerMonitorUtils.putMonitoringExtra(null,
                            BackupManagerMonitor.EXTRA_LOG_EXCEPTION_FULL_BACKUP,
                            Log.getStackTraceString(e)));

        } finally {

            if (mCancelAll) {
                backupRunStatus = BackupManager.ERROR_BACKUP_CANCELLED;
            }

            if (DEBUG) {
                Slog.i(TAG, "Full backup completed with status: " + backupRunStatus);
            }
            BackupObserverUtils.sendBackupFinished(mBackupObserver, backupRunStatus);

            cleanUpPipes(transportPipes);
            cleanUpPipes(enginePipes);

            unregisterTask();

            if (mJob != null) {
                mJob.finishBackupPass();
            }

            synchronized (backupManagerService.getQueueLock()) {
                backupManagerService.setRunningFullBackupTask(null);
            }

            mListener.onFinished("PFTBT.run()");

            mLatch.countDown();

            // Now that we're actually done with schedule-driven work, reschedule
            // the next pass based on the new queue state.
            if (mUpdateSchedule) {
                backupManagerService.scheduleNextFullBackupJob(backoff);
            }

            Slog.i(TAG, "Full data backup pass finished.");
            backupManagerService.getWakelock().release();
        }
    }

    void cleanUpPipes(ParcelFileDescriptor[] pipes) {
        if (pipes != null) {
            if (pipes[0] != null) {
                ParcelFileDescriptor fd = pipes[0];
                pipes[0] = null;
                try {
                    fd.close();
                } catch (IOException e) {
                    Slog.w(TAG, "Unable to close pipe!");
                }
            }
            if (pipes[1] != null) {
                ParcelFileDescriptor fd = pipes[1];
                pipes[1] = null;
                try {
                    fd.close();
                } catch (IOException e) {
                    Slog.w(TAG, "Unable to close pipe!");
                }
            }
        }
    }

    // Run the backup and pipe it back to the given socket -- expects to run on
    // a standalone thread.  The  runner owns this half of the pipe, and closes
    // it to indicate EOD to the other end.
    class SinglePackageBackupPreflight implements BackupRestoreTask, FullBackupPreflight {
        final AtomicLong mResult = new AtomicLong(BackupTransport.AGENT_ERROR);
        final CountDownLatch mLatch = new CountDownLatch(1);
        final TransportClient mTransportClient;
        final long mQuota;
        private final int mCurrentOpToken;
        private final int mTransportFlags;

        SinglePackageBackupPreflight(
                TransportClient transportClient,
                long quota,
                int currentOpToken,
                int transportFlags) {
            mTransportClient = transportClient;
            mQuota = quota;
            mCurrentOpToken = currentOpToken;
            mTransportFlags = transportFlags;
        }

        @Override
        public int preflightFullBackup(PackageInfo pkg, IBackupAgent agent) {
            int result;
            long fullBackupAgentTimeoutMillis =
                    mAgentTimeoutParameters.getFullBackupAgentTimeoutMillis();
            try {
                backupManagerService.prepareOperationTimeout(
                        mCurrentOpToken, fullBackupAgentTimeoutMillis, this, OP_TYPE_BACKUP_WAIT);
                backupManagerService.addBackupTrace("preflighting");
                if (MORE_DEBUG) {
                    Slog.d(TAG, "Preflighting full payload of " + pkg.packageName);
                }
                agent.doMeasureFullBackup(mQuota, mCurrentOpToken,
                        backupManagerService.getBackupManagerBinder(), mTransportFlags);

                // Now wait to get our result back.  If this backstop timeout is reached without
                // the latch being thrown, flow will continue as though a result or "normal"
                // timeout had been produced.  In case of a real backstop timeout, mResult
                // will still contain the value it was constructed with, AGENT_ERROR, which
                // intentionaly falls into the "just report failure" code.
                mLatch.await(fullBackupAgentTimeoutMillis, TimeUnit.MILLISECONDS);

                long totalSize = mResult.get();
                // If preflight timed out, mResult will contain error code as int.
                if (totalSize < 0) {
                    return (int) totalSize;
                }
                if (MORE_DEBUG) {
                    Slog.v(TAG, "Got preflight response; size=" + totalSize);
                }

                IBackupTransport transport =
                        mTransportClient.connectOrThrow("PFTBT$SPBP.preflightFullBackup()");
                result = transport.checkFullBackupSize(totalSize);
                if (result == BackupTransport.TRANSPORT_QUOTA_EXCEEDED) {
                    if (MORE_DEBUG) {
                        Slog.d(TAG, "Package hit quota limit on preflight " +
                                pkg.packageName + ": " + totalSize + " of " + mQuota);
                    }
                    agent.doQuotaExceeded(totalSize, mQuota);
                }
            } catch (Exception e) {
                Slog.w(TAG, "Exception preflighting " + pkg.packageName + ": " + e.getMessage());
                result = BackupTransport.AGENT_ERROR;
            }
            return result;
        }

        @Override
        public void execute() {
            // Unused.
        }

        @Override
        public void operationComplete(long result) {
            // got the callback, and our preflightFullBackup() method is waiting for the result
            if (MORE_DEBUG) {
                Slog.i(TAG, "Preflight op complete, result=" + result);
            }
            mResult.set(result);
            mLatch.countDown();
            backupManagerService.removeOperation(mCurrentOpToken);
        }

        @Override
        public void handleCancel(boolean cancelAll) {
            if (MORE_DEBUG) {
                Slog.i(TAG, "Preflight cancelled; failing");
            }
            mResult.set(BackupTransport.AGENT_ERROR);
            mLatch.countDown();
            backupManagerService.removeOperation(mCurrentOpToken);
        }

        @Override
        public long getExpectedSizeOrErrorCode() {
            long fullBackupAgentTimeoutMillis =
                    mAgentTimeoutParameters.getFullBackupAgentTimeoutMillis();
            try {
                mLatch.await(fullBackupAgentTimeoutMillis, TimeUnit.MILLISECONDS);
                return mResult.get();
            } catch (InterruptedException e) {
                return BackupTransport.NO_MORE_DATA;
            }
        }
    }

    class SinglePackageBackupRunner implements Runnable, BackupRestoreTask {
        final ParcelFileDescriptor mOutput;
        final PackageInfo mTarget;
        final SinglePackageBackupPreflight mPreflight;
        final CountDownLatch mPreflightLatch;
        final CountDownLatch mBackupLatch;
        private final int mCurrentOpToken;
        private final int mEphemeralToken;
        private FullBackupEngine mEngine;
        private volatile int mPreflightResult;
        private volatile int mBackupResult;
        private final long mQuota;
        private volatile boolean mIsCancelled;
        private final int mTransportFlags;

        SinglePackageBackupRunner(ParcelFileDescriptor output, PackageInfo target,
                TransportClient transportClient, long quota, int currentOpToken, int transportFlags)
                throws IOException {
            mOutput = ParcelFileDescriptor.dup(output.getFileDescriptor());
            mTarget = target;
            mCurrentOpToken = currentOpToken;
            mEphemeralToken = backupManagerService.generateRandomIntegerToken();
            mPreflight = new SinglePackageBackupPreflight(
                    transportClient, quota, mEphemeralToken, transportFlags);
            mPreflightLatch = new CountDownLatch(1);
            mBackupLatch = new CountDownLatch(1);
            mPreflightResult = BackupTransport.AGENT_ERROR;
            mBackupResult = BackupTransport.AGENT_ERROR;
            mQuota = quota;
            mTransportFlags = transportFlags;
            registerTask();
        }

        void registerTask() {
            synchronized (backupManagerService.getCurrentOpLock()) {
                backupManagerService.getCurrentOperations().put(
                        mCurrentOpToken, new Operation(OP_PENDING, this, OP_TYPE_BACKUP_WAIT));
            }
        }

        void unregisterTask() {
            synchronized (backupManagerService.getCurrentOpLock()) {
                backupManagerService.getCurrentOperations().remove(mCurrentOpToken);
            }
        }

        @Override
        public void run() {
            FileOutputStream out = new FileOutputStream(mOutput.getFileDescriptor());
            mEngine = new FullBackupEngine(backupManagerService, out, mPreflight, mTarget, false,
                    this, mQuota, mCurrentOpToken, mTransportFlags);
            try {
                try {
                    if (!mIsCancelled) {
                        mPreflightResult = mEngine.preflightCheck();
                    }
                } finally {
                    mPreflightLatch.countDown();
                }
                // If there is no error on preflight, continue backup.
                if (mPreflightResult == BackupTransport.TRANSPORT_OK) {
                    if (!mIsCancelled) {
                        mBackupResult = mEngine.backupOnePackage();
                    }
                }
            } catch (Exception e) {
                Slog.e(TAG, "Exception during full package backup of " + mTarget.packageName);
            } finally {
                unregisterTask();
                mBackupLatch.countDown();
                try {
                    mOutput.close();
                } catch (IOException e) {
                    Slog.w(TAG, "Error closing transport pipe in runner");
                }
            }
        }

        public void sendQuotaExceeded(final long backupDataBytes, final long quotaBytes) {
            mEngine.sendQuotaExceeded(backupDataBytes, quotaBytes);
        }

        // If preflight succeeded, returns positive number - preflight size,
        // otherwise return negative error code.
        long getPreflightResultBlocking() {
            long fullBackupAgentTimeoutMillis =
                    mAgentTimeoutParameters.getFullBackupAgentTimeoutMillis();
            try {
                mPreflightLatch.await(fullBackupAgentTimeoutMillis, TimeUnit.MILLISECONDS);
                if (mIsCancelled) {
                    return BackupManager.ERROR_BACKUP_CANCELLED;
                }
                if (mPreflightResult == BackupTransport.TRANSPORT_OK) {
                    return mPreflight.getExpectedSizeOrErrorCode();
                } else {
                    return mPreflightResult;
                }
            } catch (InterruptedException e) {
                return BackupTransport.AGENT_ERROR;
            }
        }

        int getBackupResultBlocking() {
            long fullBackupAgentTimeoutMillis =
                    mAgentTimeoutParameters.getFullBackupAgentTimeoutMillis();
            try {
                mBackupLatch.await(fullBackupAgentTimeoutMillis, TimeUnit.MILLISECONDS);
                if (mIsCancelled) {
                    return BackupManager.ERROR_BACKUP_CANCELLED;
                }
                return mBackupResult;
            } catch (InterruptedException e) {
                return BackupTransport.AGENT_ERROR;
            }
        }


        // BackupRestoreTask interface: specifically, timeout detection

        @Override
        public void execute() { /* intentionally empty */ }

        @Override
        public void operationComplete(long result) { /* intentionally empty */ }

        @Override
        public void handleCancel(boolean cancelAll) {
            if (DEBUG) {
                Slog.w(TAG, "Full backup cancel of " + mTarget.packageName);
            }

            mMonitor = BackupManagerMonitorUtils.monitorEvent(mMonitor,
                    BackupManagerMonitor.LOG_EVENT_ID_FULL_BACKUP_CANCEL,
                    mCurrentPackage, BackupManagerMonitor.LOG_EVENT_CATEGORY_AGENT, null);
            mIsCancelled = true;
            // Cancel tasks spun off by this task.
            backupManagerService.handleCancel(mEphemeralToken, cancelAll);
            backupManagerService.tearDownAgentAndKill(mTarget.applicationInfo);
            // Free up everyone waiting on this task and its children.
            mPreflightLatch.countDown();
            mBackupLatch.countDown();
            // We are done with this operation.
            backupManagerService.removeOperation(mCurrentOpToken);
        }
    }
}
