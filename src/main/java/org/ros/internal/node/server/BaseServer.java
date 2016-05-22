package org.ros.internal.node.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Functionally this class Extends TCPServer, takes connections and spins the worker thread to handle each one 
 * @author jg Copyright (C) NeoCoreTechs 2015
 *
 */
public final class BaseServer extends TCPServer {
	private static boolean DEBUG = true;
    private static final Log log = LogFactory.getLog(BaseServer.class);
	public int WORKBOOTPORT = 8090;
	public InetAddress address = null;
	private RpcServer rpcserver = null;
	
	
	public BaseServer(RpcServer server) throws IOException {
		super();
		WORKBOOTPORT = server.getAddress().getPort();
		this.address = server.getAddress().getAddress();
		this.rpcserver = server;
	}
	/**
	 * Construct the Server, fill in port and address later.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public BaseServer() throws IOException, ClassNotFoundException {
		super();
	}

	public void startServer() throws IOException {
		if( address == null )
			throw new IOException("Server address not defined, can not start Base Server");
		startServer(WORKBOOTPORT, address);
	}
	/**
	 * Start the server
	 * @param args
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception {
		BaseServer bs = new BaseServer();
		log.info("ROSLite Server started on "+InetAddress.getLocalHost().getHostName()+" port "+bs.WORKBOOTPORT);
	}
	
	public void run() {
		while(!shouldStop) {
				try {
					Socket datasocket = server.accept();
                    // disable Nagles algoritm; do not combine small packets into larger ones
                    datasocket.setTcpNoDelay(true);
                    // wait 1 second before close; close blocks for 1 sec. and data can be sent
                    datasocket.setSoLinger(true, 1);
					//
    
                    TCPWorker uworker = new TCPWorker(datasocket, rpcserver);
                    ThreadPoolManager.getInstance().spin(uworker);
                    
                    if( DEBUG ) {
                    	log.info("ROS Server node worker starting");
                    }
                    
				} catch(IOException e) {
                    log.error("Server socket accept exception "+e,e);
               }
		}
	
	}
	
	public Integer getPort() {
		return WORKBOOTPORT;
	}

	public String toString() {
		return "BaseServer for "+ rpcserver;
	}

}
