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

/**
 * This interface defines the required methods that should be implemented to
 * receive events from the Cloud Connector service.
 *
 * @hide
 */
public interface CloudConnectorEventListenerImpl {

    /**
     * Notifies that the Cloud Connector service has connected.
     */
    public void connected();

    /**
     * Notifies that the Cloud Connector service has disconnected.
     */
    public void disconnected();

    /**
     * Notifies that there was an error connecting.
     *
     * @param errorMessage The connection error description.
     */
    public void connectionError(String errorMessage);

    /**
     * Notifies that the data points have been sent successfully.
     */
    public void sendDataPointsSuccess();

    /**
     * Notifies that there was an error sending the data points.
     *
     * @param errorMessage The error message.
     */
    public void sendDataPointsError(String errorMessage);
}
