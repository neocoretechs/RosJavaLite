package org.ros.internal.node.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Functionally this class Extends TCPServer, takes connections and spins the worker thread to handle each one.<p/>
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2021
 *
 */
public final class BaseServer extends TCPServer {
	private static boolean DEBUG = false;
    private static final Log log = LogFactory.getLog(BaseServer.class);
    private static final String WORKERTHREADS = "WORKER";
	public int WORKBOOTPORT = 8090;
	public InetAddress address = null;
	private RpcServer rpcserver = null;
	private ConcurrentHashMap<TCPWorker, Future<?>> uworkers = new ConcurrentHashMap<TCPWorker,Future<?>>();
	
	public BaseServer(RpcServer server) throws IOException {
		super();
		WORKBOOTPORT = server.getAddress().getPort();
		this.address = server.getAddress().getAddress();
		this.rpcserver = server;
		SynchronizedThreadManager.getInstance().init(new String[]{WORKERTHREADS});
	}
	/**
	 * Construct the Server, fill in port and address later.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public BaseServer() throws IOException, ClassNotFoundException {
		super();
		SynchronizedThreadManager.getInstance().init(new String[]{WORKERTHREADS});
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
		SynchronizedThreadManager.startSupervisorThread();
	}
	
	public void run() {
		while(!shouldStop) {
			try {
				Socket datasocket = server.accept();
				// disable Nagles algoritm; do not combine small packets into larger ones
				//datasocket.setTcpNoDelay(true);
				// wait 1 second before close; close blocks for 1 sec. and data can be sent
				datasocket.setSoLinger(true, 1);
				//
				TCPWorker uworker = new TCPWorker(datasocket, rpcserver);
				Future<?> newworker= SynchronizedThreadManager.getInstance().submit(uworker,WORKERTHREADS);
				uworkers.put(uworker, newworker);
				if( DEBUG ) {
					log.info("ROS Server node worker starting");
				}

			} catch(IOException e) {
				log.error("Server socket accept exception "+e,e);
			}
		}

	}

	/**
	 * Shut down the TCPWorker, should it have been started. 
	 * @throws IOException 
	 */
	public void shutdown() throws IOException {
		Enumeration<TCPWorker> workers = uworkers.keys();
		while(workers.hasMoreElements()) {
			close(workers.nextElement());
		}
		SynchronizedThreadManager.getInstance().shutdown(WORKERTHREADS);
		super.shutdown();
	}
	
	/**
	 * Shut down a specific TCPworker after socket disconnect
	 * @param tcpworker
	 */
	public void close(TCPWorker tcpworker) {
		tcpworker.shouldRun = false;
		uworkers.get(tcpworker).cancel(true);
		uworkers.remove(tcpworker);
	}
	
	public Integer getPort() {
		return WORKBOOTPORT;
	}

	public String toString() {
		return "BaseServer for "+ rpcserver;
	}

}
