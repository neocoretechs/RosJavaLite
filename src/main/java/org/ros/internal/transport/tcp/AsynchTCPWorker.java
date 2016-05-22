package org.ros.internal.transport.tcp;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This AsynchTCPWorker is spawned for servicing traffic from connected nodes.
 * @author jg
 * Copyright (C) NeoCoreTechs 2016
 *
 */
public class AsynchTCPWorker implements Runnable {
	private static final boolean DEBUG = true;
	private static final Log log = LogFactory.getLog(AsynchTCPWorker.class);
	public boolean shouldRun = true;
	private AsynchronousSocketChannel dataSocket;
	private Object waitHalt = new Object(); 
	private TcpRosServer server; // the server we are servicing
	private ByteBuffer buf = ByteBuffer.allocate(2000000);
	
    public AsynchTCPWorker(AsynchronousSocketChannel datasocket, TcpRosServer server) throws IOException {
    	this.dataSocket = datasocket;
    	this.server = server;
    	if( DEBUG )
    		log.info("AsynchTCPWorker constructed with socket channel:"+dataSocket+" for server "+server);
	}
	
	/**
	 * Queue a request on this worker,
	 * Instead of queuing to a running thread request queue, queue this for outbound message
	 * back to master
	 * @param res
	 */
	public void queueResponse(Object res) {
	
		if( DEBUG ) {
			log.debug("Adding response "+res+" to outbound from worker");
		}
		//try {
			// Write response to master for forwarding to client
			//OutputStream os = dataSocket.getOutputStream();
			//ObjectOutputStream oos = new ObjectOutputStream(os);
			//oos.writeObject(res);
			//oos.flush();
		//} catch (IOException e) {
		//		log.error("Exception writing socket to remote master port "+e);
		//		throw new RuntimeException(e);
		//}
	}

	/**
	 * Client (Slave port) sends data to our master in the following loop
	 */
	@Override
	public void run() {

			try {
				while(shouldRun) {
					Future<Integer> read = dataSocket.read(buf);
					if( DEBUG )
						log.debug("ROS AsynchTCPWorker for "+server+" at address "+dataSocket+" command received:"+read.get());
				}
				dataSocket.close();
			} catch(Exception se) {
				if( se instanceof SocketException ) {
					log.error("Received SocketException, connection reset..");
				} else {
					log.error("Remote invocation failure ",se);
				}
			} finally {
				try {
					dataSocket.close();
				} catch (IOException e) {}
			}
			synchronized(waitHalt) {
				waitHalt.notify();
			}

	}
	
	public void close() {
		synchronized(waitHalt) {
			try {
				shouldRun = false;
				waitHalt.wait();
			} catch (InterruptedException ie) {}
		}
	}

}
