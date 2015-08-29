package org.ros.internal.node.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

/**
 * This TCPWorker is spawned for servicing traffic from a master or slave node and invoking methods thereupon.
 * @author jg
 * Copyright (C) NeoCoreTechs 2015
 *
 */
public class TCPWorker implements Runnable {
	private static final boolean DEBUG = true;
	public boolean shouldRun = true;
	private Socket dataSocket;
	private Object waitHalt = new Object(); 
	private RpcServer server; // the server we are servicing
	
    public TCPWorker(Socket datasocket, RpcServer server) throws IOException {
    	this.dataSocket = datasocket;
    	this.server = server;
    	if( DEBUG )
    		System.out.println("TCPWorker constructed with socket "+dataSocket+" for server "+server);
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
			System.out.println("Adding response "+res+" to outbound from worker");
		}
		try {
			// Write response to master for forwarding to client
			OutputStream os = dataSocket.getOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(res);
			oos.flush();
		} catch (IOException e) {
				System.out.println("Exception writing socket to remote master port "+e);
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
						System.out.println("ROS TCPWorker for "+server+" at address "+dataSocket+" command received:"+o);
					Object res = server.invokeMethod(o);
					queueResponse(res);
				}
				dataSocket.close();
			} catch(Exception se) {
				if( se instanceof SocketException ) {
					System.out.println("Received SocketException, connection reset..");
				} else {
					System.out.println("Remote invocation failure "+se);
					se.printStackTrace();
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
