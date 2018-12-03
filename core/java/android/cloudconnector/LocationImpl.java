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

import com.digi.connector.core.system.Location;

/**
 * Wrapper class of Location from Cloud Connector that implements
 * parcelable in order to be passed to underlying services.
 *
 * @hide
 */
public class LocationImpl implements Parcelable {

    // Variables.
    private Location location;

    /**
     * Class constructor. Instantiates a new {@code LocationImpl} object from
     * the given location object.
     *
     * @param location Location object to create LocationImpl from.
     */
    LocationImpl(Location location) {
        this.location = location;
    }

    /**
     * Class constructor. Instantiates a new {@code LocationImpl} object from
     * the given parcel.
     *
     * @param in Parcel to read and create the object from.
     */
    public LocationImpl(Parcel in) {
        readFromParcel(in);
    }

    /**
     * Class constructor. Instantiates a new LocationImpl object with the given
     * latitude and longitude (the elevation is considered {@code 0}).
     *
     * @param latitude Location latitude.
     * @param longitude Location longitude.
     *
     * @throws IllegalArgumentException if {@code latitude == null}
     *                                  or if {@code longitude == null}
     *                                  or if any of the provided values is an
     *                                  invalid value.
     */
    public LocationImpl(String latitude, String longitude) {
        location = new Location(latitude, longitude);
    }

    /**
     * Class constructor. Instantiates a new LocationImpl object with the given
     * latitude, longitude and altitude.
     *
     * @param latitude Location latitude.
     * @param longitude Location longitude.
     * @param elevation Location elevation.
     *
     * @throws IllegalArgumentException if {@code latitude == null}
     *                                  or if {@code longitude == null}
     *                                  or if {@code elevation == null}
     *                                  or if any of the provided values is an
     *                                  invalid value.
     */
    public LocationImpl(String latitude, String longitude, String elevation) {
        location = new Location(latitude, longitude, elevation);
    }

    /**
     * Returns the location object from the implementation.
     *
     * @return The location object from the implementation.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Parcelable creator to build the object from a parcel.
     */
    public static final Parcelable.Creator<LocationImpl> CREATOR = new Parcelable.Creator<LocationImpl>() {
        public LocationImpl createFromParcel(Parcel in) {
            return new LocationImpl(in);
        }

        public LocationImpl[] newArray(int size) {
            return new LocationImpl[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(location.getLatitude());
        dest.writeString(location.getLongitude());
        dest.writeString(location.getElevation());
        if (location.isCurrent())
            dest.writeInt(1);
        else
            dest.writeInt(0);
    }

    /**
     * Builds the object from the given parcel.
     *
     * @param in Parcel to read and build the object from.
     */
    public void readFromParcel(Parcel in) {
        location = new Location(in.readString(), in.readString(), in.readString());
        int isCurrentValue = in.readInt();
        if (isCurrentValue == 1)
            location.setCurrent(true);
        else
            location.setCurrent(false);
    }
}
