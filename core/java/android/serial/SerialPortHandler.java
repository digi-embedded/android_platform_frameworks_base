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

package android.serial;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.TooManyListenersException;

import android.content.Context;
import android.gnu.io.NoSuchPortException;
import android.gnu.io.PortInUseException;
import android.gnu.io.UnsupportedCommOperationException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.serial.ISerialPortManager;
import android.util.Log;

/**
 * This class provides access to the serial ports.
 *
 * <p>Unless noted, all serial API methods require the
 * {@code com.digi.android.permission.SERIAL} permission. If your application
 * does not have this permission it will not have access to any serial port
 * service feature.</p>
 *
 * @hide
 */
public class SerialPortHandler {

    // Constants.
    private static final String TAG = "SerialPortManager";

    private static final int TYPE_SERIAL_PORT_EVENT = 1;

    /** @hide */
    public static final String ID_DATA = "data";
    /** @hide */
    public static final String ID_CAUSE = "cause";
    /** @hide */
    public static final String ID_PORT = "port";
    /** @hide */
    public static final String ID_TYPE = "type";
    /** @hide */
    public static final String ID_OLD_VALUE = "old_value";
    /** @hide */
    public static final String ID_NEW_VALUE = "new_value";
    /** @hide */
    public static final String ID_EXCEPTION_NO_SUCH_PORT = "no_such_port_exception";
    /** @hide */
    public static final String ID_EXCEPTION_PORT_IN_USE = "port_in_use_exception";
    /** @hide */
    public static final String ID_EXCEPTION_UNSUPPORTED_OPERATION = "unsupported_operation_exception";
    /** @hide */
    public static final String ID_EXCEPTION_PORT_NOT_OPEN = "port_not_open_exception";
    /** @hide */
    public static final String ID_EXCEPTION_IO = "io_exception";
    /** @hide */
    public static final String ID_EXCEPTION_TOO_MANY_LISTENERS = "too_many_listeners";

    // Variables.
    private final ISerialPortManager service;

    // Map from SerialPortListenerImpl to their associated ListenerTransport objects
    private HashMap<SerialPortListenerImpl, ListenerTransport> listeners = new HashMap<>();

    private HashMap<String, OutputStream> outputStreams = new HashMap<>();
    private HashMap<String, InputStream> inputStreams = new HashMap<>();

    private Handler mListenerHandler;

    /**
     * @hide
     */
    public SerialPortHandler(Context context, ISerialPortManager service) {
        this.service = service;

        initWorkerThread();
    }

