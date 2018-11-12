/*
 * Copyright (C) 2016-2018 Digi International Inc., All Rights Reserved
 *
 * This software contains proprietary and confidential information of Digi.
 * International Inc. By accepting transfer of this copy, Recipient agrees
 * to retain this software in confidence, to prevent disclosure to others,
 * and to make no use of this software other than that for which it was
 * delivered. This is an unpublished copyrighted work of Digi International
 * Inc. Except as permitted by federal law, 17 USC 117, copying is strictly
 * prohibited.
 *
 * Restricted Rights Legend
 *
 * Use, duplication, or disclosure by the Government is subject to restrictions
 * set forth in sub-paragraph (c)(1)(ii) of The Rights in Technical Data and
 * Computer Software clause at DFARS 252.227-7031 or subparagraphs (c)(1) and
 * (2) of the Commercial Computer Software - Restricted Rights at 48 CFR
 * 52.227-19, as applicable.
 *
 * Digi International Inc. 11001 Bren Road East, Minnetonka, MN 55343
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
