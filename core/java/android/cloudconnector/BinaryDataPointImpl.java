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

import android.os.Parcel;
import android.os.Parcelable;

import com.digi.connector.core.BinaryDataPoint;
import com.digi.connector.core.DataStream;

/**
 * Wrapper class of BinaryDataPoint from Cloud Connector that implements
 * parcelable in order to be passed to underlying services.
 *
 * @hide
 */
public class BinaryDataPointImpl implements Parcelable {

    // Variable.
    private BinaryDataPoint dataPoint;

    /**
     * Class constructor. Instantiates a new {@code BinaryDataPoint} object from
     * the given parcel.
     *
     * @param in Parcel to read and create the object from.
     */
    BinaryDataPointImpl(Parcel in) {
        readFromParcel(in);
    }

    /**
     * Class constructor. Instantiates a new binary data point with the given
     * parameters.
     *
     * @param data Binary data point value.
     * @param stream Data stream destination of this binary data point.
     *
     * @throws IllegalArgumentException
     *             if {@code data == null} or
     *             if {@code stream == null}.
     */
    public BinaryDataPointImpl(byte[] data, DataStreamImpl stream) {
        dataPoint = new BinaryDataPoint(data, stream.getDataStream());
    }

    /**
     * Returns the binary data point associated with this implementation.
     *
     * @return The binary data point associated to this implementation.
     */
    public BinaryDataPoint getBinaryDataPoint() {
        return dataPoint;
    }

    /**
     * Parcelable creator to build the object from a parcel.
     */
    public static final Parcelable.Creator<BinaryDataPointImpl> CREATOR = new Parcelable.Creator<BinaryDataPointImpl>() {
        public BinaryDataPointImpl createFromParcel(Parcel in) {
            return new BinaryDataPointImpl(in);
        }

        public BinaryDataPointImpl[] newArray(int size) {
            return new BinaryDataPointImpl[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(dataPoint.getData().length);
        dest.writeByteArray(dataPoint.getData());
        dest.writeParcelable(new DataStreamImpl(dataPoint.getDataStream()), flags);
    }

    /**
     * Builds the object from the given parcel.
     *
     * @param in Parcel to read and build the object from.
     */
    public void readFromParcel(Parcel in) {
        byte[] data = new byte[in.readInt()];
        in.readByteArray(data);
        DataStreamImpl streamImpl = in.readParcelable(DataStreamImpl.class.getClassLoader());
        dataPoint = new BinaryDataPoint(data, streamImpl.getDataStream());
    }
}
