/**
 * Copyright (c) 2011 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.thumbslug;

import java.math.BigInteger;

import org.apache.log4j.Logger;

/**
 * DerDecoder
 */
public class DerDecoder {
    private static Logger log = Logger.getLogger(DerDecoder.class);

    private DerDecoder() {

    }

    public static String parseDerUtf8String(byte[] encoded) {
        if (encoded == null) {
            return null;
        }
        // Verify it's a utf 8 or basic string encoded value, just in case.
        if ((encoded[0] & 0x1F) != 0x0C && (encoded[0] & 0x1F) != 0x04) {
            return null;
        }

        int length;
        int offset;
        // decode how many bytes we have for the length of this value
        if ((encoded[1] & ~0x7F) == 0) {
            length = encoded[1];
            offset = 2;
        }
        else {
            int octets = encoded[1] & 0x7F;
            byte[] lengthBytes = new byte[octets];
            System.arraycopy(encoded, 2, lengthBytes, 0, octets);

            length = new BigInteger(1, lengthBytes).intValue();
            offset = 2 + octets;
        }

        try {
            return new String(encoded, offset, length, "UTF-8");
        }
        catch (Exception e) {
            log.error("Unable to decode DER string", e);
            // bad encoding, improper length, etc
            return null;
        }
    }
}
