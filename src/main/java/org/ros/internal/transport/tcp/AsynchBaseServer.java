package org.ros.internal.transport.tcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.internal.transport.ChannelHandlerContext;
import org.ros.internal.transport.ChannelHandlerContextImpl;

/**
 * Functionally this class Extends AsynchTCPServer, takes connections and spins the worker thread to handle each one 
 * @author jg Copyright (C) NeoCoreTechs 2016
 *
 */
public final class AsynchBaseServer extends AsynchTCPServer {
	private static boolean DEBUG = true;
    private static final Log log = LogFactory.getLog(AsynchBaseServer.class);
	public int WORKBOOTPORT = 0;
	public InetSocketAddress address = null;
	private TcpRosServer tcpserver = null;
	
	
	public AsynchBaseServer(TcpRosServer tcpserver) throws IOException {
		super();
		this.address = tcpserver.getAddress();
		this.tcpserver = tcpserver;
		this.exc = tcpserver.getExecutor();
		this.channelGroup = tcpserver.getChannelGroup();
	}

	public void startServer() throws IOException {
		if( address == null )
			throw new IOException("Server address not defined, can not start Base Server");
		startServer(channelGroup, exc, address);
	}

	
	public void run() {
		while(!shouldStop) {
			try {
				Future<AsynchronousSocketChannel> channel = server.accept();
				if( DEBUG ) {
					log.info("Accept "+channel.get());
				}
				ChannelHandlerContext ctx = new ChannelHandlerContextImpl(channelGroup, channel.get(), exc);
				tcpserver.getSubscribers().add(ctx);
				ctx.pipeline().fireChannelActive();
				// inject the handlers, start handshake
				tcpserver.getFactoryStack().inject(ctx);
				// We have to set context as ready AFTER we do the handshake to keep traffic from
				// interfering with handshake
				// notify channel up and ready
				ctx.pipeline().fireChannelRegistered(); 
				// spin worker to handle traffic from asynch socket
                AsynchTCPWorker uworker = new AsynchTCPWorker(ctx);
                // and send it all to executor for running
                exc.execute(uworker);  
                if( DEBUG ) {
                    	log.info("ROS Asynch transport server worker starting for context:"+ctx);
                }   
			} catch(Exception e) {
                    log.error("Asynch Server socket accept exception "+e,e);
            } 
		}
	
	}
	
	public Integer getPort() {
		return WORKBOOTPORT;
	}

	public String toString() {
		return "AsynchBaseServer for "+ tcpserver;
	}

}
