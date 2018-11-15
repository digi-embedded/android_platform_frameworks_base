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

package android.pwm;

import java.util.HashMap;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This exception indicates that an operation related to PWM access or
 * configuration has failed.
 *
 * <p>The exception stores information of which PWM channel caused it and the
 * type of exception thrown.</p>
 *
 * @see #UNSPECIFIED_EXCEPTION
 * @see #PWM_CHANNEL_NOT_AVAILABLE
 * @see #READING_ERROR
 * @see #WRITING_ERROR
 *
 * @hide
 */
public class PWMExceptionImpl extends Exception implements Parcelable {

	public static final long serialVersionUID = -1;

	/** Any exception that does not specify a specific issue. */
	public static final int UNSPECIFIED_EXCEPTION = 0;
	/** The provided PWM channel index is not available. */
	public static final int PWM_CHANNEL_NOT_AVAILABLE = 1;
	/** Error reading from PWM channel files. */
	public static final int READING_ERROR = 2;
	/** Error writing to PWM channel files. */
	public static final int WRITING_ERROR = 3;

	public static final Parcelable.Creator<PWMExceptionImpl> CREATOR =
			new Parcelable.Creator<PWMExceptionImpl>() {
		@Override
		public PWMExceptionImpl createFromParcel(Parcel in) {
			int channelIndex = in.readInt();
			int exceptionType = in.readInt();
			String message = in.readString();
			if (message == null)
				return new PWMExceptionImpl(channelIndex, exceptionType);
			else
				return new PWMExceptionImpl(channelIndex, exceptionType, message);
		}

		@Override
		public PWMExceptionImpl[] newArray(int size) {
			return new PWMExceptionImpl[size];
		}
	};

	// Standard messages.
	private static final String UNSPECIFIED_EXCEPTION_MESSAGE = "Not specific error cause.";
	private static final String PWM_CHANNEL_NOT_AVAILABLE_MESSAGE = "The provided PWM channel is not available.";
	private static final String READING_ERROR_MESSAGE = "Error reading from PWM channel files.";
	private static final String WRITING_ERROR_MESSAGE = "Error writing to PWM channel files.";

	private static HashMap<Integer, String> messagesTable = new HashMap<Integer, String>();

	static {
		messagesTable.put(UNSPECIFIED_EXCEPTION, UNSPECIFIED_EXCEPTION_MESSAGE);
		messagesTable.put(PWM_CHANNEL_NOT_AVAILABLE, PWM_CHANNEL_NOT_AVAILABLE_MESSAGE);
		messagesTable.put(READING_ERROR, READING_ERROR_MESSAGE);
		messagesTable.put(WRITING_ERROR, WRITING_ERROR_MESSAGE);
	}

	// Variables.
	protected int exceptionType;
	protected int channelIndex;

	/**
	 * Constructs a PWMExceptionImpl for the given PWM channel index.
	 *
	 * @param channelIndex PWM channel index that threw the exception.
	 */
	public PWMExceptionImpl(int channelIndex) {
		this(channelIndex, UNSPECIFIED_EXCEPTION);
	}

	/**
	 * Constructs a PWMExceptionImpl for the given PWM channel index and with
	 * the given cause.
	 *
	 * @param channelIndex PWM channel index that threw the exception.
	 * @param exceptionType Type of exception.
	 *
	 * @see #UNSPECIFIED_EXCEPTION
	 * @see #PWM_CHANNEL_NOT_AVAILABLE
	 * @see #READING_ERROR
	 * @see #WRITING_ERROR
	 */
	public PWMExceptionImpl(int channelIndex, int exceptionType) {
		this(channelIndex, exceptionType, null, null);
	}

	/**
	 * Constructs a PWMExceptionImpl for the given PWM channel index, exception
	 * cause and detailed exception message.
	 *
	 * @param channelIndex PWM channel index that threw the exception.
	 * @param exceptionType Type of exception.
	 * @param message Additional message for the exception.
	 *
	 * @see #UNSPECIFIED_EXCEPTION
	 * @see #PWM_CHANNEL_NOT_AVAILABLE
	 * @see #READING_ERROR
	 * @see #WRITING_ERROR
	 */
	public PWMExceptionImpl(int channelIndex, int exceptionType, String message) {
		this(channelIndex, exceptionType, message, null);
	}

	/**
	 * Constructs a PWMExceptionImpl for the given PWM channel index, exception
	 * cause and parent exception.
	 *
	 * @param channelIndex PWM channel index that threw the exception.
	 * @param exceptionType Type of exception.
	 * @param throwable Parent exception.
	 *
	 * @see #UNSPECIFIED_EXCEPTION
	 * @see #PWM_CHANNEL_NOT_AVAILABLE
	 * @see #READING_ERROR
	 * @see #WRITING_ERROR
	 */
	public PWMExceptionImpl(int channelIndex, int exceptionType, Throwable throwable) {
		this(channelIndex, exceptionType, null, throwable);
	}

	/**
	 * Constructs a PWMExceptionImpl for the given PWM channel index, exception
	 * cause, detailed message and parent exception.
	 *
	 * @param channelIndex PWM channel index that threw the exception.
	 * @param exceptionType Type of exception.
	 * @param message Additional message for the exception.
	 * @param throwable Parent exception.
	 *
	 * @see #UNSPECIFIED_EXCEPTION
	 * @see #PWM_CHANNEL_NOT_AVAILABLE
	 * @see #READING_ERROR
	 * @see #WRITING_ERROR
	 */
	public PWMExceptionImpl(int channelIndex, int exceptionType, String message, Throwable throwable) {
		super(message, throwable);
		this.channelIndex = channelIndex;
		this.exceptionType = exceptionType;
	}

	/**
	 * Retrieves the exception type. Will be {@link #UNSPECIFIED_EXCEPTION}
	 * if not explicitly set.
	 *
	 * @return Returns the exception type.
	 *
	 * @see #UNSPECIFIED_EXCEPTION
	 * @see #PWM_CHANNEL_NOT_AVAILABLE
	 * @see #READING_ERROR
	 * @see #WRITING_ERROR
	 */
	public int getType() {
		return exceptionType;
	}

	/**
	 * Retrieves the PWM channel index associated to this exception.
	 *
	 * @return PWM channel index associated with this exception.
	 */
	public int getPWMChannelIndex() {
		return channelIndex;
	}

	/**
	 * Retrieves a detailed message of the exception.
	 *
	 * <p>Message is built using the PWM channel index and cause. If additional
	 * detailed message was provided on construction time, it will be used
	 * too to build the final message.</p>
	 *
	 * @return Detailed exception message.
	 */
	@Override
	public String getMessage() {
		String storedMessage = super.getMessage();
		String typeMessage = messagesTable.get(exceptionType);
		String message = "PWM channel " + channelIndex + " > "
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
		dest.writeInt(channelIndex);
		dest.writeInt(exceptionType);
		dest.writeString(super.getMessage());
	}
}
