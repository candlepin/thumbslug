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

import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;

import org.candlepin.thumbslug.HttpCandlepinClient.CandlepinClientResponseHandler;
import org.candlepin.thumbslug.HttpCdnClientChannelFactory.OnCdnConnectedCallback;
import org.candlepin.thumbslug.ssl.SslPemException;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.ssl.SslHandler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLPeerUnverifiedException;

/**
 * HttpRequestHandler
 */
class HttpRequestHandler extends SimpleChannelUpstreamHandler {

    private static Logger log = Logger.getLogger(HttpRequestHandler.class);

    private HttpRequest request;
    private boolean readingChunks;
    private Channel cdnChannel;
    private Channel candlepinChannel;

    private Config config;
    private HttpCdnClientChannelFactory clientFactory;
    private ChannelFactory channelFactory;

    HttpRequestHandler(Config config, HttpCdnClientChannelFactory clientFactory,
        ChannelFactory channelFactory) {
        System.out.println("XXX HttpRequestHandler.ctor");
        this.config = config;
        this.clientFactory = clientFactory;
        this.channelFactory = channelFactory;

        this.candlepinChannel = null;
        this.cdnChannel = null;
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
        // Add to the global list of open channels for graceful shutdown
        Main.ALL_CHANNELS.add(ctx.getChannel());
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
        throws Exception {
        System.out.println("XXX HttpRequestHandler.messageReceived");
        if (!readingChunks) {
            requestStartReceived(ctx, e);
        }
        else {
            requestChunkReceived(e);
        }
    }

    // we need a java.security.cert rather than a javax one, so we can read extensions.
    private static X509Certificate convertCertificate(
        javax.security.cert.X509Certificate cert) {
        System.out.println("XXX convertCertificate");
        String errMsg = "Unable to convert x509 certificate";
        try {
            byte[] encoded = cert.getEncoded();
            ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
            java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory
                .getInstance("X.509");
            return (java.security.cert.X509Certificate) cf
                .generateCertificate(bis);
        }
        catch (java.security.cert.CertificateEncodingException e) {
            log.error(errMsg, e);
        }
        catch (javax.security.cert.CertificateEncodingException e) {
            log.error(errMsg, e);
        }
        catch (java.security.cert.CertificateException e) {
            log.error(errMsg, e);
        }
        return null;
    }

    private String getEntitlementId(SslHandler handler) {
        try {
            X509Certificate cert = convertCertificate(
                handler.getEngine().getSession().getPeerCertificateChain()[0]);

            String entitlementId = cert.getSubjectX500Principal().getName().split("=")[1];
            return entitlementId;
        }
        catch (SSLPeerUnverifiedException e) {
            // This isn't going to happen here, afaik.
            log.error("Unverified peer!", e);
        }
        return null;
    }

    private void requestStartReceived(final ChannelHandlerContext ctx, final MessageEvent e)
        throws Exception {
        System.out.println("XXX requestStartReceived");
        // configured to use a static ssl cert for all cdn communication. for testing only!
        // XXX we'll have to remove this at some point and just always do it

        // prevent any more messages until we are connected to the cdn.
        e.getChannel().setReadable(false);

        if (config.getBoolean("ssl.client.dynamicSsl")) {
            // Grab the entitlement UUID
            String entitlementUuid = getEntitlementId(
                ctx.getChannel().getPipeline().get(SslHandler.class));

            HttpCandlepinClient client = new HttpCandlepinClient(config,
                new CandlepinClientResponseHandler() {
                    @Override
                    public void onResponse(String buffer) throws Exception {
                        log.debug("Buffer for /entitlements call :" + buffer);
                        beginCdnCommunication(ctx, e, buffer);
                    }

                    @Override
                    public void onError(Throwable reason) {
                        log.error("Error talking to candlepin", reason);
                        sendResponseToClient(ctx, HttpResponseStatus.BAD_GATEWAY);
                    }

                    @Override
                    public void onNotFound() {
                        log.error("Subscription cert has been revoked!");
                        sendResponseToClient(ctx, HttpResponseStatus.UNAUTHORIZED);
                    }

                    @Override
                    public void onOtherResponse(int code) {
                        log.error("Unexpected response code from candlepin: " + code);
                        sendResponseToClient(ctx, HttpResponseStatus.BAD_GATEWAY);
                    }

                }, channelFactory);

            candlepinChannel =
                client.getSubscriptionCertificateViaEntitlementId(entitlementUuid);
        }
        else {
            String pem = "";
            FileInputStream fis = null;
            String errorMsg = "Failed to read static client PEM file";
            try {
                fis = new FileInputStream(
                    new File(config.getProperty("ssl.client.keystore")));

                final char[] buffer = new char[0x10000];
                StringBuilder out = new StringBuilder();
                Reader in = new InputStreamReader(fis, "UTF-8");
                int read;
                do {
                    read = in.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        out.append(buffer, 0, read);
                    }
                } while (read >= 0);

                pem = out.toString();
            }
            catch (Exception exception) {
                throw new Error(errorMsg, exception);
            }
            finally {
                if (fis != null) {
                    try {
                        fis.close();
                    }
                    catch (IOException exception) {
                        throw new Error(errorMsg, exception);
                    }
                }
            }

            beginCdnCommunication(ctx, e, pem);
        }
    }

