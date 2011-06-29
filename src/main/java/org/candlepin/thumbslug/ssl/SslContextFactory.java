/*
 * Adapted from Netty example code, which is 
 *      Copyright (C) 2008  Trustin Heuiseung Lee
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, 5th Floor, Boston, MA 02110-1301 USA
 */

package org.candlepin.thumbslug.ssl;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.Security;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.apache.log4j.Logger;

public class SslContextFactory {
    private static Logger log = Logger.getLogger(SslContextFactory.class);


    private static final String PROTOCOL = "TLS";

    public static SSLContext getServerContext(String keystoreUrl, String keystorePassword) {
        String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
        if (algorithm == null) {
            algorithm = "SunX509";
        }

        SSLContext serverContext = null;
        try {
            log.info("reading keystore");
            FileInputStream fis = new FileInputStream(new File(keystoreUrl));
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(fis, keystorePassword.toCharArray());

            // Set up key manager factory to use our key store
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
            kmf.init(ks, keystorePassword.toCharArray());

            // Initialize the SSLContext to work with our key managers.
            serverContext = SSLContext.getInstance(PROTOCOL);
            serverContext.init(
                    kmf.getKeyManagers(),
                    TrustManagerFactory.getTrustManagers(), null);
        }
        catch (Exception e) {
            throw new Error(
                    "Failed to initialize the server-side SSLContext", e);
        }

        return serverContext;
    }

    public static SSLContext getClientContext() {
        SSLContext clientContext = null;
    
        try {
            clientContext = SSLContext.getInstance(PROTOCOL);
            clientContext.init(
                    null, TrustManagerFactory.getTrustManagers(), null);
        }
        catch (Exception e) {
            throw new Error(
                    "Failed to initialize the client-side SSLContext", e);
        }

        return clientContext;
    }
}
