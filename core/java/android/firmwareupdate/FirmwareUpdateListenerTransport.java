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

package android.firmwareupdate;

import android.firmwareupdate.IFirmwareUpdateManager;
import android.firmwareupdate.IFirmwareUpdateListener;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

/**
 * Helper class used to wrap a firmware update listener object.
 *
 * @hide
 */
public class FirmwareUpdateListenerTransport extends IFirmwareUpdateListener.Stub {

    // Constants.
    private static final int TYPE_VERIFY_STARTED = 0;
    private static final int TYPE_VERIFY_PROGRESS = 1;
    private static final int TYPE_VERIFY_FINISHED = 2;
    private static final int TYPE_UPDATE_STARTED = 3;
    private static final int TYPE_ERROR = 4;

    // Variables.
    private FirmwareUpdateListenerImpl mListener;

    private Handler mListenerHandler;

    private IFirmwareUpdateManager mService;

    /**
     * Class constructor. Instantiates a new {@code FirmwareUpdateListenerTransport}
     * object with the given parameters.
     *
     * @param listener Firmware update listener.
     * @param service Firmware update service.
     */
    FirmwareUpdateListenerTransport(FirmwareUpdateListenerImpl listener, IFirmwareUpdateManager service) {
        mListener = listener;
        mService = service;

        Thread workerThread = new Thread() {
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                Looper.prepare();
                mListenerHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        _handleMessage(msg);
                    }
                };
                Looper.loop();
            };
        };
        workerThread.start();
    }

    @Override
    public void verifyStarted() {
        mListenerHandler.sendEmptyMessage(TYPE_VERIFY_STARTED);
    }

    @Override
    public void verifyProgress(int progress) {
        Message msg = Message.obtain();
        msg.what = TYPE_VERIFY_PROGRESS;
        msg.arg1 = progress;
        mListenerHandler.sendMessage(msg);
    }

    @Override
    public void verifyFinished() {
        mListenerHandler.sendEmptyMessage(TYPE_VERIFY_FINISHED);
    }

    @Override
    public void updateStarted() {
        mListenerHandler.sendEmptyMessage(TYPE_UPDATE_STARTED);
    }

    @Override
    public void onError(String error) {
        Message msg = Message.obtain();
        msg.what = TYPE_ERROR;
        msg.obj = error;
        mListenerHandler.sendMessage(msg);
    }

    /**
     * Handles the given message.
     *
     * @param msg Message to handle.
     */
    private void _handleMessage(Message msg) {
        switch (msg.what) {
        case TYPE_VERIFY_STARTED:
            mListener.verifyStarted();
            break;
        case TYPE_VERIFY_PROGRESS:
            mListener.verifyProgress(msg.arg1);
            break;
        case TYPE_VERIFY_FINISHED:
            mListener.verifyFinished();
            break;
        case TYPE_UPDATE_STARTED:
            mListener.updateStarted();
            break;
        case TYPE_ERROR:
            mListener.onError((String)msg.obj);
            break;
        }
        try {
            mService.listenerCallbackFinished(this);
        } catch (RemoteException e) {
            Log.e(FirmwareUpdateHandler.TAG, "RemoteException in progressCallbackFinished", e);
        }
    }
}
