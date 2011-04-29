package com.redhat.katello.sam.proxy;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int port = 8088;
        // Configure the server.
		          ServerBootstrap bootstrap = new ServerBootstrap(
		                  new NioServerSocketChannelFactory(
		                          Executors.newCachedThreadPool(),
		                          Executors.newCachedThreadPool()));
		  
		          // Set up the event pipeline factory.
		          bootstrap.setPipelineFactory(new HttpServerPipelineFactory());
		  
		          // Bind and start to accept incoming connections.
		          bootstrap.bind(new InetSocketAddress(port));
		          System.out.println("Running SAM proxy on port " + port);
	}

}
