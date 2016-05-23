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
	
	
	public AsynchBaseServer(TcpRosServer server) throws IOException {
		super();
		this.address = server.getAddress();
		this.tcpserver = server;
	}
	/**
	 * Construct the Server, fill in port and address later.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public AsynchBaseServer() throws IOException, ClassNotFoundException {
		super();
	}

	public void startServer() throws IOException {
		if( address == null )
			throw new IOException("Server address not defined, can not start Base Server");
		startServer(channelGroup, exc, address);
	}
	/**
	 * Start the server
	 * @param args
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception {
		AsynchBaseServer bs = new AsynchBaseServer();
		log.info("ROSLite Asynch transport Server started on "+InetAddress.getLocalHost().getHostName()+" port "+bs.WORKBOOTPORT);
	}
	
	public void run() {
		while(!shouldStop) {
			try {
				Future<AsynchronousSocketChannel> channel = server.accept();
				if( DEBUG ) {
					log.info("Accept "+channel);
				}
				ChannelHandlerContext ctx = new ChannelHandlerContextImpl(channelGroup, channel.get(), exc);
				tcpserver.getSubscribers().add(ctx);
				ctx.pipeline().fireChannelActive();
				// inject the handlers, start handshake
				tcpserver.getFactoryStack().inject(ctx);
				ctx.pipeline().fireChannelRegistered(); 
                AsynchTCPWorker uworker = new AsynchTCPWorker(ctx, channel.get());
                exc.execute(uworker);  
                if( DEBUG ) {
                    	log.info("ROS Asynch transport server worker starting");
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
