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
 * receive Device Requests from the Cloud Connector service.
 *
 * @hide
 */
public interface DeviceRequestListenerImpl {

    /**
     * Handles a binary device request for the given target with the given data.
     *
     * @param target Target for which the binary device request has been sent.
     * @param data Data of the binary device request as byte array.
     *
     * @return Request answer after processing the data.
     */
    public String handleDeviceRequest(String target, byte[] data);

    /**
     * Handles a plain device request for the given target with the given data.
     *
     * @param target Target for which the device request has been sent.
     * @param data Data of the device request as plain text.
     *
     * @return Request answer after processing the data.
     */
    public String handleDeviceRequest(String target, String data);
}
