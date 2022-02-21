package org.ros.internal.transport.tcp;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
//import java.nio.channels.AsynchronousChannelGroup;
//import java.nio.channels.AsynchronousServerSocketChannel;
//import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
* AsynchTCPServer is the superclass of all objects using AsynchServerSockets.<p/>
* Extended by AsynchBaseServer which takes a TcpRosServer and does a ServerSocket.accept<p/>
* The executor service is shut down here.
* @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2021
*/
public abstract class AsynchTCPServer implements Cloneable, Runnable {
	private static boolean DEBUG = false;
	private static final Log log = LogFactory.getLog(AsynchTCPServer.class);
	//AsynchronousServerSocketChannel server = null;
	//AsynchronousChannelGroup channelGroup;
	//AsynchronousSocketChannel data = null;
	ServerSocket server = null;
	ExecutorService executor;

	volatile boolean shouldStop = false;
	/**
	 * Construct a ServerSocket bound to the specified port using the supplied channel group.<p/>
	 * The primary purpose of the channel group is to provide the executor to start the server.
	 * @param executor The executor for task running
	 * @param port
	 * @throws IOException
	 */
	public synchronized void startServer(Executor executor, int port) throws IOException {	
		if( server == null ) {
			if( DEBUG )
				log.info("AsynchTCPServer attempt local bind port "+port);
			this.executor = (ExecutorService) executor;
			//server = AsynchronousServerSocketChannel.open(channelGroup);
			server = new ServerSocket();//channelGroup);
			server.bind(new InetSocketAddress(port));
			executor.execute(this);
		}
	}
	//public synchronized void startServer(AsynchronousChannelGroup group, InetSocketAddress binder) throws IOException {
	public synchronized void startServer(Executor executor, InetSocketAddress binder) throws IOException {
		if( server == null ) {
			if( DEBUG )
				log.info("AsynchTCPServer attempt bind "+binder);
			this.executor = (ExecutorService) executor;
			server = new ServerSocket();
			server.bind(binder);
			executor.execute(this);
		}
	}
	
	public synchronized void shutdown() throws IOException {
		if( server != null ) {
			shouldStop = true;
			server.close();
			server = null;
			((ExecutorService)executor).shutdown();
		}
	}


}	

