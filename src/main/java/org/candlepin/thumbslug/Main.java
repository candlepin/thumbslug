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

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import org.apache.log4j.Logger;

/**
 * Main
 */
public class Main {
    
    private static Logger log = Logger.getLogger(Main.class);
    
    // maintain a list of open channels so we can shut them down on app exit
    static final ChannelGroup ALL_CHANNELS = new DefaultChannelGroup("thumbslug");

    private Main() {
        // silence checkstyle
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        Config config = new Config();
        int port = config.getInt("port");
        log.warn("HELLO!");

        // Configure the server.
        ServerBootstrap bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool()));

        // Set up the event pipeline factory.
        bootstrap.setPipelineFactory(new HttpServerPipelineFactory(config));

        // Bind and start to accept incoming connections.
        Channel channel = bootstrap.bind(new InetSocketAddress(port));
        System.out.println("Running Thumbslug on port " + port);

        ALL_CHANNELS.add(channel);
        
        //intercept shutdown signal from VM and shut-down gracefully. 
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() { 
            public void run() { 
                System.out.println("Shutting down...");
                ChannelGroupFuture future = ALL_CHANNELS.close();
                future.awaitUninterruptibly();
            } 
        }, "shutdownHook")); 
    }

}
