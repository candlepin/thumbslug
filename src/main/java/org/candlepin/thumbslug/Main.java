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
import java.util.concurrent.Executor;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.candlepin.thumbslug.ssl.SslContextFactory;
import org.candlepin.thumbslug.ssl.SslKeystoreException;
import org.candlepin.thumbslug.ssl.SslPemException;

import com.sun.akuma.Daemon;

/**
 * Main
 */
public class Main {

    private static Logger log = Logger.getLogger(Main.class);

    private static final int ERROR_DAEMON_INIT = -1;
    private static final int ERROR_DAEMON_DAEMONIZE = -2;
    private static final int ERROR_CONFIGURE_SSL = -3;
    private static final int ERROR_NO_CONFIG = -4;

    // maintain a list of open channels so we can shut them down on app exit
    static final ChannelGroup ALL_CHANNELS = new DefaultChannelGroup("thumbslug");

    private Main() {
        // silence checkstyle
    }

    /**
     * Do an initial bootstrap setup of the server SSL Context, so we can shake out any
     * errors early, and abort if needed.
     *
     * @param config our Config
     */
    private static boolean configureSSL(Config config) {
        if (!config.getBoolean("ssl")) {
            return true;
        }

        try {
            SslContextFactory.getServerContext(config.getProperty("ssl.keystore"),
                config.getProperty("ssl.keystore.password"),
                config.getProperty("ssl.ca.keystore"));
            return true;
        }
        catch (SslKeystoreException e) {
            log.error("Unable to load the ssl keystore. " +
                "Check that ssl.keystore and ssl.keystore.password are set correctly.", e);
            return false;
        }
        catch (SslPemException e) {
            log.error("Unable to load the CA certificate. " +
                "Check that ssl.ca.keystore is set correctly.", e);
            return false;
        }
    }

    private static void configureLogging(String fileName, Properties loggingProperties) {
        try {
            Logger.getRootLogger().setLevel(Level.ALL);
            Layout layout = Logger.getRootLogger().getAppender("RootAppender").getLayout();
            FileAppender fileAppender = new FileAppender(layout, fileName);
            Logger.getRootLogger().addAppender(fileAppender);
        }
        catch (Exception e) {
            log.error("unable to open error.log for writing!", e);
            // we'll just ignore this, and allow logging to happen to the cli.
        }


        for (Entry<Object, Object> entry : loggingProperties.entrySet()) {
            String key = (String) entry.getKey();
            Logger.getLogger(key).setLevel(Level.toLevel((String) entry.getValue()));
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        Config config = null;

        try {
            String confFile = Config.CONFIG_FILE;
            if (args.length == 1) {
                confFile = args[0];
            }
            config = new Config(confFile);
        }
        catch (Exception e) {
            log.error("Unable to load config!", e);
            log.warn("Unable to load config! Shutting down...");
            System.exit(ERROR_NO_CONFIG);
        }

        configureLogging(config.getProperty("log.error"), config.getLoggingConfig());

        int port = config.getInt("port");
        boolean shouldDaemonize = config.getBoolean("daemonize");

        Daemon daemon = new Daemon();
        if (daemon.isDaemonized()) {
            try {
                daemon.init(config.getProperty("pid.file"));
                // XXX I am not sure if it is possible to get to this line:
                log.debug("Daemonized");
            }
            catch (Exception e) {
                log.error("Exception caught during daemon initialization!", e);
                log.warn("Error during daemon initialization! Shutting down...");
                System.exit(ERROR_DAEMON_INIT);
            }
        }
        else {
            if (shouldDaemonize) {
                try {
                    log.debug("Daemonizing..");
                    daemon.daemonize();
                    log.debug("Daemonized, exiting");
                }
                catch (IOException e) {
                    log.error("Unable to daemonize properly", e);
                    System.exit(ERROR_DAEMON_DAEMONIZE);
                }
                System.exit(0);
            }
        }

        if (!configureSSL(config)) {
            System.exit(ERROR_CONFIGURE_SSL);
        }

        Executor executor = Executors.newCachedThreadPool();

        // Configure the server.
        ServerBootstrap bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(executor, executor));

        OrderedMemoryAwareThreadPoolExecutor eventExecutor =
            new OrderedMemoryAwareThreadPoolExecutor(16, 0, 0);

        // Set up the event pipeline factory.
        bootstrap.setPipelineFactory(new HttpServerPipelineFactory(config, executor,
            eventExecutor));

        // Bind and start to accept incoming connections.
        Channel channel = bootstrap.bind(new InetSocketAddress(port));
        log.warn("Running Thumbslug on port " + port);

        ALL_CHANNELS.add(channel);

        // intercept shutdown signal from VM and shut-down gracefully.
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                log.warn("Shutting down...");
                ChannelGroupFuture future = ALL_CHANNELS.close();
                future.awaitUninterruptibly();
            }
        }, "shutdownHook"));
    }
}
