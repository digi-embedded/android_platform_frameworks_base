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

import com.digi.connector.core.DataStream;

/**
 * Wrapper class of DataStream from Cloud Connector that implements parcelable
 * in order to be passed to underlying services.
 *
 * @hide
 */
public class DataStreamImpl implements Parcelable {

    // Variables.
    private DataStream stream;

    /**
     * Class constructor. Instantiates a new {@code DataStreamImpl} object from
     * the given data stream.
     *
     * @param stream Data stream to create the object from.
     */
    DataStreamImpl(DataStream stream) {
        this.stream = stream;
    }

    /**
     * Class constructor. Instantiates a new {@code DataStreamImpl} object from
     * the given parcel.
     *
     * @param in Parcel to read and create the object from.
     */
    DataStreamImpl(Parcel in) {
        readFromParcel(in);
    }

    /**
     * Class constructor. Instantiates a new {@code DataStreamImpl} with the
     * given parameters.
     *
     * @param name Data stream name.
     */
    public DataStreamImpl(String name) {
        stream = new DataStream(name);
    }

    /**
     * Class constructor. Instantiates a new {@code DataStreamImpl} with the
     * given parameters.
     *
     * @param name Data stream name.
     * @param units Data stream units.
     * @param fowardTo List of data streams names to replicate data points to.
     */
    public DataStreamImpl(String name, String units, String[] fowardTo) {
        stream = new DataStream(name, units, fowardTo);
    }

    /**
     * Returns the data stream object from the implementation.
     *
     * @return The data stream object from the implementation.
     */
    public DataStream getDataStream() {
        return stream;
    }

    /**
     * Parcelable creator to build the object from a parcel.
     */
    public static final Parcelable.Creator<DataStreamImpl> CREATOR = new Parcelable.Creator<DataStreamImpl>() {
        public DataStreamImpl createFromParcel(Parcel in) {
            return new DataStreamImpl(in);
        }

        public DataStreamImpl[] newArray(int size) {
            return new DataStreamImpl[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(stream.getName());
        dest.writeString(stream.getUnits());
        dest.writeStringArray(stream.getForwardTo());
    }

    /**
     * Builds the object from the given parcel.
     *
     * @param in Parcel to read and build the object from.
     */
    public void readFromParcel(Parcel in) {
        stream = new DataStream(in.readString());
        stream.setUnits(in.readString());
        stream.setForwardTo(in.readStringArray());
    }
}
