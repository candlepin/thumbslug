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
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.channel.ExceptionEvent;

/**
 * HttpRelayingResponseHandler
 */
public class HttpRelayingResponseHandler extends SimpleChannelUpstreamHandler {
    private static Logger log = Logger.getLogger(HttpRelayingResponseHandler.class);


    private boolean readingChunks;
    private Channel client;
    private boolean keepAlive;
    private boolean pendingException;

    public HttpRelayingResponseHandler(Channel client, boolean keepAlive) {
        this.client = client;
        this.keepAlive = false;
        this.pendingException = false;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
        throws Exception {
        /*
         * We want to send a 502 bad_gateway to the client here, but most likely this is
         * happening because of a misconfiguration, and we've timedout trying to connect to
         * the cdn. Thus, the client hasn't even had a chance yet to send its request up.
         * Delay sending the 502 till after the client begins a write, then shut them down.
         */ 
        pendingException = true;

        //e.getCause().printStackTrace();
    }
    /**
     * This is an event *to* the client coming *from* the cdn
     * 
     * @throws Exception - an exception
     */
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
        throws Exception {
        log.debug("message received!");
        if (!readingChunks) {
            HttpResponse response = (HttpResponse) e.getMessage();

            ChannelFuture future = client.write(response);

            if (response.getStatus().getCode() == 200 && response.isChunked()) {
                readingChunks = true;
            }
            else {
                if (!keepAlive) {
                    future.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture arg0) throws Exception {
                            client.close();
                        }
                    });
                }
            }
        }
        else {
            HttpChunk chunk = (HttpChunk) e.getMessage();
            ChannelFuture future = client.write(chunk);
            if (chunk.isLast()) {
                readingChunks = false;
                
                if (!keepAlive) {
                    future.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture arg0) throws Exception {
                            client.close();
                        }
                    });
                }
            }
        }
    }
    
    
    /**
     * A request going *from* the client *to* the cdn.
     * At the moment we only intercept these in this class to send back a bad gateway
     * reply if needed.
     * 
     * @throws Exception - an exception
     */
    @Override
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e)
        throws Exception {
        if (pendingException) {
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.BAD_GATEWAY);
            ChannelFuture future = client.write(response);
            if (!keepAlive) {
                future.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture arg0) throws Exception {
                        client.close();
                    }
                });
            }
        }
        super.handleUpstream(ctx, e);
    }
    
//    private void writeResponse(MessageEvent e) {
//        // Decide whether to close the connection or not.
//        boolean keepAlive = isKeepAlive(request);
//
//        // Build the response object.
//        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
//        response.setContent(ChannelBuffers.copiedBuffer(buf.toString(),
//                CharsetUtil.UTF_8));
//        response.setHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");
//
//        if (keepAlive) {
//            // Add 'Content-Length' header only for a keep-alive connection.
//            response.setHeader(CONTENT_LENGTH, response.getContent()
//                    .readableBytes());
//        }
//
//        // Encode the cookie.
//        String cookieString = request.getHeader(COOKIE);
//        if (cookieString != null) {
//            CookieDecoder cookieDecoder = new CookieDecoder();
//            Set<Cookie> cookies = cookieDecoder.decode(cookieString);
//            if (!cookies.isEmpty()) {
//                // Reset the cookies if necessary.
//                CookieEncoder cookieEncoder = new CookieEncoder(true);
//                for (Cookie cookie : cookies) {
//                    cookieEncoder.addCookie(cookie);
//                }
//                response.addHeader(SET_COOKIE, cookieEncoder.encode());
//            }
//        }
//
//        // Write the response.
//        ChannelFuture future = e.getChannel().write(response);
//
//        // Close the non-keep-alive connection after the write operation is
//        // done.
//        if (!keepAlive) {
//            future.addListener(ChannelFutureListener.CLOSE);
//        }
//
//        System.out.println(String.format("%s %s %d", request.getMethod(),
//                request.getUri(), response.getStatus().getCode()));
//    }

}
