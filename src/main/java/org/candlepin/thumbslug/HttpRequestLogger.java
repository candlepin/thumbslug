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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Calendar;
import java.util.Locale;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

/**
 * HttpRequestLogger - logs client requests in an apache like format
 */
class HttpRequestLogger extends SimpleChannelHandler {
    // this logger we use for the access log. the other is
    // for logging stuff like not being able to open the access log file
    private final Logger accessLog = Logger.getLogger(
        HttpRequestLogger.class.getCanonicalName() + ".accessLog");
    private final Logger log = Logger.getLogger(HttpRequestLogger.class);

    private static boolean loggingConfigured = false;

    // CLF. see wikipedia ;)
    private static final String DEFAULT_LOG_FORMAT =
        "%1$s - - [%2$td/%2$tb/%2$tY:%2$tT %2$tz] \"%3$s %4$s %5$s\" %6$d %7$d";

    private String inetAddress;
    private String method;
    private String uri;
    private String protocol;
    private int status;
    private Long contentLength;

    HttpRequestLogger(String fileName) {
        clearState();
        configureAccessLog(fileName);
    }

    // the client request
    @Override
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        if (e instanceof MessageEvent) {
            HttpRequest request = (HttpRequest) ((MessageEvent) e).getMessage();

            // on the off chance that this isn't an ip connection,
            // we're going to check first
            inetAddress = "-";
            SocketAddress address = ((MessageEvent) e).getRemoteAddress();
            if (address instanceof InetSocketAddress) {
                inetAddress = ((InetSocketAddress) address).getAddress().toString();
                if (inetAddress.startsWith("/")) {
                    inetAddress = inetAddress.substring(1);
                }

            }
            // maybe this request was proxied or load balanced.
            // try and get the real originating IP
            // XXX: someone could set this header on their request to hide their ip
            if (request.containsHeader("X-Forwarded-For")) {
                // can contain multiple IPs for proxy chains. the first ip is our client.
                String proxyChain = request.getHeader("X-Forwarded-For");
                // we'll skip an empty header, but a malformed header will still get through
                if (!proxyChain.equals("")) {
                    int firstComma = proxyChain.indexOf(',');
                    if (firstComma != -1) {
                        inetAddress = proxyChain.substring(0, proxyChain.indexOf(','));
                    }
                    else {
                        inetAddress = proxyChain;
                    }
                }
            }

            method = request.getMethod().getName();
            uri = request.getUri();
            protocol = request.getProtocolVersion().toString();
        }
        super.handleUpstream(ctx, e);
    }

    /**
     * handleDownstream - handle the http response
     *
     * in both cases, we can get the status and content length from the first reply message.
     * if the reply is chunked, we have to wait till we've sent back the last response
     * before logging, so we can get an accurate time.
     */
    @Override
    public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e)
        throws Exception {
        super.handleDownstream(ctx, e);

        if (e instanceof MessageEvent) {
            Object msg = ((MessageEvent) e).getMessage();
            if (msg instanceof HttpResponse) {
                HttpResponse response = (HttpResponse) msg;

                status = response.getStatus().getCode();
                contentLength = HttpHeaders.getContentLength(response);

                if (!response.isChunked()) {
                    logAccess();
                    clearState();
                }
            }
            else if (msg instanceof HttpChunk) {
                HttpChunk chunk = (HttpChunk) msg;

                if (chunk.isLast()) {
                    logAccess();
                    clearState();
                }
            }
        }
    }

    private void clearState() {
        inetAddress = null;
        method = null;
        uri = null;
        protocol = null;
        status = -1;
        contentLength = -1L;
    }

    private void logAccess() {
        // We've got to use the a default locale here, so that month name is
        // formatted properly for CLF, regardless of server locale. I've chosen Canada!
        if (accessLog.isInfoEnabled()) {
            accessLog.info(String.format(Locale.CANADA, DEFAULT_LOG_FORMAT,
                inetAddress, Calendar.getInstance(), method, uri, protocol, status,
                contentLength));
        }
    }

    private synchronized void configureAccessLog(String fileName) {
        if (loggingConfigured) {
            return;
        }
        loggingConfigured = true;

        accessLog.setLevel(org.apache.log4j.Level.INFO);
        try {
            Layout layout = new PatternLayout("%m%n");
            FileAppender fileAppender = new FileAppender(layout, fileName);
            accessLog.addAppender(fileAppender);
            accessLog.setAdditivity(false);
        }
        catch (IOException e) {
            // if we error here, we'll just end up logging the accesses to the standard
            // log output, which is ok.
            log.error("unable to open access.log for writing!", e);
        }
    }

    // for testing
    String getInetAddress() {
        return this.inetAddress;
    }
}
