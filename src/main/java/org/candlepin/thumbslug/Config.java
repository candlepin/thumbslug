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
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Config
 */
public class Config {
    private static final String REQUIRED = "XX_REQUIRED_VALUE_XX";

    private static final String CONFIG_FILE = "/etc/thumbslug/thumbslug.conf";
    private static final String DEFAULT_CONFIG_RESOURCE = "config/thumbslug.conf";
    private Properties props;
    
    private Set<Object> requiredKeys;

    public Config() {

        // Load system properties file, otherwise use the default:
        try {
            InputStream is = null;

            
            URL url = this.getClass().getClassLoader().getResource(
                DEFAULT_CONFIG_RESOURCE);
            is = url.openStream();

            props = new Properties();

            props.load(is);
            
            // figure out the keys that we need. in the resources config file,
            // they'll be listed but not set.
            requiredKeys = props.keySet();
            

            // override any defaults with the config file
            File configFile = new File(CONFIG_FILE);
            if (configFile.exists()) {
                is = new FileInputStream(configFile);
                props.load(is);
                is.close();
            }

            is.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        Set<String> missingKeys = new HashSet<String>();
        for (Object key : requiredKeys) {
            if (REQUIRED.equals(getProperty((String) key))) {
                missingKeys.add((String) key);
            }
        }
        if (missingKeys.size() > 0) {
            String errorMessage = "Missing config values for:";
            for (String missingKey : missingKeys) {
                errorMessage += "\n  " + missingKey;
            }
            throw new Error(errorMessage);
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
        if (prop.toLowerCase().equals("true") || prop.toLowerCase().equals("yes") ||
            prop.equals("1")) {
            value = true;
        }
        
        return value;
    }

}
