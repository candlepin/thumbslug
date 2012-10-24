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

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

/**
 * Config
 */
public class Config {
    private static final String REQUIRED = "XX_REQUIRED_VALUE_XX";
    private static final String CONFIG_FILE = "/etc/thumbslug/thumbslug.conf";
    private static final String DEFAULT_CONFIG_RESOURCE = "config/thumbslug.conf";
    private static final String LOGGER_PREFIX = "log4j.logger.";

    private Set<Object> requiredKeys;

    // NavigableMap based for finding subMap of config
    protected TreeMap<String, String> configuration = null;

    private static Logger log = Logger.getLogger(Config.class);


    public Config() {

        Properties props;
        // load default properties, and then attempt to overwrite
        // with system properties
        InputStream is = null;
        props = new Properties();
        try {
            URL url = this.getClass().getClassLoader().getResource(
                DEFAULT_CONFIG_RESOURCE);
            is = url.openStream();
            props.load(is);
        }
        catch (IOException e) {
            // we need to bail out if we can't read the default props
            throw new RuntimeException(e);
        }
        finally {
            try {
                is.close();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // figure out the keys that we need. in the resources config file,
        // they'll be listed but not set.
        requiredKeys = props.keySet();

        // override any defaults with the config file
        // and include any new props
        try {
            File configFile = new File(CONFIG_FILE);
            is = new FileInputStream(configFile);
            props.load(is);
        }
        catch (IOException e) {
            // no need to bail out here, just log it
            log.warn("Could not read " + CONFIG_FILE, e);
        }
        finally {
            try {
                is.close();
            }
            catch (IOException e) {
                // no need to bail out here, just log it
                log.warn("Could not close handle for " + CONFIG_FILE, e);
            }
        }

        Set<String> missingKeys = new HashSet<String>();
        for (Object key : requiredKeys) {
            if (REQUIRED.equals(System.getProperty((String) key))) {
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

        // Convert the loaded Properties to a TreeMap so we
        // can do a submap to find entries for a given prefix
        configuration = propsToConfiguration(props);

        // add system properties, so we can find them when we
        // search for logging properties, etc, override default
        // and config
        configuration.putAll(propsToConfiguration(System.getProperties()));
    }

    @SuppressWarnings("unchecked")
    private TreeMap<String,String> propsToConfiguration(Properties loadedProps)
    {
        TreeMap<String,String> configMap;
        configMap = new TreeMap<String, String>();
        configMap.putAll((Map) loadedProps);
        return configMap;
    }

    public String getProperty(String key) {
        // allow for command line/system override of values
        String value = System.getProperty(key);
        if (value != null) {
            return value;
        }
        else {
            return configuration.get(key);
        }
    }

    public int getInt(String key) {
        return Integer.parseInt(getProperty(key));
    }

    public boolean getBoolean(String key) {
        String prop = getProperty(key);
        if (prop == null) {
            throw new RuntimeException("unable to find value for config property " + key);
        }
        boolean value = false;
        if (prop.toLowerCase().equals("true") || prop.toLowerCase().equals("yes") ||
            prop.equals("1")) {
            value = true;
        }

        return value;
    }

    /**
     * Return configuration entry for the given prefix.
     *
     * @param prefix prefix for the entry sought.
     * @return configuration entry for the given prefix.
     */
    public Map<String, String> configurationWithPrefix(String prefix) {
        return configuration.subMap(prefix, prefix + Character.MAX_VALUE);
    }

    /**
     * Return configuration entry for the given prefix.
     *
     * @param prefix prefix for the entry sought.
     * @return configuration entry for the given prefix.
     */
    public Properties getNamespaceProperties(String prefix) {
        Properties p = new Properties();

        p.putAll(configurationWithPrefix(prefix));
        return p;
    }


    /**
     * Return the log4j config properties
     * @return log4j related config properties
     *
     */
    public Properties getLoggingConfig() {
        Properties loggingProperties = getNamespaceProperties(LOGGER_PREFIX);
        Properties p = new Properties();
        for (Entry<Object, Object> entry : loggingProperties.entrySet()) {
            String loggingKey = (String) entry.getKey();
            String key = loggingKey.replace(LOGGER_PREFIX, "");
            p.put(key, entry.getValue());
        }
        return p;
    }

}
