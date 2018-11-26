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

package android.firmwareupdate;

/** @hide */
oneway interface IFirmwareUpdateListener
{
    /**
     * Notifies that package verification started.
     */
    void verifyStarted();

    /**
     * Notifies about package verification progress.
     */
    void verifyProgress(int progress);

    /**
     * Notifies that package verification finished.
     */
    void verifyFinished();

    /**
     * Notifies that update process started and device is about to reboot.
     */
    void updateStarted();

    /**
     * Notifies about firmware update error.
     */
    void onError(String error);
}
