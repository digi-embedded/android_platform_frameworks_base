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

package android.i2c;

import android.os.Bundle;

/** @hide */
interface II2CManager
{
    /* Returns a bundle containing the file descriptor for the I2C. */
    Bundle openI2C(int interfaceNumber);
    
    /* Returns a bundle containing any exception thrown during the close process. */
    Bundle closeI2C(int fd);
    
    /* Returns a list of all available I2C interfaces. */
    int[] listInterfaces();

    /* Returns a bundle containing any exception thrown while reading data. */
    Bundle readData(int fd, int numBytes);

    /* Returns a bundle containing any exception thrown while writing data. */
    Bundle writeData(int fd, in byte[] txData);

    /* Returns a bundle containing any exception thrown while setting the I2C address. */
    Bundle setAddress(int fd, int address);
}
