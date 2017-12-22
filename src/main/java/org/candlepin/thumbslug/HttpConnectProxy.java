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

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpVersion;

/**
 * HttpConnectProxy
 */
public class HttpConnectProxy extends SimpleChannelUpstreamHandler {

    /**
     * OnProxyConnectedCallback
     */
    // XXX pass exceptions back through interface
    interface OnProxyConnectedCallback {
        void onConnected(ChannelHandlerContext ctx);
        //void onError(ChannelHandlerContext ctx, Throwable reason);
    };

    private OnProxyConnectedCallback callback;
    private String uri;
    private String proxyAuth;

    private static Logger log = Logger.getLogger(HttpConnectProxy.class);


    public HttpConnectProxy(Config config, OnProxyConnectedCallback callback) {
        this.callback = callback;
        uri = config.getProperty("cdn.host") + ":" +
            config.getProperty("cdn.port");

        if (config.getProperty("cdn.proxy.user") != null &&
            !config.getProperty("cdn.proxy.user").equals("")) {
            proxyAuth = config.getProperty("cdn.proxy.user") + ":" +
                config.getProperty("cdn.proxy.password");
        }
        else {
            proxyAuth = null;
        }
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent event)
        throws Exception {
        Channel channel = ctx.getChannel();

        HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
            HttpMethod.CONNECT, uri);

        request.addHeader("Host", uri);

        if (proxyAuth != null) {
            request.addHeader("Proxy-authorization", "Basic " +
                new String(Base64.encodeBase64(proxyAuth.getBytes())));
        }

        channel.write(request);
        ctx.sendUpstream(event);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent event)
        throws Exception {
        final ChannelFuture future = Channels.future(ctx.getChannel());

        if (!future.isDone()) {
            final HttpResponse response = (HttpResponse) event.getMessage();

            final int code = response.getStatus().getCode();

            if (!(code >= 200 && code < 300)) {
                log.warn("Received status code " + code + " from upstream proxy");
                // XXX set callback error here
            }
            callback.onConnected(ctx);
        }
        else {
            throw new RuntimeException("Programmer error. Proxy Connection " +
                "not initialized properly!");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        log.error("Exception caught!", e.getCause());

        //we shouldn't be getting here..
        e.getChannel().close();

    }


}