    private void sendResponseToClient(ChannelHandlerContext ctx,
        HttpResponseStatus status) {
        System.out.println("XXX send response to client");
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
        ChannelFuture future = ctx.getChannel().write(response);
        future.addListener(ChannelFutureListener.CLOSE);
    }

    private void beginCdnCommunication(ChannelHandlerContext ctx, MessageEvent e,
        String pem) throws Exception {

        System.out.println("XXX talk to CDN");
        this.request = (HttpRequest) e.getMessage();
        final HttpRequest request = this.request;
        final Channel inbound = e.getChannel();
        if (config.getBoolean("cdn.sendTSheader")) {
            request.addHeader("X-Forwarded-By", "Thumbslug v1.0");
        }

        // Reset the host header to our new request.
        // A certain CDN provider is quite picky about this.
        request.setHeader("Host",
            config.getProperty("cdn.host") + ":" + config.getProperty("cdn.port"));

        // Likewise, we have to reset the get path, just in case.
        URI uri = new URI(request.getUri());
        String rebuiltUri = uri.getRawPath();
        if (uri.getRawQuery() != null) {
            rebuiltUri += uri.getRawQuery();
        }
        if (uri.getRawFragment() != null) {
            rebuiltUri += uri.getRawFragment();
        }
        request.setUri(rebuiltUri);

        try {
            clientFactory.getPipeline(e.getChannel(),
                isKeepAlive(request), pem, new OnCdnConnectedCallback() {
                    @Override
                    public void onCdnConnected(Channel channel) {
                        cdnChannel = channel;
                        cdnChannel.write(request).addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture arg0)
                                throws Exception {
                                inbound.setReadable(true);
                            }
                        });
                    }
                    @Override
                    public void onCdnError(Channel channel) {
                        // this is where we send a 502 back if we could not
                        // interpret the cert from candlepin
                        HttpResponse response = new DefaultHttpResponse(
                            HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY);

                        cdnChannel = channel;
                        inbound.write(response).addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture arg0)
                                throws Exception {
                                inbound.setReadable(true)
                                    .addListener(ChannelFutureListener.CLOSE);
                            }
                        });
                    }
                });
        }
        catch (SslPemException p) {
            // thrown when we can't build an ssl engine from the pem cert.
            // that is, before a connection is established.
            sendResponseToClient(ctx, HttpResponseStatus.BAD_GATEWAY);
        }

        if (request.isChunked()) {
            readingChunks = true;
        }
    }

    private void requestChunkReceived(MessageEvent e) {
        HttpChunk chunk = (HttpChunk) e.getMessage();
        if (chunk.isLast()) {
            readingChunks = false;
        }
        else {
            cdnChannel.write(chunk);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
        throws Exception {
        log.error("Exception caught!", e.getCause());
        ctx.getChannel().close();
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent event)
        throws Exception {
        if (cdnChannel != null && cdnChannel.isConnected()) {
            cdnChannel.write(ChannelBuffers.EMPTY_BUFFER)
                .addListener(ChannelFutureListener.CLOSE);
        }

        if (candlepinChannel != null && candlepinChannel.isConnected()) {
            candlepinChannel.write(ChannelBuffers.EMPTY_BUFFER)
                .addListener(ChannelFutureListener.CLOSE);
        }
    }
}
