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

import static org.jboss.netty.channel.Channels.pipeline;

import org.candlepin.thumbslug.HttpConnectProxy.OnProxyConnectedCallback;
import org.candlepin.thumbslug.ssl.SslContextFactory;
import org.candlepin.thumbslug.ssl.SslPemException;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpClientCodec;
import org.jboss.netty.handler.ssl.SslHandler;

import java.net.InetSocketAddress;

import javax.net.ssl.SSLEngine;

/**
 * HttpClientPipelineFactory
 */
class HttpCdnClientChannelFactory {
    private final Config config;
    private final ChannelFactory channelFactory;

    private static Logger log = Logger.getLogger(HttpCdnClientChannelFactory.class);


    /**
     * OnCdnConnectedCallback
     */
    // XXX pass exceptions back through interface
    interface OnCdnConnectedCallback {
        void onCdnConnected(Channel channel);
        void onCdnError(Channel channel);
    }


    public HttpCdnClientChannelFactory(Config config, ChannelFactory channelFactory) {
        this.config = config;
        this.channelFactory = channelFactory;

    }

    private void buildFinalPipeline(ChannelPipeline pipeline, Channel client,
        boolean keepAlive, String pem) throws SslPemException {

        if (config.getBoolean("cdn.ssl")) {
            //this is where we bomb if we can't read the PEM data from cpin
            SSLEngine engine = SslContextFactory.getClientContext(pem,
                config.getProperty("cdn.ssl.ca.keystore")).createSSLEngine();
            engine.setUseClientMode(true);
            pipeline.addLast("ssl", new SslHandler(engine));
        }
        pipeline.addLast("codec", new HttpClientCodec());
        pipeline.addLast("handler", new HttpRelayingResponseHandler(client, keepAlive));
    }

    private void handshakeSsl(ChannelHandlerContext ctx,
                                final OnCdnConnectedCallback callback) {
        SslHandler handler = (SslHandler) ctx.getPipeline().get("ssl");
        ChannelFuture future = handler.handshake();
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    callback.onCdnConnected(future.getChannel());
                }
                else {
                    callback.onCdnError(future.getChannel());
                }
            }
        });
    }

    public void getPipeline(final Channel client, final boolean keepAlive, final String pem,
        final OnCdnConnectedCallback callback)
        throws SslPemException {
        ChannelPipeline pipeline = pipeline();


        if (config.getBoolean("cdn.proxy")) {
            buildProxyPipeline(client, keepAlive, pem, callback, pipeline);
        }
        else {
            InetSocketAddress remote = new InetSocketAddress(
                config.getProperty("cdn.host"), config.getInt("cdn.port"));

            buildFinalPipeline(pipeline, client, keepAlive, pem);

            Channel cdnChannel = channelFactory.newChannel(pipeline);
            ChannelFuture future = cdnChannel.connect(remote);
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        callback.onCdnConnected(future.getChannel());
                    }
                    else {
                        callback.onCdnError(future.getChannel());
                    }
                }
            });
        }
    }

    private void buildProxyPipeline(final Channel client,
        final boolean keepAlive, final String pem,
        final OnCdnConnectedCallback callback, ChannelPipeline pipeline) {

        InetSocketAddress remote;
        pipeline.addLast("codec", new HttpClientCodec());
        pipeline.addLast("proxy", new HttpConnectProxy(config,
                                new OnProxyConnectedCallback() {
                @Override
                public void onConnected(ChannelHandlerContext ctx) {
                    ChannelPipeline pipeline = ctx.getPipeline();
                    pipeline.remove("codec");

                    try {
                        buildFinalPipeline(pipeline, client, keepAlive, pem);
                    }
                    catch (SslPemException e) {
                        // send a 502 back to the user
                        log.error("unable to read ssl cert from cpin, " +
                            "sending a 502 back to the client: ", e);
                        callback.onCdnError(ctx.getChannel());
                        return;
                    }
                    // we keep this handler around until after build final pipeline so
                    // that there's always at least one handler on the channel.
                    // this prevents netty from nagging us about channels with no handlers
                    pipeline.remove("proxy");

                    if (config.getBoolean("cdn.ssl")) {
                        handshakeSsl(ctx, callback);
                    }
                    else {
                        callback.onCdnConnected(ctx.getChannel());
                    }
                }
            }));

        remote = new InetSocketAddress(config.getProperty("cdn.proxy.host"),
            config.getInt("cdn.proxy.port"));

        Channel cdnChannel = channelFactory.newChannel(pipeline);
        ChannelFuture future = cdnChannel.connect(remote);
        future.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) {

                if (!future.isSuccess()) {
                    log.warn("unable to connect to proxy!");
                    callback.onCdnError(future.getChannel());
                }
            }
        });
    }
}
