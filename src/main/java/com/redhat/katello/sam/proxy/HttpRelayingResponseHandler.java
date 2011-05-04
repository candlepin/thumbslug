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
package com.redhat.katello.sam.proxy;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpResponse;

/**
 * HttpRelayingResponseHandler
 */
public class HttpRelayingResponseHandler extends SimpleChannelUpstreamHandler {

    private boolean readingChunks;
    private Channel client;

    public HttpRelayingResponseHandler(Channel client) {
        this.client = client;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
        throws Exception {
        if (!readingChunks) {
            HttpResponse response = (HttpResponse) e.getMessage();

            // System.out.println("STATUS: " + response.getStatus());
            // System.out.println("VERSION: " + response.getProtocolVersion());
            // System.out.println();

            if (!response.getHeaderNames().isEmpty()) {
                for (String name : response.getHeaderNames()) {
                    for (String value : response.getHeaders(name)) {
                        // System.out.println("HEADER: " + name + " = " +
                        // value);
                    }
                }
                // System.out.println();
            }

            if (response.getStatus().getCode() == 200 && response.isChunked()) {
                readingChunks = true;
                // System.out.println("CHUNKED CONTENT {");
            }
            else {
                ChannelBuffer content = response.getContent();
                if (content.readable()) {
                    // System.out.println("CONTENT {");
                    // System.out.println(content.toString(CharsetUtil.UTF_8));
                    // System.out.println("} END OF CONTENT");
                }
                client.write(response);
            }
        }
        else {
            HttpChunk chunk = (HttpChunk) e.getMessage();
            if (chunk.isLast()) {
                readingChunks = false;
                // System.out.println("} END OF CHUNKED CONTENT");
            }
            else {
                client.write(chunk);
                // System.out.print(chunk.getContent().toString(CharsetUtil.UTF_8));
                // System.out.flush();
            }
        }
    }
}
