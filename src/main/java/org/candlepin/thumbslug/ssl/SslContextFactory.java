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

/*
 * Adapted from Netty example code, which is
 *      Copyright (C) 2008  Trustin Heuiseung Lee
 */

package org.candlepin.thumbslug.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Scanner;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import net.oauth.signature.pem.PEMReader;

import org.apache.log4j.Logger;

/**
 * SslContextFactory
 */
public class SslContextFactory {
    private static Logger log = Logger.getLogger(SslContextFactory.class);

    private static final String PROTOCOL = "TLS";

    // we only want to initialize the server and client context once.
    private static SSLContext serverContext = null;
    private static SSLContext clientContext = null;

    private SslContextFactory() {
        // for checkstyle
    }

    public static SSLContext getServerContext(String keystoreUrl, String keystorePassword,
        String caUrl) throws SslKeystoreException, SslPemException {

        if (serverContext != null) {
            return serverContext;
        }

        String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
        if (algorithm == null) {
            algorithm = "SunX509";
        }

        FileInputStream fis = null;
        try {
            log.info("reading keystore");
            fis = new FileInputStream(new File(keystoreUrl));
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(fis, keystorePassword.toCharArray());

            // Set up key manager factory to use our key store
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
            kmf.init(ks, keystorePassword.toCharArray());

            Scanner scanner = new Scanner(new File(caUrl));
            scanner.useDelimiter("\\Z");
            String caPem = scanner.next();

            // Initialize the SSLContext to work with our key managers.
            serverContext = SSLContext.getInstance(PROTOCOL);
            serverContext.init(kmf.getKeyManagers(),
                ServerContextTrustManager.getTrustManagers(
                    PemChainLoader.loadChain(caPem)), null);
        }
        catch (SslPemException e) {
            throw e;
        }
        catch (Exception e) {
            throw new SslKeystoreException(
                    "Failed to initialize the server-side SSLContext.", e);
        }
        finally {
            if (fis != null) {
                try {
                    fis.close();
                }
                catch (IOException e) {
                    throw new Error(
                        "Failed to initialize the client-side SSLContext", e);
                }

            }
        }

        return serverContext;
    }

    public static SSLContext getClientContext(String pem, String caUrl)
        throws SslPemException {

        if (clientContext != null) {
            return clientContext;
        }

        try {
            log.debug("loading thumbslug to cdn entitlement certificate (pem encoded)");

            // split the pem into its two parts, then figure out which is
            // the private and which is the public part
            log.debug("Cert is: \n" + pem);
            int endOfFirstPart = pem.indexOf("\n", pem.indexOf("END"));
            if (endOfFirstPart == -1) {
                throw new IllegalArgumentException("unable to parse PEM data");
            }
            String certificate = pem.substring(0, endOfFirstPart);
            String privateKey = pem.substring(endOfFirstPart);
            if (!certificate.startsWith(PEMReader.CERTIFICATE_X509_MARKER)) {
                String tmp = privateKey;
                privateKey = certificate;
                certificate = tmp;
            }

            PEMx509KeyManager [] managers = new PEMx509KeyManager[1];
            managers[0] = new PEMx509KeyManager();
            managers[0].addPEM(certificate, privateKey);

            Scanner scanner = new Scanner(new File(caUrl));
            scanner.useDelimiter("\\Z");
            String caPem = scanner.next();

            // Initialize the SSLContext to work with our key managers.
            clientContext = SSLContext.getInstance(PROTOCOL);
            clientContext.init(managers,
                    ClientContextTrustManager.getTrustManagers(
                        PemChainLoader.loadChain(caPem)), null);
            log.debug("SSL context initialized!");
        }
        catch (Exception e) {
            log.error("Unable to load pem file!", e);
            throw new SslPemException(
                    "Failed to initialize the client-side SSLContext", e);
        }

        return clientContext;
    }

    public static SSLContext getCandlepinClientContext() {
        // Candlepin client context, we won't be sending up an ssl cert,
        // just verifying that of candlepin
        SSLContext clientContext = null;

        try {
            clientContext = SSLContext.getInstance(PROTOCOL);
            clientContext.init(null, ClientContextTrustManager.getTrustManagers(null),
                null);
        }
        catch (NoSuchAlgorithmException e) {
            throw new Error("Failed to initialize the thumbslug to candlepin ssl context",
                e);
        }
        catch (KeyManagementException e) {
            throw new Error("Failed to initialize the thumbslug to candlepin ssl context",
                e);
        }

        return clientContext;
    }
}
