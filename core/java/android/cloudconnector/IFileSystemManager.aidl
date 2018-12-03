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

import android.os.Bundle;
import android.os.ParcelFileDescriptor;

/** @hide */
interface IFileSystemManager
{
    boolean isSymbolicLink(String path);
    boolean fileExists(String path);
    boolean canRead(String path);
    boolean canWrite(String path);
    boolean removeFile(String path);
    long getFileSize(String path);
    boolean addDataToFile(String path, in byte[] data, int offset, boolean isLast, boolean truncate);
    boolean createFile(String path);
    byte[] getByteArrayFromFile(String path);
    int readFileChunk(String path, inout byte[] data, int offset, int length, boolean isLast);
    String[] listFiles(String path);
    boolean isDirectory(String path);
    long getLastModified(String path);
    String getName(String path);
    boolean enoughStorageSizeAvailable(String dir, int fileSize);
}
