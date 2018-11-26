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

package android.can;

import android.os.Bundle;

/** @hide */
interface ICANManager
{
    /* Returns a bundle containing the file descriptor for the CAN. */
    Bundle openInterface(int interfaceNumber);
    
    /* Returns a bundle ontaining any exception thrown during the close process. */
    Bundle closeInterface(int fd);
    
    /* Returns whether it was possible to stop reading from the CAN interface or not. */
    boolean stopReading(int fd);

    /* Returns a bundle containing any exception thrown while reading data. */
    Bundle readData(int fd, in int[] filterIDs, in boolean[] extIds, in int[] masks, inout int[] frameInfo);

    /* Returns a bundle containing any exception thrown while writing data. */
    Bundle writeData(int fd, in int frameID, boolean isExtended, boolean isRTR, in byte[] frameData);

    /* Changes the bitrate of the given CAN interface. */
    Bundle setBitrate(int interfaceNumber, int bitrate);
}
