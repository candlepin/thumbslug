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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Config
 */
public class Config {
    private static final Logger log = Logger.getLogger(Config.class.getName());

    private static final String CONFIG_FILE = "/etc/thumbslug/thumbslug.conf";
    private static final String DEFAULT_CONFIG_RESOURCE = "config/thumbslug.conf";
    private Properties props;
    
    private String[] defaultKeys = {"port", "ssl", "cdn.port", "cdn.host", "cdn.ssl", "sendTSheader"};

    public Config() {

        // Load system properties file, otherwise use the default:
        try {
            InputStream is = null;

            File configFile = new File(CONFIG_FILE);
            if (configFile.exists()) {
                is = new FileInputStream(configFile);
            }
            else {
                URL url = this.getClass().getClassLoader().getResource(
                    DEFAULT_CONFIG_RESOURCE);
                is = url.openStream();
            }
            props = new Properties();

            props.load(is);

            is.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        log.info("Config values:");
        for (String key : defaultKeys) {
            log.info(String.format("\t%1$s : %2$s", key, getProperty(key)));
        }
    }

    public String getProperty(String key) {
        // allow for command line/system override of values
        String value = System.getProperty(key);
        if (value != null) {
            return value;
        }
        else {
            return props.getProperty(key);
        }
    }
    
    public int getInt(String key) {
        return Integer.parseInt(getProperty(key));
    }
    
    public boolean getBoolean(String key) {
        String prop = getProperty(key);
        boolean value = false;
        if (prop.toLowerCase().equals("true") || prop.toLowerCase().equals("yes") || prop.equals("1")) {
            value = true;
        }
        
        return value;
    }

}
