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

package android.gpio;

import java.util.HashMap;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This exception indicates that an operation related to GPIO access or 
 * configuration has failed.
 * 
 * <p>The exception stores information of which GPIO caused it and the type of 
 * exception thrown.</p>
 * 
 * @see #UNSPECIFIED_EXCEPTION
 * @see #INVALID_GPIO
 * @see #GPIO_IN_USE
 * @see #READING_ERROR
 * @see #WRITING_ERROR
 * @see #INTERRUPT_TIMEOUT
 * 
 * @hide
 */
public class GPIOExceptionImpl extends Exception implements Parcelable {
	
	// Constants.
	public static final long serialVersionUID = -1;
	
	/** Any exception that does not specify a specific issue. */
	public static final int UNSPECIFIED_EXCEPTION = 0;
	/** The provided GPIO number is not valid. */
	public static final int INVALID_GPIO = 1;
	/** Timeout while waiting for interrupt event on the GPIO. */
	public static final int GPIO_IN_USE = 2;
	/** Error reading from GPIO files. */
	public static final int READING_ERROR = 3;
	/** Error writing to GPIO files. */
	public static final int WRITING_ERROR = 4;
	/** Timeout while waiting for interrupt event on the GPIO. */
	public static final int INTERRUPT_TIMEOUT = 5;
	
	public static final Parcelable.Creator<GPIOExceptionImpl> CREATOR = 
			new Parcelable.Creator<GPIOExceptionImpl>() {
		@Override
		public GPIOExceptionImpl createFromParcel(Parcel in) {
			int gpioNumber = in.readInt();
			int exceptionType = in.readInt();
			String message = in.readString();
			if (message == null)
				return new GPIOExceptionImpl(gpioNumber, exceptionType);
			else
				return new GPIOExceptionImpl(gpioNumber, exceptionType, message);
		}
		
		@Override
		public GPIOExceptionImpl[] newArray(int size) {
			return new GPIOExceptionImpl[size];
		}
	};
	
	// Standard messages.
	private static final String UNSPECIFIED_EXCEPTION_MESSAGE = "Not specific error cause.";
	private static final String INVALID_GPIO_MESSAGE = "The provided GPIO number is not valid.";
	private static final String GPIO_IN_USE_MESSAGE = "Timeout while waiting for interrupt event on the GPIO.";
	private static final String READING_ERROR_MESSAGE = "Error reading from GPIO files.";
	private static final String WRITING_ERROR_MESSAGE = "Error writing to GPIO files.";
	private static final String INTERRUPT_TIMEOUT_MESSAGE = "Timeout while waiting for interrupt event on the GPIO.";
	
	private static HashMap<Integer, String> messagesTable = new HashMap<Integer, String>();
	
	static {
		messagesTable.put(UNSPECIFIED_EXCEPTION, UNSPECIFIED_EXCEPTION_MESSAGE);
		messagesTable.put(INVALID_GPIO, INVALID_GPIO_MESSAGE);
		messagesTable.put(GPIO_IN_USE, GPIO_IN_USE_MESSAGE);
		messagesTable.put(READING_ERROR, READING_ERROR_MESSAGE);
		messagesTable.put(WRITING_ERROR, WRITING_ERROR_MESSAGE);
		messagesTable.put(INTERRUPT_TIMEOUT, INTERRUPT_TIMEOUT_MESSAGE);
	}
	
	// Variables.
	protected int gpioNumber;
	protected int exceptionType;
	
	/**
	 * Constructs a GPIOExceptionImpl for the given GPIO number.
	 * 
	 * @param gpioNumber Number of GPIO that threw the exception.
	 */
	public GPIOExceptionImpl(int gpioNumber) {
		this(gpioNumber, UNSPECIFIED_EXCEPTION);
	}
	
	/**
	 * Constructs a GPIOExceptionImpl for the given GPIO number and with the 
	 * given cause.
	 * 
	 * @param gpioNumber Number of GPIO that threw the exception.
	 * @param exceptionType Type of exception.
	 * 
	 * @see #UNSPECIFIED_EXCEPTION
	 * @see #INVALID_GPIO
	 * @see #GPIO_IN_USE
	 * @see #READING_ERROR
	 * @see #WRITING_ERROR
	 * @see #INTERRUPT_TIMEOUT
	 */
	public GPIOExceptionImpl(int gpioNumber, int exceptionType) {
		this(gpioNumber, exceptionType, null, null);
	}
	
