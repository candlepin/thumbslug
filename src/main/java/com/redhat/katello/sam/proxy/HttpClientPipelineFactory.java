package com.redhat.katello.sam.proxy;

import static org.jboss.netty.channel.Channels.*;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.http.HttpClientCodec;
import org.jboss.netty.handler.codec.http.HttpContentDecompressor;

public class HttpClientPipelineFactory implements ChannelPipelineFactory {

    private Channel client;

    public HttpClientPipelineFactory(Channel client) {
        this.client = client;
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = pipeline();
        pipeline.addLast("codec", new HttpClientCodec());

        // Remove the following line if you don't want automatic content
        // decompression.
        pipeline.addLast("inflater", new HttpContentDecompressor());

        // Uncomment the following line if you don't want to handle HttpChunks.
        // pipeline.addLast("aggregator", new HttpChunkAggregator(1048576));

        pipeline.addLast("handler", new HttpRelayingResponseHandler(client));
        return pipeline;
    }

}
