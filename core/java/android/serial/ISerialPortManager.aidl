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

import android.os.Bundle;
import android.serial.ISerialPortListener;

/** @hide */
interface ISerialPortManager
{
    /* Returns the list of available serial ports. */
    String[] listSerialPorts();

    /* Opens the given serial port with the given parameters. */
    Bundle openSerialPort(String port, int baudRate, int dataBits, int stopBits, int parity, int flowControl, int readTimeout);

    /* Closes the given serial port. */
    Bundle close(String port);

    /* Returns the serial port input stream. */
    Bundle getInputStream(String port);

    /* Returns the serial port output stream. */
    Bundle getOutputStream(String port);

    /* Registers the given serial port listener. */
    Bundle addEventListener(String port, in ISerialPortListener listener);

    /* Un-registers the given serial port listener. */
    Bundle removeEventListener(String port);

    /* Notifies a Serial Port Event callback has finished. */
    void serialEventCallbackFinished(String port);

    /* Sets the given serial port parameters. */
    Bundle setPortParameters(String port, int baudRate, int dataBits, int stopBits, int parity, int flowControl);

    /* Returns the configured serial port baud rate. */
    Bundle getBaudRate(String port);

    /* Returns the configured serial port data bits. */
    Bundle getDataBits(String port);

    /* Returns the configured serial port stop bits. */
    Bundle getStopBits(String port);

    /* Returns the configured serial port parity. */
    Bundle getParity(String port);

    /* Returns the configured serial port flow control. */
    Bundle getFlowControl(String port);

    /* Returns the serial port DTR line status. */
    Bundle isDTR(String port);

    /* Sets the serial port DTR line status. */
    Bundle setDTR(String port, boolean state);

    /* Returns the serial port RTS line status. */
    Bundle isRTS(String port);

    /* Sets the serial port RTS line status. */
    Bundle setRTS(String port, boolean state);

    /* Returns the serial port CTS line status. */
    Bundle isCTS(String port);

    /* Returns the serial port DSR line status. */
    Bundle isDSR(String port);

    /* Returns the serial port CD line status. */
    Bundle isCD(String port);

    /* Returns the serial port RI line status. */
    Bundle isRI(String port);

    /* Sends a brak line signal to the serial port. */
    Bundle sendBreak(String port, int duration);

    /* Configures the serial port data available notification. */
    Bundle notifyOnDataAvailable(String port, boolean enable);

    /* Configures the serial port output empty notification. */
    Bundle notifyOnOutputEmpty(String port, boolean enable);

    /* Configures the serial port CTS notification. */
    Bundle notifyOnCTS(String port, boolean enable);

    /* Configures the serial port DSR notification. */
    Bundle notifyOnDSR(String port, boolean enable);

    /* Configures the serial port Ring indicator notification. */
    Bundle notifyOnRingIndicator(String port, boolean enable);

    /* Configures the serial port carrier detect notification. */
    Bundle notifyOnCarrierDetect(String port, boolean enable);

    /* Configures the serial port overrun error notification. */
    Bundle notifyOnOverrunError(String port, boolean enable);

    /* Configures the serial port parity error notification. */
    Bundle notifyOnParityError(String port, boolean enable);

    /* Configures the serial port framing error notification. */
    Bundle notifyOnFramingError(String port, boolean enable);

    /* Configures the serial port break interrupt notification. */
    Bundle notifyOnBreakInterrupt(String port, boolean enable);

    /* Returns the serial port parity error character. */
    Bundle getParityErrorChar(String port);

    /* Sets the serial port parity error character. */
    Bundle setParityErrorChar(String port, byte b);

    /* Returns the serial port end of input character. */
    Bundle getEndOfInputChar(String port);

    /* Sets the serial port end of input character. */
    Bundle setEndOfInputChar(String port, byte b);

    /* Sets the serial port baud base. */
    Bundle setBaudBase(String port, int baudBase);

    /* Returns the serial port baud base. */
    Bundle getBaudBase(String port);

    /* Sets the serial port divisor. */
    Bundle setDivisor(String port, int divisor);

    /* Returns the serial port divisor. */
    Bundle getDivisor(String port);

    /* Disables the serial port receive timeout. */
    Bundle disableReceiveTimeout(String port);

    /* Enables the serial port receive timeout. */
    Bundle enableReceiveTimeout(String port, int time);

    /* Returns the serial port receive timeout status. */
    Bundle isReceiveTimeoutEnabled(String port);

    /* Returns the serial port receive timeout. */
    Bundle getReceiveTimeout(String port);

    /* Enables the serial port receive threshold. */
    Bundle enableReceiveThreshold(String port, int thresh);

    /* Disables the serial port receive threshold. */
    Bundle disableReceiveThreshold(String port);

    /* Returns the serial port receive threshold. */
    Bundle getReceiveThreshold(String port);

    /* Returns the serial port receive threshold status. */
    Bundle isReceiveThresholdEnabled(String port);

    /* Sets the serial port input buffer size. */
    Bundle setInputBufferSize(String port, int size);

    /* Returns the serial port input buffer size. */
    Bundle getInputBufferSize(String port);

    /* Sets the serial port output buffer size. */
    Bundle setOutputBufferSize(String port, int size);

    /* Returns the serial port output buffer size. */
    Bundle getOutputBufferSize(String port);
}
