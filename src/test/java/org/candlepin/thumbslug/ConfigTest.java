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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.candlepin.thumbslug.model.CdnInfo;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Properties;

/**
 * ConfigTest
 */
public class ConfigTest {

    //Config(null) to not read the default config file
    @Test(expected = RuntimeException.class)
    @Ignore
    public void testRuntimeExceptionOnBadConfigLookup() {
        Config config = new Config(null);
        config.getBoolean("foofoofoofoo");
    }

    @Test
    @Ignore
    public void testConfigGetString() {
        Config config = new Config(null);
        String result = config.getProperty("log.access");
        assertEquals(result, "/var/log/thumbslug/access.log");
    }

    @Ignore
    @Test
    public void testConfigGetLogging() {
        Config config = new Config(null);
        Properties props = config.getLoggingConfig();
        // No logging in default config
        assertTrue(props.isEmpty());
    }

    @Test
    @Ignore
    public void testConfigGetNamespaceProperties() {
        Config config = new Config(null);
        Properties props = config.getNamespaceProperties("");
        assertFalse(props.isEmpty());

        Properties noProps = config.getNamespaceProperties("this.does.not.exist");
        assertTrue(noProps.isEmpty());

        Properties sslProps = config.getNamespaceProperties("ssl.client");
        assertEquals(2, sslProps.entrySet().size());
    }

    @Test
    public void parseParts() throws Exception {
        FileReader fr = new FileReader("o.json");
        BufferedReader br = new BufferedReader(fr);
        String line = null;
        StringBuffer buf = new StringBuffer();
        while ((line = br.readLine()) != null) {
            buf.append(line);
        }
        br.close();
        fr.close();
        System.out.println(buf.toString());

        ObjectMapper mapper = getObjectMapper();
        CdnInfo realcdn = mapper.readValue(buf.toString(), CdnInfo.class);
        System.out.println(realcdn.getCdnUrl());
        assertEquals("https://cdn.test.com", realcdn.getCdnUrl());
    }

    public ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospector primary = new JacksonAnnotationIntrospector();
        mapper.setAnnotationIntrospector(primary);
        mapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }

}