	/**
	 * Constructs a GPIOExceptionImpl for the given GPIO number, exception cause 
	 * and detailed exception message.
	 * 
	 * @param gpioNumber Number of GPIO that threw the exception.
	 * @param exceptionType Type of exception.
	 * @param message Additional message for the exception.
	 * 
	 * @see #UNSPECIFIED_EXCEPTION
	 * @see #INVALID_GPIO
	 * @see #GPIO_IN_USE
	 * @see #READING_ERROR
	 * @see #WRITING_ERROR
	 * @see #INTERRUPT_TIMEOUT
	 */
	public GPIOExceptionImpl(int gpioNumber, int exceptionType, String message) {
		this(gpioNumber, exceptionType, message, null);
	}
	
	/**
	 * Constructs a GPIOExceptionImpl for the given GPIO number, exception cause
	 * and parent exception.
	 * 
	 * @param gpioNumber Number of GPIO that threw the exception.
	 * @param exceptionType Type of exception.
	 * @param throwable Parent exception.
	 * 
	 * @see #UNSPECIFIED_EXCEPTION
	 * @see #INVALID_GPIO
	 * @see #GPIO_IN_USE
	 * @see #READING_ERROR
	 * @see #WRITING_ERROR
	 * @see #INTERRUPT_TIMEOUT
	 */
	public GPIOExceptionImpl(int gpioNumber, int exceptionType, Throwable throwable) {
		this(gpioNumber, exceptionType, null, throwable);
	}
	
	/**
	 * Constructs a GPIOExceptionImpl for the given GPIO number, exception 
	 * cause, detailed message and parent exception.
	 * 
	 * @param gpioNumber Number of GPIO that threw the exception.
	 * @param exceptionType Type of exception.
	 * @param message Additional message for the exception.
	 * @param throwable Parent exception.
	 * 
	 * @see #UNSPECIFIED_EXCEPTION
	 * @see #INVALID_GPIO
	 * @see #GPIO_IN_USE
	 * @see #READING_ERROR
	 * @see #WRITING_ERROR
	 * @see #INTERRUPT_TIMEOUT
	 */
	public GPIOExceptionImpl(int gpioNumber, int exceptionType, String message, Throwable throwable) {
		super(message, throwable);
		this.gpioNumber = gpioNumber;
		this.exceptionType = exceptionType;
	}
	
	/**
	 * Retrieves the GPIO kernel number associated to this exception.
	 * 
	 * @return GPIO kernel number associated with this exception.
	 */
	public int getGPIONumber() {
		return gpioNumber;
	}
	
	/**
	 * Retrieves the exception type. Will be {@link #UNSPECIFIED_EXCEPTION} if 
	 * not explicitly set.
	 * 
	 * @return The exception type.
	 * 
	 * @see #UNSPECIFIED_EXCEPTION
	 * @see #INVALID_GPIO
	 * @see #GPIO_IN_USE
	 * @see #READING_ERROR
	 * @see #WRITING_ERROR
	 * @see #INTERRUPT_TIMEOUT
	 */
	public int getType() {
		return exceptionType;
	}
	
	/**
	 * Retrieves a detailed message of the exception.
	 * 
	 * <p>Message is built using the GPIO number and cause. If additional 
	 * detailed message was provided on construction time, it will be used too 
	 * to build the final message.</p>
	 * 
	 * @return Detailed exception message.
	 */
	@Override
	public String getMessage() {
		String storedMessage = super.getMessage();
		String typeMessage = messagesTable.get(exceptionType);
		String message = "GPIO " + gpioNumber + " > " 
				+ typeMessage == null ? UNSPECIFIED_EXCEPTION_MESSAGE : typeMessage;
		
		if (storedMessage != null)
			message = message + " > " + storedMessage;
		return message;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(gpioNumber);
		dest.writeInt(exceptionType);
		dest.writeString(super.getMessage());
	}
}
