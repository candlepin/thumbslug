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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.junit.Test;

/**
 * HttpRequestLoggerTests
 */
public class HttpRequestLoggerTest {

    @Test
    public void testIpAddressIsSet() throws Exception {
        HttpRequestLogger logger = new HttpRequestLogger("nofile");
        
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        MessageEvent e = mock(MessageEvent.class);
        DefaultHttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
            HttpMethod.GET, "/");
        when(e.getMessage()).thenReturn(request);
        
        InetSocketAddress mockAddress = createIpAddress();
        when(e.getRemoteAddress()).thenReturn(mockAddress);
        
        logger.handleUpstream(ctx, e);
        
        assertEquals("192.168.0.3", logger.getInetAddress());
    }
    
    @Test
    public void testIpAddressIsSetFromHeader() throws Exception {
        HttpRequestLogger logger = new HttpRequestLogger("nofile");
        
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        MessageEvent e = mock(MessageEvent.class);
        DefaultHttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
            HttpMethod.GET, "/");
        when(e.getMessage()).thenReturn(request);
           
        request.addHeader("X-Forwarded-For", "192.168.0.7");
        
        InetSocketAddress mockAddress = createIpAddress();
        when(e.getRemoteAddress()).thenReturn(mockAddress);
        
        logger.handleUpstream(ctx, e);
        
        assertEquals("192.168.0.7", logger.getInetAddress());
    }

    @Test
    public void testIpAddressIsSetFromHeaderWithProxyChain() throws Exception {
        HttpRequestLogger logger = new HttpRequestLogger("nofile");
        
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        MessageEvent e = mock(MessageEvent.class);
        DefaultHttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
            HttpMethod.GET, "/");
        when(e.getMessage()).thenReturn(request);
           
        request.addHeader("X-Forwarded-For", "192.168.0.7, 24.222.0.22");
        
        InetSocketAddress mockAddress = createIpAddress();
        when(e.getRemoteAddress()).thenReturn(mockAddress);
        
        logger.handleUpstream(ctx, e);
        
        assertEquals("192.168.0.7", logger.getInetAddress());
    }

    @Test
    public void testIpAddressStillUsedIfEmptyProxyHeader() throws Exception {
        HttpRequestLogger logger = new HttpRequestLogger("nofile");
        
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        MessageEvent e = mock(MessageEvent.class);
        DefaultHttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
            HttpMethod.GET, "/");
        when(e.getMessage()).thenReturn(request);
           
        request.addHeader("X-Forwarded-For", "");
        
        InetSocketAddress mockAddress = createIpAddress();
        when(e.getRemoteAddress()).thenReturn(mockAddress);
        
        logger.handleUpstream(ctx, e);
        
        assertEquals("192.168.0.3", logger.getInetAddress());
    }

    
    
    /**
     * @return
     * @throws UnknownHostException
     */
    private InetSocketAddress createIpAddress() throws UnknownHostException {
        // declaring an ip address is much harder than it should be!
        InetSocketAddress mockAddress = new InetSocketAddress(
            InetAddress.getByAddress(
                new byte[] {-64, -88, 0x0, 3}),
            8888);
        return mockAddress;
    }
}
