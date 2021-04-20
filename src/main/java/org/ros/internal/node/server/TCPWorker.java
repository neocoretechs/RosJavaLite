package org.ros.internal.node.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This TCPWorker is spawned for servicing traffic from a master or slave node and invoking methods thereupon.<p/>
 * Constructed with Socket and RpcServer, creates an ObjectInputStream from datasocket Socket and queues a response
 * on the Objectoutputstream.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2021
 *
 */
public class TCPWorker implements Runnable {
	private static final boolean DEBUG = false;
	private static final Log log = LogFactory.getLog(TCPWorker.class);
	public volatile boolean shouldRun = true;
	private Socket dataSocket;
	private RpcServer server; // the server we are servicing
	
    public TCPWorker(Socket datasocket, RpcServer server) throws IOException {
    	this.dataSocket = datasocket;
    	this.server = server;
    	if( DEBUG )
    		log.info("TCPWorker constructed with socket "+dataSocket+" for server "+server);
	}
	
	/**
	 * Queue a request on this worker,
	 * Instead of queuing to a running thread request queue, queue this for outbound message
	 * The type is RemoteCompletionInterface and contains the Id and the payload
	 * back to master
	 * @param res
	 */
	public void queueResponse(Object res) {
	
		if( DEBUG ) {
			log.debug("Adding response "+res+" to outbound from worker");
		}
		try {
			// Write response to master for forwarding to client
			OutputStream os = dataSocket.getOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(res);
			oos.flush();
		} catch (IOException e) {
				log.error("Exception writing socket to remote master port "+e);
				throw new RuntimeException(e);
		}
	}

	/**
	 * Client (Slave port) sends data to our master in the following loop
	 */
	@Override
	public void run() {
			try {
				while(shouldRun) {
					ObjectInputStream ois = new ObjectInputStream(dataSocket.getInputStream());
					RemoteRequestInterface o = (RemoteRequestInterface) ois.readObject();
					if( DEBUG )
						log.debug("ROS TCPWorker for "+server+" at address "+dataSocket+" command received:"+o);
					Object res = server.invokeMethod(o);
					queueResponse(res);
				}
			} catch(Exception se) {
				if( se instanceof SocketException ) {
					log.error("Received SocketException, connection reset..");
				} else {
					log.error("Remote invocation failure ",se);
				}
			} finally {
				close();
			}

	}
	
	private void close() {
		shouldRun = false;
		if(dataSocket != null)
			try {
				dataSocket.close();
			} catch (IOException e) {}
		server.close(this);
	}
	
	public void shutdown() {
		shouldRun = false;
		if(dataSocket != null)
			try {
				dataSocket.close();
			} catch (IOException e) {}
		try {
			server.shutdown();
		} catch (IOException e) {}
	}

}
