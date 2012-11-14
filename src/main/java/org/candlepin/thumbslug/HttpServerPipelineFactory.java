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

import java.util.concurrent.Executor;

import javax.net.ssl.SSLEngine;

import org.candlepin.thumbslug.ssl.SslContextFactory;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.ssl.SslHandler;

/**
 * HttpServerPipelineFactory
 */
public class HttpServerPipelineFactory implements ChannelPipelineFactory {

    private Config config;
    private ChannelFactory channelFactory;
    private HttpCdnClientChannelFactory httpClientPipelineFactory;
    private Executor eventExecutor;

    public HttpServerPipelineFactory(Config config, Executor executor,
        Executor eventExecutor) {
        this.config = config;
        this.eventExecutor = eventExecutor;

        channelFactory = new NioClientSocketChannelFactory(executor, executor);
        httpClientPipelineFactory = new HttpCdnClientChannelFactory(config, channelFactory);
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        // Create a default pipeline implementation.
        ChannelPipeline pipeline = pipeline();

        if (config.getBoolean("ssl")) {
            SSLEngine engine =
                SslContextFactory.getServerContext(
                    config.getProperty("ssl.keystore"),
                    config.getProperty("ssl.keystore.password"),
                    config.getProperty("ssl.ca.keystore")).createSSLEngine();
            engine.setUseClientMode(false);
            engine.setNeedClientAuth(true);
            pipeline.addLast("ssl", new SslHandler(engine));
        }

        pipeline.addLast("decoder", new HttpRequestDecoder());
        // Uncomment the following line if you don't want to handle HttpChunks.
        // pipeline.addLast("aggregator", new HttpChunkAggregator(1048576));
        pipeline.addLast("encoder", new HttpResponseEncoder());

        // we're explicitly not compressing here; the CDN takes care of that.
        // pipeline.addLast("deflater", new HttpContentCompressor());

        pipeline.addLast("logger", new HttpRequestLogger(config.getProperty("log.access")));
        pipeline.addLast("pipelineExecutor", new ExecutionHandler(eventExecutor));
        pipeline.addLast("ping", new PingHandler(config, channelFactory));
        pipeline.addLast("handler", new HttpRequestHandler(config,
                                            httpClientPipelineFactory, channelFactory));
        return pipeline;
    }
}
