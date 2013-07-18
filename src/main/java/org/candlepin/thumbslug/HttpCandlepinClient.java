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

import static org.jboss.netty.channel.Channels.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;

import javax.net.ssl.SSLEngine;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;

import org.apache.log4j.Logger;
import org.candlepin.thumbslug.ssl.SslContextFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpClientCodec;
import org.jboss.netty.handler.codec.http.HttpContentDecompressor;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.util.CharsetUtil;

/**
 * HttpCandlepinClient - a class to make HTTP calls to candlepin
 */
class HttpCandlepinClient {
    private static Logger log = Logger.getLogger(HttpCandlepinClient.class);

    private String buffer;
    private CandlepinClientResponseHandler responseHandler;

    private String candlepinHost;
    private int candlepinPort;
    private boolean useSSL;

    private String oAuthKey;
    private String oAuthSecret;

    private ChannelFactory channelFactory;

    HttpCandlepinClient(Config config,
        CandlepinClientResponseHandler responseHandler, ChannelFactory channelFactory) {
        this.responseHandler = responseHandler;
        this.channelFactory = channelFactory;

        candlepinHost = config.getProperty("candlepin.host");
        candlepinPort = config.getInt("candlepin.port");
        useSSL = config.getBoolean("candlepin.ssl");

        oAuthKey = config.getProperty("candlepin.oauth.key");
        oAuthSecret = config.getProperty("candlepin.oauth.secret");
    }

    private ChannelPipeline getPipeline() {
        ChannelPipeline pipeline = pipeline();

        if (useSSL) {
            SSLEngine engine =
                SslContextFactory.getCandlepinClientContext().createSSLEngine();
            engine.setUseClientMode(true);
            pipeline.addLast("ssl", new SslHandler(engine));
        }

        pipeline.addLast("codec", new HttpClientCodec());

        pipeline.addLast("inflater", new HttpContentDecompressor());

        // Uncomment the following line if you don't want to handle HttpChunks.
        // XXX handle chunking ourselves and begin passing the pem onwards as
        // soon as we see a good http response
        pipeline.addLast("aggregator", new HttpChunkAggregator(1048576));
        pipeline.addLast("handler", new HttpResponseHandler());

        return pipeline;
    }

    private class HttpResponseHandler extends SimpleChannelUpstreamHandler {
        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
            HttpResponse response = (HttpResponse) e.getMessage();
            buffer = response.getContent().toString(CharsetUtil.UTF_8);

            log.debug("candlepin response: " + response.getStatus().getCode());
            log.debug("candlepin response: " + response.getHeaders());
            log.debug("candlepin response: \n" + buffer);
            if (response.getStatus().equals(HttpResponseStatus.NOT_FOUND)) {
                responseHandler.onNotFound();
            }
            else if (!response.getStatus().equals(HttpResponseStatus.OK) &&
                !response.getStatus().equals(HttpResponseStatus.PARTIAL_CONTENT)) {
                responseHandler.onOtherResponse(response.getStatus().getCode());
            }
            else {
                //we got a 200 here, but not necessarily a valid cert.
                responseHandler.onResponse(buffer);
            }
            ctx.getChannel().close();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
            responseHandler.onError(e.getCause());
            ctx.getChannel().close();
        }
    }

    Channel getSubscriptionCertificateViaEntitlementId(final String entitlementId) {
        Channel requestChannel = channelFactory.newChannel(getPipeline());
        // Set up the event pipeline factory.

        // Start the connection attempt.
        ChannelFuture future = requestChannel.connect(new InetSocketAddress(candlepinHost,
            candlepinPort));

        future.addListener(new ChannelFutureListener() {
            public void operationComplete(final ChannelFuture future)
                throws Exception {
                System.out.println("XXX operationComplete called");
                onSubscriptionCertificateViaEntitlementId(
                    future.getChannel(), entitlementId);
            }
        });

        return requestChannel;
    }

    public void getCandlepinStatus() {
        Channel requestChannel = channelFactory.newChannel(getPipeline());
        ChannelFuture future = requestChannel.connect(new InetSocketAddress(candlepinHost,
            candlepinPort));

        future.addListener(new ChannelFutureListener() {
            public void operationComplete(final ChannelFuture future)
                throws Exception {
                onGetStatus(future.getChannel());
            }
        });
    }

    private void onSubscriptionCertificateViaEntitlementId(Channel channel,
        String entitlementId) {
        String url = String.format("http%s://%s:%s/candlepin/entitlements/%s/upstream_cert",
            useSSL ? "s" : "", candlepinHost, candlepinPort, entitlementId);

        onConnectedToCandlepin(channel, url, true);
    }

    private void onGetStatus(Channel channel) {
        String url = String.format("http%s://%s:%s/candlepin/status",
            useSSL ? "s" : "", candlepinHost, candlepinPort);

        onConnectedToCandlepin(channel, url, false);
    }

    private void onConnectedToCandlepin(Channel channel, String url, boolean textOnly) {
        // Prepare the HTTP request.

        HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
            HttpMethod.GET, url);
        request.setHeader(HttpHeaders.Names.HOST, candlepinHost);
        request.setHeader(HttpHeaders.Names.CONNECTION,
            HttpHeaders.Values.CLOSE);
        request.setHeader(HttpHeaders.Names.ACCEPT_ENCODING,
            HttpHeaders.Values.GZIP);

        if (textOnly) {
            request.setHeader(HttpHeaders.Names.ACCEPT, "text/plain");
        }

        OAuthConsumer consumer = new OAuthConsumer(null, oAuthKey, oAuthSecret, null);
        consumer.setProperty(OAuth.OAUTH_SIGNATURE_METHOD, OAuth.HMAC_SHA1);
        OAuthAccessor accessor = new OAuthAccessor(consumer);

        String errMsg = "OAuth error!";

        try {
            System.out.println("Making request to [" + url + "]");

            OAuthMessage oAuthRequest = accessor.newRequestMessage(OAuthMessage.GET, url,
                null);
            request.setHeader(HttpHeaders.Names.AUTHORIZATION,
                oAuthRequest.getAuthorizationHeader(null));
        }
        catch (OAuthException e) {
            // TODO Auto-generated catch block
            log.error(errMsg, e);
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            log.error(errMsg, e);
        }
        catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            log.error(errMsg, e);
        }

        // Send the HTTP request.
        channel.write(request);
    }

    /**
     * CandlepinClientResponseHandler
     */
    public interface CandlepinClientResponseHandler {
        void onResponse(String buffer) throws Exception;
        void onError(Throwable reason);
        void onNotFound();
        void onOtherResponse(int code);
    }
}
