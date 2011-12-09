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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * HttpRequestLoggerTests
 */
public class DerDecoderTest {

    @Test
    public void testNullByteReturnsNull() {
        byte[] hello = null;

        assertEquals(null, DerDecoder.parseDerUtf8String(hello));
    }

    @Test
    public void testDerDecodeGoodAsciiShortString() {
        byte[] hello = {(byte) 0x04, (byte) 0x05, (byte) 0x68, (byte) 0x65, (byte) 0x6c,
            (byte) 0x6c, (byte) 0x6f};

        assertEquals("hello", DerDecoder.parseDerUtf8String(hello));
    }

    @Test
    public void testDerDecodeGoodShortString() {
        byte[] hello = {(byte) 0x0C, (byte) 0x05, (byte) 0x68, (byte) 0x65, (byte) 0x6c,
            (byte) 0x6c, (byte) 0x6f};

        assertEquals("hello", DerDecoder.parseDerUtf8String(hello));
    }

    @Test
    public void testDerDecodeGoodLongString() {
        byte[] hello = new byte[131];
        hello[0] = (byte) 0x0C;
        hello[1] = (byte) 0x81;
        hello[2] = (byte) 0x80;

        String expected = "";
        for (int i = 3; i < hello.length; i++) {
            expected += "o";
            hello[i] = 0x6f;
        }

        assertEquals(expected, DerDecoder.parseDerUtf8String(hello));
    }

    @Test
    public void testDerDecodeUnknownType() {
        byte[] hello = {(byte) 0x0a, (byte) 0x05, (byte) 0x68, (byte) 0x65, (byte) 0x6c,
            (byte) 0x6c, (byte) 0x6f};

        assertEquals(null, DerDecoder.parseDerUtf8String(hello));
    }

    @Test
    public void testDerDecodeBadLength() {
        byte[] hello = {(byte) 0x0a, (byte) 0x05, (byte) 0x68, (byte) 0x65, (byte) 0x6c,
            (byte) 0x6c};

        assertEquals(null, DerDecoder.parseDerUtf8String(hello));
    }

    @Test
    public void testDerDecodeBadLengthLongForm() {
        byte[] hello = new byte[131];
        hello[0] = (byte) 0x0C;
        hello[1] = (byte) 0x81;
        hello[2] = (byte) 0xFF;

        String expected = "";
        for (int i = 3; i < hello.length; i++) {
            expected += "o";
            hello[i] = 0x6f;
        }

        assertEquals(null, DerDecoder.parseDerUtf8String(hello));
    }
}
