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
import android.util.Base64;

import com.digi.connector.core.DataPoint;

/**
 * Wrapper class of DataPoint from Cloud Connector that implements parcelable
 * in order to be passed to underlying services.
 *
 * @hide
 */
public class DataPointImpl implements Parcelable {

    // Variables.
    private DataPoint dataPoint;

    /**
     * Class constructor. Instantiates a new {@code DataPointImpl} object from
     * the given parcel.
     *
     * @param in Parcel to read and create the object from.
     */
    DataPointImpl(Parcel in) {
        readFromParcel(in);
    }

    /**
     * Class constructor. Instantiates a new DataPointImpl of type integer with
     * the given data value.
     *
     * @param data Data point value.
     * @param stream Data stream destination of this data point.
     *
     * @throws IllegalArgumentException if {@code stream == null}.
     */
    public DataPointImpl(int data, DataStreamImpl stream) {
        dataPoint = new DataPoint(data, stream.getDataStream());
    }

    /**
     * Class constructor. Instantiates a new DataPointImpl of type long with
     * the given data value.
     *
     * @param data Data point value.
     * @param stream Data stream destination of this data point.
     *
     * @throws IllegalArgumentException if {@code stream == null}.
     */
    public DataPointImpl(long data, DataStreamImpl stream) {
        dataPoint = new DataPoint(data, stream.getDataStream());
    }

    /**
     * Class constructor. Instantiates a new DataPointImpl of type float with
     * the given data value.
     *
     * @param data Data point value.
     * @param stream Data stream destination of this data point.
     *
     * @throws IllegalArgumentException if {@code stream == null}.
     */
    public DataPointImpl(float data, DataStreamImpl stream) {
        dataPoint = new DataPoint(data, stream.getDataStream());
    }

    /**
     * Class constructor. Instantiates a new DataPointImpl of type double with
     * the given data value.
     *
     * @param data Data point value.
     * @param stream Data stream destination of this data point.
     *
     * @throws IllegalArgumentException if {@code stream == null}.
     */
    public DataPointImpl(double data, DataStreamImpl stream) {
        dataPoint = new DataPoint(data, stream.getDataStream());
    }

    /**
     * Class constructor. Instantiates a new DataPointImpl of type string with
     * the given data value.
     *
     * @param data Data point value.
     * @param stream Data stream destination of this data point.
     *
     * @throws IllegalArgumentException if {@code data == null} or
     *                                  if {@code stream == null}.
     */
    public DataPointImpl(String data, DataStreamImpl stream) {
        dataPoint = new DataPoint(data, stream.getDataStream());
    }

    /**
     * Class constructor. Instantiates a new DataPointImpl of type binary with
     * the given data value.
     *
     * @param data Data point value.
     * @param stream Data stream destination of this data point.
     *
     * @throws IllegalArgumentException if {@code data == null} or
     *                                  if {@code stream == null}.
     */
    public DataPointImpl(byte[] data, DataStreamImpl stream) {
        dataPoint = new DataPoint(data, stream.getDataStream());
    }

    /**
     * Sets the data point description.
     *
     * @param description Data point description.
     */
    public void setDescription(String description) {
        dataPoint.setDescription(description);
    }

    /**
     * Sets the data point quality.
     *
     * @param quality The data point quality.
     */
    public void setQuality(Integer quality) {
        dataPoint.setQuality(quality);
    }

    /**
     * Sets the data point time stamp.
     *
     * @param timestamp The data point time stamp.
     */
    public void setTimestamp(String timestamp) {
        dataPoint.setTimestamp(timestamp);
    }

    /**
     * Sets the data point location using a LocationImpl object.
     *
     * @param location The LocationImpl object to use as location.
     */
    public void setLocation(LocationImpl location) {
        if (location != null)
            dataPoint.setLocation(location.getLocation());
    }

    /**
     * Returns the data point associated with this implementation.
     *
     * @return The data point associated to this implementation.
     */
    public DataPoint getDataPoint() {
        return dataPoint;
    }

    /**
     * Parcelable creator to build the object from a parcel.
     */
    public static final Parcelable.Creator<DataPointImpl> CREATOR = new Parcelable.Creator<DataPointImpl>() {
        public DataPointImpl createFromParcel(Parcel in) {
            return new DataPointImpl(in);
        }

        public DataPointImpl[] newArray(int size) {
            return new DataPointImpl[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(dataPoint.getData());
        dest.writeParcelable(new DataStreamImpl(dataPoint.getDataStream()), flags);
        dest.writeInt(dataPoint.getType());
        dest.writeString(dataPoint.getDescription());
        dest.writeString(dataPoint.getTimestamp());
        dest.writeInt(dataPoint.getQuality());
        if (dataPoint.getLocation() != null) {
            dest.writeInt(1);
            dest.writeParcelable(new LocationImpl(dataPoint.getLocation()), flags);
        } else
            dest.writeInt(0);
    }

    /**
     * Builds the object from the given parcel.
     *
     * @param in Parcel to read and build the object from.
     */
    public void readFromParcel(Parcel in) {
        String data = in.readString();
        DataStreamImpl streamImpl = in.readParcelable(DataStreamImpl.class.getClassLoader());
        int type = in.readInt();
        switch (type) {
        case 0: // Integer
            dataPoint = new DataPoint(Integer.valueOf(data), streamImpl.getDataStream());
            break;
        case 1: // Long
            dataPoint = new DataPoint(Long.valueOf(data), streamImpl.getDataStream());
            break;
        case 2: // Float
            dataPoint = new DataPoint(Float.valueOf(data), streamImpl.getDataStream());
            break;
        case 3: // Double
            dataPoint = new DataPoint(Double.valueOf(data), streamImpl.getDataStream());
            break;
        case 4: // String
            dataPoint = new DataPoint(data, streamImpl.getDataStream());
            break;
        case 5: // Byte[]
            dataPoint = new DataPoint(Base64.decode(data, Base64.DEFAULT), streamImpl.getDataStream());
            break;
        }
        dataPoint.setDescription(in.readString());
        dataPoint.setTimestamp(in.readString());
        dataPoint.setQuality(in.readInt());
        // Check if there is location.
        int locationExist = in.readInt();
        if (locationExist == 1) {
            LocationImpl locationImpl = in.readParcelable(LocationImpl.class.getClassLoader());
            dataPoint.setLocation(locationImpl.getLocation());
        }
    }
}
