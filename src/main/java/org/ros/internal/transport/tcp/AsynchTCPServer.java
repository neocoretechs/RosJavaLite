package org.ros.internal.transport.tcp;
import java.net.*;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.io.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
* AsynchTCPServer is the superclass of all objects using AsynchServerSockets.
*/
public abstract class AsynchTCPServer implements Cloneable, Runnable {
	private static boolean DEBUG = true;
	private static final Log log = LogFactory.getLog(AsynchTCPServer.class);
	AsynchronousServerSocketChannel server = null;
	AsynchronousChannelGroup channelGroup;
	AsynchronousSocketChannel data = null;
	Executor exc;
	boolean shouldStop = false;
	public synchronized void startServer(AsynchronousChannelGroup group, Executor exc, int port) throws IOException {
		this.exc = exc;
		if( server == null ) {
			if( DEBUG )
				log.info("AsynchTCPServer attempt local bind port "+port);
			channelGroup = group;
			 server = AsynchronousServerSocketChannel.open(channelGroup);
			 server.bind(new InetSocketAddress(port));
			exc.execute(this);
		}
	}
	public synchronized void startServer(AsynchronousChannelGroup group, Executor exc, InetSocketAddress binder) throws IOException {
		if( server == null ) {
			if( DEBUG )
				log.info("AsynchTCPServer attempt bind "+binder);
			channelGroup = group;
			server = AsynchronousServerSocketChannel.open(channelGroup);
			server.bind(binder);
			exc.execute(this);
		}
	}
	public synchronized void stopServer() throws IOException {
		if( server != null ) {
			shouldStop = true;
			server.close();
			server = null;
		}
	}

    public void reInit() throws IOException {
         	if( data != null ) data.close();
    }
}	