    /**
     * Initializes the worker thread that will take care of handler actions
     * while the main thread performs its tasks.
     */
    private void initWorkerThread() {
        Thread workerThread = new Thread() {
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                Looper.prepare();
                mListenerHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        switch (msg.what) {
                        case TYPE_SERIAL_PORT_EVENT:
                            Bundle bundle = msg.getData();
                            String port = bundle.getString(ID_PORT);
                            synchronized (listeners) {
                                for (ListenerTransport transport:listeners.values()) {
                                    if (transport.getPort().equals(port)) {
                                        transport.getListener().serialEvent(port,
                                                bundle.getInt(ID_TYPE),
                                                bundle.getBoolean(ID_OLD_VALUE),
                                                bundle.getBoolean(ID_NEW_VALUE));
                                        try {
                                            service.serialEventCallbackFinished(port);
                                        } catch (RemoteException e) {
                                            Log.e(TAG, "serialEventCallbackFinished: RemoteException", e);
                                        }
                                        break;
                                    }
                                }
                            }
                            break;
                        }
                    }
                };
                Looper.loop();
            };
        };
        workerThread.start();
    }

    /**
     * Lists all the available serial ports.
     *
     * @return List with the available serial ports.
     *
     * @hide
     */
    public String[] listSerialPorts() {
        // Read timeout from the service.
        try {
            return service.listSerialPorts();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in listSerialPorts", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Opens a serial port connection with the given port name
     * and serial port parameters.
     *
     * @param port Port name.
     * @param baudRate Baud rate.
     * @param dataBits Data bits.
     * @param stopBits Stop bits.
     * @param parity Parity.
     * @param flowControl Flow control.
     * @param readTimeout Read timeout.
     *
     * @throws NoSuchPortException If the given port does not exist.
     * @throws PortInUseException If the given port is in use.
     * @throws UnsupportedCommOperationException If the serial port parameters
     *                                           are invalid.
     *
     * @hide
     */
    public void openSerialPort(String port, int baudRate, int dataBits, int stopBits, int parity,
            int flowControl, int readTimeout) throws NoSuchPortException, PortInUseException,
            UnsupportedCommOperationException {
        try {
            Bundle bundle = service.openSerialPort(port, baudRate, dataBits, stopBits, parity, flowControl, readTimeout);
            if (bundle.containsKey(ID_EXCEPTION_NO_SUCH_PORT))
                throw new NoSuchPortException(bundle.getString(ID_EXCEPTION_NO_SUCH_PORT),
                        new Throwable(bundle.getString(ID_CAUSE)));
            if (bundle.containsKey(ID_EXCEPTION_PORT_IN_USE))
                throw new PortInUseException(bundle.getString(ID_EXCEPTION_PORT_IN_USE),
                        new Throwable(bundle.getString(ID_CAUSE)));
            if (bundle.containsKey(ID_EXCEPTION_UNSUPPORTED_OPERATION))
                throw new UnsupportedCommOperationException(bundle.getString(ID_EXCEPTION_UNSUPPORTED_OPERATION),
                        new Throwable(bundle.getString(ID_CAUSE)));
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in openSerialPort", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Closes the serial port.
     *
     * @param port Serial port.
     *
     * @hide
     */
    public void close(String port) {
        InputStream is = inputStreams.remove(port);
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) { }
        }
        OutputStream os = outputStreams.remove(port);
        if (is != null) {
            try {
                os.close();
            } catch (IOException e) { }
        }
        try {
            Bundle bundle = service.close(port);
            checkPortNotOpenException(bundle);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in close", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the connection interface input stream to read data from.
     *
     * @param port Serial port.
     *
     * @return The connection interface input stream to read data from.
     *
     * @throws IOException If an I/O error occurs.
     *
     * @hide
     */
    public InputStream getInputStream(String port) throws IOException {
        try {
            Bundle bundle = service.getInputStream(port);
            checkPortNotOpenException(bundle);
            if (bundle.containsKey(ID_EXCEPTION_IO))
                throw new IOException(bundle.getString(ID_EXCEPTION_IO));
            ParcelFileDescriptor pfd = bundle.getParcelable(ID_DATA);
            InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
            inputStreams.put(port, is);
            return is;
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getInputStream", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the connection interface output stream to write data to.
     *
     * @param port Serial port.
     *
     * @return The connection interface output stream to write data to.
     *
     * @throws IOException If an I/O error occurs.
     *
     * @hide
     */
    public OutputStream getOutputStream(String port) throws IOException {
        try {
            Bundle bundle = service.getOutputStream(port);
            checkPortNotOpenException(bundle);
            if (bundle.containsKey(ID_EXCEPTION_IO))
                throw new IOException(bundle.getString(ID_EXCEPTION_IO));
            ParcelFileDescriptor pfd = bundle.getParcelable(ID_DATA);
            OutputStream os = new ParcelFileDescriptor.AutoCloseOutputStream(pfd);
            outputStreams.put(port, os);
            return os;
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getOutputStream", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Registers the given listener to be notified when a serial port event
     * occurs.
     *
     * <p>Note that you can only register one listener.</p>
     *
     * @param port Serial port.
     * @param listener Serial port event listener to add.
     *
     * @throws TooManyListenersException If you register more than one listener.
     *
     * @hide
     */
    public void addEventListener(String port, SerialPortListenerImpl listener) throws TooManyListenersException {
        // Wrap the listener class.
        ListenerTransport transport = wrapListener(port, listener);

        // Register the listener in the service.
        try {
            Bundle bundle = service.addEventListener(port, transport);
            checkPortNotOpenException(bundle);
            if (bundle.containsKey(ID_EXCEPTION_TOO_MANY_LISTENERS))
                throw new TooManyListenersException(bundle.getString(ID_EXCEPTION_TOO_MANY_LISTENERS));
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in addEventListener", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Unregisters the configured serial port event listener.
     *
     * @param port Serial port.
     * @param listener Serial port event listener to remove.
     *
     * @hide
     */
    public void removeEventListener(String port, SerialPortListenerImpl listener) {
        // Remove listener transport from the list.
        ListenerTransport transport;
        synchronized (listeners) {
            transport = listeners.remove(listener);
        }
        // If listener transport was not in the list, return.
        if (transport == null)
            return;

        // Remove the listener from the service.
        try {
            Bundle bundle = service.removeEventListener(port);
            checkPortNotOpenException(bundle);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in removeEventListener", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the new parameters of the serial port.
     *
     * @param port Serial port.
     * @param baudRate Baud rate.
     * @param dataBits Data bits.
     * @param stopBits Stop bits.
     * @param parity Parity.
     * @param flowControl Flow control.
     *
     * @throws UnsupportedCommOperationException If the operation is not
     *                                           supported by the driver.
     *
     * @hide
     */
    public void setPortParameters(String port, int baudRate, int dataBits, int stopBits, int parity, 
            int flowControl) throws UnsupportedCommOperationException {
        try {
            Bundle bundle = service.setPortParameters(port, baudRate, dataBits, stopBits, parity, flowControl);
            checkPortNotOpenException(bundle);
            if (bundle.containsKey(ID_EXCEPTION_UNSUPPORTED_OPERATION))
                throw new UnsupportedCommOperationException(bundle.getString(ID_EXCEPTION_UNSUPPORTED_OPERATION),
                        new Throwable(bundle.getString(ID_CAUSE)));
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in setPortParameters", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the configured baud rate.
     *
     * @param port Serial port.
     *
     * @return Baud rate.
     *
     * @hide
     */
    public int getBaudRate(String port) {
        try {
            Bundle bundle = service.getBaudRate(port);
            checkPortNotOpenException(bundle);
            return bundle.getInt(ID_DATA);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getBaudRate", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the configured data bits.
     *
     * @param port Serial port.
     *
     * @return Data bits.
     *
     * @hide
     */
    public int getDataBits(String port) {
        try {
            Bundle bundle = service.getDataBits(port);
            checkPortNotOpenException(bundle);
            return bundle.getInt(ID_DATA);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getDataBits", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the configured stop bits.
     *
     * @param port Serial port.
     *
     * @return Stop bits.
     *
     * @hide
     */
    public int getStopBits(String port) {
        try {
            Bundle bundle = service.getStopBits(port);
            checkPortNotOpenException(bundle);
            return bundle.getInt(ID_DATA);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getStopBits", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the configured parity.
     *
     * @param port Serial port.
     *
     * @return Parity.
     *
     * @hide
     */
    public int getParity(String port) {
        try {
            Bundle bundle = service.getParity(port);
            checkPortNotOpenException(bundle);
            return bundle.getInt(ID_DATA);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getParity", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the configured flow control.
     *
     * @param port Serial port.
     *
     * @return Flow control.
     *
     * @hide
     */
    public int getFlowControl(String port) {
        try {
            Bundle bundle = service.getFlowControl(port);
            checkPortNotOpenException(bundle);
            return bundle.getInt(ID_DATA);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getFlowControl", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the state of the DTR line.
     *
     * @param port Serial port.
     *
     * @return {@code true} if the line is high, {@code false} otherwise.
     *
     * @hide
     */
    public boolean isDTR(String port) {
        try {
            Bundle bundle = service.isDTR(port);
            checkPortNotOpenException(bundle);
            return bundle.getBoolean(ID_DATA);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in isDTR", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the state of the DTR.
     *
     * @param port Serial port.
     * @param state {@code true} to set the line status high, {@code false} to
     *              set it low.
     *
     * @hide
     */
    public void setDTR(String port, boolean state) {
        try {
            Bundle bundle = service.setDTR(port, state);
            checkPortNotOpenException(bundle);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in setDTR", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the state of the RTS line.
     *
     * @param port Serial port.
     *
     * @return {@code true} if the line is high, {@code false} otherwise.
     *
     * @hide
     */
    public boolean isRTS(String port) {
        try {
            Bundle bundle = service.isRTS(port);
            checkPortNotOpenException(bundle);
            return bundle.getBoolean(ID_DATA);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in isRTS", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the state of the RTS line.
     *
     * @param port Serial port.
     *
     * @param state {@code true} to set the line status high, {@code false} to
     *              set it low.
     *
     * @hide
     */
    public void setRTS(String port, boolean state) {
        try {
            Bundle bundle = service.setRTS(port, state);
            checkPortNotOpenException(bundle);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in setRTS", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the state of the CTS line.
     *
     * @param port Serial port.
     *
     * @return {@code true} if the line is high, {@code false} otherwise.
     *
     * @hide
     */
    public boolean isCTS(String port) {
        try {
            Bundle bundle = service.isCTS(port);
            checkPortNotOpenException(bundle);
            return bundle.getBoolean(ID_DATA);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in isCTS", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the state of the DSR line.
     *
     * @param port Serial port.
     *
     * @return {@code true} if the line is high, {@code false} otherwise.
     *
     * @hide
     */
    public boolean isDSR(String port) {
        try {
            Bundle bundle = service.isDSR(port);
            checkPortNotOpenException(bundle);
            return bundle.getBoolean(ID_DATA);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in isDSR", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the state of the CD line.
     *
     * @param port Serial port.
     *
     * @return {@code true} if the line is high, {@code false} otherwise.
     *
     * @hide
     */
    public boolean isCD(String port) {
        try {
            Bundle bundle = service.isCD(port);
            checkPortNotOpenException(bundle);
            return bundle.getBoolean(ID_DATA);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in isCD", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the state of the RI line.
     *
     * @param port Serial port.
     *
     * @return {@code true} if the line is high, {@code false} otherwise.
     *
     * @hide
     */
    public boolean isRI(String port) {
        try {
            Bundle bundle = service.isRI(port);
            checkPortNotOpenException(bundle);
            return bundle.getBoolean(ID_DATA);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in isRI", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sends a break signal to the serial port with the given duration
     * (in milliseconds).
     *
     * @param port Serial port.
     * @param duration Duration of the break signal in milliseconds.
     *
     * @hide
     */
    public void sendBreak(String port, int duration) {
        try {
            Bundle bundle = service.sendBreak(port, duration);
            checkPortNotOpenException(bundle);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in sendBreak", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets whether the listener should be notified on data available.
     *
     * @param port Serial port.
     * @param enable {@code true} to notify.
     *
     * @hide
     */
    public void notifyOnDataAvailable(String port, boolean enable) {
        try {
            Bundle bundle = service.notifyOnDataAvailable(port, enable);
            checkPortNotOpenException(bundle);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in notifyOnDataAvailable", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets whether the listener should be notified on output empty.
     *
     * @param port Serial port.
     * @param enable {@code true} to notify.
     *
     * @hide
     */
    public void notifyOnOutputEmpty(String port, boolean enable) {
        try {
            Bundle bundle = service.notifyOnOutputEmpty(port, enable);
            checkPortNotOpenException(bundle);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in notifyOnOutputEmpty", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets whether the listener should be notified on CTS.
     *
     * @param port Serial port.
     * @param enable {@code true} to notify.
     *
     * @hide
     */
    public void notifyOnCTS(String port, boolean enable) {
        try {
            Bundle bundle = service.notifyOnCTS(port, enable);
            checkPortNotOpenException(bundle);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in notifyOnCTS", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets whether the listener should be notified on DSR.
     *
     * @param port Serial port.
     * @param enable {@code true} to notify.
     *
     * @hide
     */
    public void notifyOnDSR(String port, boolean enable) {
        try {
            Bundle bundle = service.notifyOnDSR(port, enable);
            checkPortNotOpenException(bundle);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in notifyOnDSR", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets whether the listener should be notified on ring indicator.
     *
     * @param port Serial port.
     * @param enable {@code true} to notify.
     *
     * @hide
     */
    public void notifyOnRingIndicator(String port, boolean enable) {
        try {
            Bundle bundle = service.notifyOnRingIndicator(port, enable);
            checkPortNotOpenException(bundle);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in notifyOnRingIndicator", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets whether the listener should be notified on carrier detect.
     *
     * @param port Serial port.
     * @param enable {@code true} to notify.
     *
     * @hide
     */
    public void notifyOnCarrierDetect(String port, boolean enable) {
        try {
            Bundle bundle = service.notifyOnCarrierDetect(port, enable);
            checkPortNotOpenException(bundle);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in notifyOnCarrierDetect", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets whether the listener should be notified on overrun error.
     *
     * @param port Serial port.
     * @param enable {@code true} to notify.
     *
     * @hide
     */
    public void notifyOnOverrunError(String port, boolean enable) {
        try {
            Bundle bundle = service.notifyOnOverrunError(port, enable);
            checkPortNotOpenException(bundle);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in notifyOnOverrunError", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets whether the listener should be notified on parity error.
     *
     * @param port Serial port.
     * @param enable {@code true} to notify.
     *
     * @hide
     */
    public void notifyOnParityError(String port, boolean enable) {
        try {
            Bundle bundle = service.notifyOnParityError(port, enable);
            checkPortNotOpenException(bundle);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in notifyOnParityError", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets whether the listener should be notified on framing error.
     *
     * @param port Serial port.
     * @param enable {@code true} to notify.
     *
     * @hide
     */
    public void notifyOnFramingError(String port, boolean enable) {
        try {
            Bundle bundle = service.notifyOnFramingError(port, enable);
            checkPortNotOpenException(bundle);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in notifyOnFramingError", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets whether the listener should be notified on break interrupt.
     *
     * @param port Serial port.
     * @param enable {@code true} to notify.
     *
     * @hide
     */
    public void notifyOnBreakInterrupt(String port, boolean enable) {
        try {
            Bundle bundle = service.notifyOnBreakInterrupt(port, enable);
            checkPortNotOpenException(bundle);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in notifyOnBreakInterrupt", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the parity error character.
     *
     * @param port Serial port.
     *
     * @return The Parity Error Character.
     *
     * @throws UnsupportedCommOperationException If the operation is not
     *                                           supported by the driver.
     *
     * @hide
     */
    public byte getParityErrorChar(String port) throws UnsupportedCommOperationException {
        try {
            Bundle bundle = service.getParityErrorChar(port);
            checkPortNotOpenException(bundle);
            if (bundle.containsKey(ID_EXCEPTION_UNSUPPORTED_OPERATION))
                throw new UnsupportedCommOperationException(bundle.getString(ID_EXCEPTION_UNSUPPORTED_OPERATION),
                        new Throwable(bundle.getString(ID_CAUSE)));
            return bundle.getByte(ID_DATA);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getParityErrorChar", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the parity error character.
     *
     * @param port Serial port.
     * @param b Parity Error Character.
     *
     * @return {@code true} on success.
     *
     * @throws UnsupportedCommOperationException If the operation is not
     *                                           supported by the driver.
     *
     * @hide
     */
    public boolean setParityErrorChar(String port, byte b) throws UnsupportedCommOperationException {
        try {
            Bundle bundle = service.setParityErrorChar(port, b);
            checkPortNotOpenException(bundle);
            if (bundle.containsKey(ID_EXCEPTION_UNSUPPORTED_OPERATION))
                throw new UnsupportedCommOperationException(bundle.getString(ID_EXCEPTION_UNSUPPORTED_OPERATION),
                        new Throwable(bundle.getString(ID_CAUSE)));
            return bundle.getBoolean(ID_DATA);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in setParityErrorChar", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the end of input character.
     *
     * @param port Serial port.
     *
     * @return The End of Input Character.
     *
     * @throws UnsupportedCommOperationException If the operation is not
     *                                           supported by the driver.
     *
     * @hide
     */
    public byte getEndOfInputChar(String port) throws UnsupportedCommOperationException {
        try {
            Bundle bundle = service.getEndOfInputChar(port);
            checkPortNotOpenException(bundle);
            if (bundle.containsKey(ID_EXCEPTION_UNSUPPORTED_OPERATION))
                throw new UnsupportedCommOperationException(bundle.getString(ID_EXCEPTION_UNSUPPORTED_OPERATION),
                        new Throwable(bundle.getString(ID_CAUSE)));
            return bundle.getByte(ID_DATA);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getEndOfInputChar", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the end of input character.
     *
     * @param port Serial port.
     * @param b End Of Input Character.
     *
     * @return {@code true} on success.
     *
     * @throws UnsupportedCommOperationException If the operation is not
     *                                           supported by the driver.
     *
     * @hide
     */
    public boolean setEndOfInputChar(String port, byte b) throws UnsupportedCommOperationException {
        try {
            Bundle bundle = service.setEndOfInputChar(port, b);
            checkPortNotOpenException(bundle);
            if (bundle.containsKey(ID_EXCEPTION_UNSUPPORTED_OPERATION))
                throw new UnsupportedCommOperationException(bundle.getString(ID_EXCEPTION_UNSUPPORTED_OPERATION),
                        new Throwable(bundle.getString(ID_CAUSE)));
            return bundle.getBoolean(ID_DATA);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in setEndOfInputChar", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the baud base value.
     *
     * @param port Serial port.
     * @param baudBase The clock frequency divided by 16. Default
     *                 BaudBase is 115200.
     *
     * @return {@code true} on success.
     *
     * @throws IOException If an I/O error occurs.
     * @throws UnsupportedCommOperationException If the operation is not
     *                                           supported by the driver.
     *
     * @hide
     */
    public boolean setBaudBase(String port, int baudBase)
            throws UnsupportedCommOperationException, IOException {
        try {
            Bundle bundle = service.setBaudBase(port, baudBase);
            checkPortNotOpenException(bundle);
            if (bundle.containsKey(ID_EXCEPTION_UNSUPPORTED_OPERATION))
                throw new UnsupportedCommOperationException(bundle.getString(ID_EXCEPTION_UNSUPPORTED_OPERATION),
                        new Throwable(bundle.getString(ID_CAUSE)));
            if (bundle.containsKey(ID_EXCEPTION_IO))
                throw new UnsupportedCommOperationException(bundle.getString(ID_EXCEPTION_IO));
            return bundle.getBoolean(ID_DATA);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in setBaudBase", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the baud base value.
     *
     * @param port Serial port.
     *
     * @return Baud base.
     *
     * @throws IOException If an I/O error occurs.
     * @throws UnsupportedCommOperationException If the operation is not
     *                                           supported by the driver.
     *
     * @hide
     */
    public int getBaudBase(String port) throws UnsupportedCommOperationException, IOException {
        try {
            Bundle bundle = service.getBaudBase(port);
            checkPortNotOpenException(bundle);
            if (bundle.containsKey(ID_EXCEPTION_UNSUPPORTED_OPERATION))
                throw new UnsupportedCommOperationException(bundle.getString(ID_EXCEPTION_UNSUPPORTED_OPERATION),
                        new Throwable(bundle.getString(ID_CAUSE)));
            if (bundle.containsKey(ID_EXCEPTION_IO))
                throw new UnsupportedCommOperationException(bundle.getString(ID_EXCEPTION_IO));
            return bundle.getInt(ID_DATA);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getBaudBase", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the divisor value.
     *
     * @param port Serial port.
     * @param divisor Divisor.
     *
     * @return {@code true} on success.
     *
     * @throws IOException If an I/O error occurs.
     * @throws UnsupportedCommOperationException If the operation is not
     *                                           supported by the driver.
     *
     * @hide
     */
    public boolean setDivisor(String port, int divisor)
        throws UnsupportedCommOperationException, IOException {
        try {
            Bundle bundle = service.setDivisor(port, divisor);
            checkPortNotOpenException(bundle);
            if (bundle.containsKey(ID_EXCEPTION_UNSUPPORTED_OPERATION))
                throw new UnsupportedCommOperationException(bundle.getString(ID_EXCEPTION_UNSUPPORTED_OPERATION),
                        new Throwable(bundle.getString(ID_CAUSE)));
            if (bundle.containsKey(ID_EXCEPTION_IO))
                throw new UnsupportedCommOperationException(bundle.getString(ID_EXCEPTION_IO));
            return bundle.getBoolean(ID_DATA);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in setDivisor", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the divisor value.
     *
     * @param port Serial port.
     *
     * @return Divisor.
     *
     * @throws IOException If an I/O error occurs.
     * @throws UnsupportedCommOperationException If the operation is not
     *                                           supported by the driver.
     *
     * @hide
     */
    public int getDivisor(String port)
        throws UnsupportedCommOperationException, IOException {
        try {
            Bundle bundle = service.getDivisor(port);
            checkPortNotOpenException(bundle);
            if (bundle.containsKey(ID_EXCEPTION_UNSUPPORTED_OPERATION))
                throw new UnsupportedCommOperationException(bundle.getString(ID_EXCEPTION_UNSUPPORTED_OPERATION),
                        new Throwable(bundle.getString(ID_CAUSE)));
            if (bundle.containsKey(ID_EXCEPTION_IO))
                throw new UnsupportedCommOperationException(bundle.getString(ID_EXCEPTION_IO));
            return bundle.getInt(ID_DATA);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getDivisor", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Disables receive timeout.
     *
     * @param port Serial port.
     *
     * @hide
     */
    public void disableReceiveTimeout(String port) {
        try {
            Bundle bundle = service.disableReceiveTimeout(port);
            checkPortNotOpenException(bundle);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in disableReceiveTimeout", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Enables receive timeout, if this feature is supported by the driver.
     * When the receive timeout condition becomes true, a {@code read} from the
     * input stream for this port will return immediately.
     *
     * <p>This is an advisory method which the driver may not implement. By
     * default, receive timeout is not enabled.</p>
     *
     * <p>An application can determine whether the driver supports this feature
     * by first calling the {@link #enableReceiveTimeout(int)} method and then
     * calling the {@link #isReceiveTimeoutEnabled()} method. If it returns
     * {@code false}, then receive timeout is not supported by the driver.</p>
     *
     * @param port Serial port.
     * @param time When this many milliseconds have elapsed, return immediately
     *             from {@code read}, regardless of bytes in input buffer.
     *
     * @throws UnsupportedCommOperationException If the operation is not 
     *                                           supported by the driver.
     *
     * @hide
     */
    public void enableReceiveTimeout(String port, int time) throws UnsupportedCommOperationException {
        try {
            Bundle bundle = service.enableReceiveTimeout(port, time);
            checkPortNotOpenException(bundle);
            if (bundle.containsKey(ID_EXCEPTION_UNSUPPORTED_OPERATION))
                throw new UnsupportedCommOperationException(bundle.getString(ID_EXCEPTION_UNSUPPORTED_OPERATION),
                        new Throwable(bundle.getString(ID_CAUSE)));
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in enableReceiveTimeout", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks if receive timeout is enabled.
     *
     * <p>An application can determine whether the driver supports this feature
     * by first calling the {@link #enableReceiveTimeout(int)} method and then
     * calling the {@link #isReceiveTimeoutEnabled()} method. If it returns
     * {@code false}, then receive timeout is not supported by the driver.</p>
     *
     * @param port Serial port.
     *
     * @return {@code true} if the receive timeout is enabled, {@code false}
     *         otherwise.
     *
     * @hide
     */
    public boolean isReceiveTimeoutEnabled(String port) {
        try {
            Bundle bundle = service.isReceiveTimeoutEnabled(port);
            checkPortNotOpenException(bundle);
            return bundle.getBoolean(ID_DATA);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in isReceiveTimeoutEnabled", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the integer value of the receive timeout.
     *
     * @param port Serial port.
     *
     * @return Number of milliseconds in receive timeout.
     *
     * @hide
     */
    public int getReceiveTimeout(String port) {
        try {
            Bundle bundle = service.getReceiveTimeout(port);
            checkPortNotOpenException(bundle);
            return bundle.getInt(ID_DATA);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getReceiveTimeout", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Enables receive threshold, if this feature is supported by the driver.
     * When the receive threshold condition becomes true, a {@code read} from
     * the input stream for this port will return immediately.
     *
     * <p>This is an advisory method which the driver may not implement. By
     * default, receive threshold is not enabled.</p>
     *
     * <p>An application can determine whether the driver supports this feature
     * by first calling the {@link #enableReceiveThreshold(int)} method and then
     * calling the {@link #isReceiveThresholdEnabled()} method. If it returns
     * {@code false}, then receive threshold is not supported by the driver.</p>
     *
     * @param port Serial port.
     * @param thresh When this many bytes are in the input buffer, return
     *               immediately from {@code read}.
     *
     * @throws UnsupportedCommOperationException If the operation is not
     *                                           supported by the driver.
     *
     * @hide
     */
    public void enableReceiveThreshold(String port, int thresh) throws UnsupportedCommOperationException {
        try {
            Bundle bundle = service.enableReceiveThreshold(port, thresh);
            checkPortNotOpenException(bundle);
            if (bundle.containsKey(ID_EXCEPTION_UNSUPPORTED_OPERATION))
                throw new UnsupportedCommOperationException(bundle.getString(ID_EXCEPTION_UNSUPPORTED_OPERATION),
                        new Throwable(bundle.getString(ID_CAUSE)));
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in enableReceiveThreshold", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Disables receive threshold.
     *
     * @param port Serial port.
     *
     * @hide
     */
    public void disableReceiveThreshold(String port) {
        try {
            Bundle bundle = service.disableReceiveThreshold(port);
            checkPortNotOpenException(bundle);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in disableReceiveThreshold", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the integer value of the receive threshold.
     *
     * @param port Serial port.
     *
     * @return Number of bytes for receive threshold.
     *
     * @hide
     */
    public int getReceiveThreshold(String port) {
        try {
            Bundle bundle = service.getReceiveThreshold(port);
            checkPortNotOpenException(bundle);
            return bundle.getInt(ID_DATA);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getReceiveThreshold", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks if receive threshold is enabled.
     *
     * <p>An application can determine whether the driver supports this feature
     * by first calling the {@link #enableReceiveThreshold(int)} method and then
     * calling the {@link #isReceiveThresholdEnabled()} method. If it returns
     * {@code false}, then receive threshold is not supported by the driver.</p>
     *
     * @param port Serial port.
     *
     * @return {@code true} if the receive threshold is enabled, {@code false}
     *         otherwise.
     *
     * @hide
     */
    public boolean isReceiveThresholdEnabled(String port) {
        try {
            Bundle bundle = service.isReceiveThresholdEnabled(port);
            checkPortNotOpenException(bundle);
            return bundle.getBoolean(ID_DATA);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in isReceiveThresholdEnabled", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the input buffer size.
     *
     * @param port Serial port.
     * @param size Size of the input buffer in bytes.
     *
     * @hide
     */
    public void setInputBufferSize(String port, int size) {
        try {
            Bundle bundle = service.setInputBufferSize(port, size);
            checkPortNotOpenException(bundle);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in setInputBufferSize", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the input buffer size.
     *
     * @param port Serial port.
     *
     * @return Input buffer size currently in use in bytes.
     *
     * @hide
     */
    public int getInputBufferSize(String port) {
        try {
            Bundle bundle = service.getInputBufferSize(port);
            checkPortNotOpenException(bundle);
            return bundle.getInt(ID_DATA);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getInputBufferSize", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the output buffer size.
     *
     * @param port Serial port.
     * @param size Size of the output buffer in bytes.
     *
     * @hide
     */
    public void setOutputBufferSize(String port, int size) {
        try {
            Bundle bundle = service.setOutputBufferSize(port, size);
            checkPortNotOpenException(bundle);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in setOutputBufferSize", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the output buffer size.
     *
     * @param port Serial port.
     *
     * @return Output buffer size currently in use in bytes.
     *
     * @hide
     */
    public int getOutputBufferSize(String port) {
        try {
            Bundle bundle = service.getOutputBufferSize(port);
            checkPortNotOpenException(bundle);
            return bundle.getInt(ID_DATA);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getOutputBufferSize", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks whether bundle contains a port not open exception or not.
     *
     * @param bundle The bundle to check.
     */
    private void checkPortNotOpenException(Bundle bundle) {
        if (bundle != null && bundle.containsKey(ID_EXCEPTION_PORT_NOT_OPEN)) {
            Log.e(TAG, bundle.getString(ID_EXCEPTION_PORT_NOT_OPEN));
            throw new RuntimeException(bundle.getString(ID_EXCEPTION_PORT_NOT_OPEN));
        }
    }

    /**
     * Wraps the given Serial Port Listener in a Listener Transport Object.
     *
     * @param port The Serial Port.
     * @param listener Serial Port Listener.
     *
     * @return The Listener Transport object containing the Serial Port listener object.
     */
    private ListenerTransport wrapListener(String port, SerialPortListenerImpl listener) {
        // Sanity checks.
        if (listener == null)
            return null;

        // Create listener wrapper and add it to the list.
        synchronized (listeners) {
            ListenerTransport transport = listeners.get(listener);
            if (transport == null)
                transport = new ListenerTransport(port, listener);
            listeners.put(listener, transport);
            return transport;
        }
    }

    /**
     * Helper class used to wrap a Serial Port Listener object.
     */
    private class ListenerTransport extends ISerialPortListener.Stub {

        // Variables.
        private SerialPortListenerImpl mListener;

        private String port;

        /**
         * Class constructor. Instantiates a new Listener Transport object
         * using the given parameters.
         *
         * @param port The serial port.
         * @param listener Serial Port Listener.
         */
        ListenerTransport(String port, SerialPortListenerImpl listener) {
            this.port = port;
            mListener = listener;
        }

        /**
         * Returns the transport associated port.
         *
         * @return The transport associated port.
         */
        public String getPort() {
            return port;
        }

        /**
         * Returns the transport associated listener.
         *
         * @return The transport associated listener.
         */
        public SerialPortListenerImpl getListener() {
            return mListener;
        }

        @Override
        public void serialEvent(String port, int eventType, boolean oldValue, boolean newValue) {
            Message msg = Message.obtain();
            Bundle bundle = new Bundle();
            bundle.putString(ID_PORT, port);
            bundle.putInt(ID_TYPE, eventType);
            bundle.putBoolean(ID_OLD_VALUE, oldValue);
            bundle.putBoolean(ID_NEW_VALUE, newValue);
            msg.what = TYPE_SERIAL_PORT_EVENT;
            msg.setData(bundle);
            mListenerHandler.sendMessage(msg);
        }
    }
}
