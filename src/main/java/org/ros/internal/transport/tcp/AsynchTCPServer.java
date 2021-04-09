package org.ros.internal.transport.tcp;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
//import java.nio.channels.AsynchronousChannelGroup;
//import java.nio.channels.AsynchronousServerSocketChannel;
//import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
* AsynchTCPServer is the superclass of all objects using AsynchServerSockets.<p/>
* Extended by AsynchBaseServer which takes a TcpRosServer and does a ServerSocket.accept<p/>
* @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2021
*/
public abstract class AsynchTCPServer implements Cloneable, Runnable {
	private static boolean DEBUG = false;
	private static final Log log = LogFactory.getLog(AsynchTCPServer.class);
	//AsynchronousServerSocketChannel server = null;
	//AsynchronousChannelGroup channelGroup;
	//AsynchronousSocketChannel data = null;
	ServerSocket server = null;
	ChannelGroup channelGroup;

	volatile boolean shouldStop = false;
	/**
	 * Construct a ServerSocket bound to the specified port using the supplied channel group.<p/>
	 * The primary purpose of the channel group is to provide the executor to start the server.
	 * @param group
	 * @param port
	 * @throws IOException
	 */
	public synchronized void startServer(ChannelGroup group, int port) throws IOException {	
		if( server == null ) {
			if( DEBUG )
				log.info("AsynchTCPServer attempt local bind port "+port);
			channelGroup = group;
			//server = AsynchronousServerSocketChannel.open(channelGroup);
			server = new ServerSocket();//channelGroup);
			server.bind(new InetSocketAddress(port));
			group.getExecutorService().execute(this);
		}
	}
	//public synchronized void startServer(AsynchronousChannelGroup group, InetSocketAddress binder) throws IOException {
	public synchronized void startServer(ChannelGroup group, InetSocketAddress binder) throws IOException {
		if( server == null ) {
			if( DEBUG )
				log.info("AsynchTCPServer attempt bind "+binder);
			channelGroup = group;
			server = new ServerSocket();
			server.bind(binder);
			group.getExecutorService().execute(this);
		}
	}
	
	public synchronized void shutdown() throws IOException {
		if( server != null ) {
			shouldStop = true;
			server.close();
			server = null;
		}
	}


}	

