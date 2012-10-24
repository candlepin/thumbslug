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

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * ConfigTest
 */
public class ConfigTest {

    @Test(expected = RuntimeException.class)
    public void testRuntimeExceptionOnBadConfigLookup() {
        Config config = new Config();
        config.getBoolean("foofoofoofoo");
    }

    @Test
    public void testConfigGetString() {
        Config config = new Config();
        String result = config.getProperty("log.access");
        assertEquals(result, "/var/log/thumbslug/access.log");
    }

}
