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

import android.cloudconnector.ICloudConnectorEventListener;
import android.cloudconnector.ICloudConnectorManager;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

/**
 * Helper class used to wrap a Cloud Connector Event Listener object.
 *
 * @hide
 */
public class EventListenerTransport extends ICloudConnectorEventListener.Stub {

    // Constants.
    private static final int TYPE_CONNECTED = 0;
    private static final int TYPE_DISCONNECTED = 1;
    private static final int TYPE_CONNECTION_ERROR = 2;
    private static final int TYPE_DATA_POINTS_SUCCESS = 3;
    private static final int TYPE_DATA_POINTS_ERROR = 4;

    // Variables.
    private CloudConnectorEventListenerImpl mListener;

    private final Handler mListenerHandler;

    private ICloudConnectorManager mService;

    /**
     * Class constructor. Instantiates a new {@code EventListenerTransport}
     * object with the given parameters.
     *
     * @param listener Cloud Connector Event Listener.
     * @param service Cloud Connector service.
     */
    EventListenerTransport(CloudConnectorEventListenerImpl listener, ICloudConnectorManager service) {
        mListener = listener;
        mService = service;

        mListenerHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                _handleMessage(msg);
            }
        };
    }

    @Override
    public void connected() {
        Message msg = Message.obtain();
        msg.what = TYPE_CONNECTED;
        mListenerHandler.sendMessage(msg);
    }

    @Override
    public void disconnected() {
        Message msg = Message.obtain();
        msg.what = TYPE_DISCONNECTED;
        mListenerHandler.sendMessage(msg);
    }

    @Override
    public void connectionError(String errorMessage) {
        Message msg = Message.obtain();
        msg.what = TYPE_CONNECTION_ERROR;
        msg.obj = errorMessage;
        mListenerHandler.sendMessage(msg);
    }

    @Override
    public void sendDataPointsSuccess() {
        Message msg = Message.obtain();
        msg.what = TYPE_DATA_POINTS_SUCCESS;
        mListenerHandler.sendMessage(msg);
    }

    @Override
    public void sendDataPointsError(String errorMessage) {
        Message msg = Message.obtain();
        msg.what = TYPE_DATA_POINTS_ERROR;
        msg.obj = errorMessage;
        mListenerHandler.sendMessage(msg);
    }

    /**
     * Handles the given message.
     *
     * @param msg Message to handle.
     */
    private void _handleMessage(Message msg) {
        switch (msg.what) {
        case TYPE_CONNECTED:
            mListener.connected();
            break;
        case TYPE_DISCONNECTED:
            mListener.disconnected();
            break;
        case TYPE_CONNECTION_ERROR:
            mListener.connectionError((String)msg.obj);
            break;
        case TYPE_DATA_POINTS_SUCCESS:
            mListener.sendDataPointsSuccess();
            break;
        case TYPE_DATA_POINTS_ERROR:
            mListener.sendDataPointsError((String)msg.obj);
            break;
        }
        try {
            mService.eventCallbackFinished(this);
        } catch (RemoteException e) {
            Log.e(CloudConnectorHandler.TAG, "RemoteException in eventCallbackFinished", e);
        }
    }
}
