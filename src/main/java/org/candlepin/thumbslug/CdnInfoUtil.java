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
import org.candlepin.thumbslug.model.CdnInfo;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * CdnInfoUtil is a utility method that knows how to introspect a CdnInfo
 * object for its information and if not present, then knows how to look
 * at the Thumbslug configuration for the correct values. This prevents
 * a bunch of conditional logic in the main Thumbslug code and keeps
 * the Config out of CdnInfo.
 */
public class CdnInfoUtil {
    private static Logger log = Logger.getLogger(CdnInfoUtil.class);

    private CdnInfoUtil() {
        // do nothing
    }

    public static String getCdnHost(CdnInfo cdn, Config config) {
        String host = null;
        if (cdn != null && cdn.getCdnUrl() != null) {
            host = getAsUrl(cdn.getCdnUrl()).getHost();
        }

        if (host == null) {
            host = config.getProperty("cdn.host");
        }

        return host;
    }

    public static int getCdnPort(CdnInfo cdn, Config config) {
        int port = -1; // getPort returns this if not set

        if (cdn != null && cdn.getCdnUrl() != null) {
            port = getAsUrl(cdn.getCdnUrl()).getPort();
            System.err.println("XXX cdn port: " + port);
        }

        // if no port was configured better get the one from
        // the configuration file.
        if (port < 0) {
            port = config.getInt("cdn.port");
            System.err.println("XXX config cdn.port: " + port);
        }

        log.debug("using port: " + String.valueOf(port));
        return port;
    }

    public static String getCdnSslKeystore(CdnInfo cdn, Config config) {
        String cdnCert = null;
        if (cdn != null) {
            System.err.println("XXX using CDN cert from candlepin");
            cdnCert = cdn.getCdnCert();
        }

        if (cdnCert == null) {
            System.err.println("XXX using configured cdn.ssl.ca.keystore");
            cdnCert = config.getProperty("cdn.ssl.ca.keystore");
        }

        return cdnCert;
    }

    public static URL getAsUrl(String urlStr) {
        URL url = null;

        try {
            url = new URL(urlStr);
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return url;
    }
}
