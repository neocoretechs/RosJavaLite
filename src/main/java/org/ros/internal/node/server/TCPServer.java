package org.ros.internal.node.server;
import java.net.*;
import java.io.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
* TCPServer is the superclass of all objects using ServerSockets.
* @author Jonathan Groff Copyright (C) NeoCoreTechs 2015m2021
*/
public abstract class TCPServer implements Cloneable, Runnable {
	private static boolean DEBUG = false;
	private static final Log log = LogFactory.getLog(TCPServer.class);
	ServerSocket server = null;
	volatile boolean shouldStop = false;
	public synchronized void startServer(int port) throws IOException {
		if( server == null ) {
			server = new ServerSocket(port);
			//runner = new Thread(this);
			//runner.start();
			SynchronizedThreadManager.getInstance().init(new String[]{"TCPSERVER","WORKERS"}, false);
			SynchronizedThreadManager.getInstance().spin(this,"TCPSERVER");
		}
	}
	public synchronized void startServer(int port, InetAddress binder) throws IOException {
		if( server == null ) {
			if( DEBUG )
				log.info("TCPServer attempt local bind "+binder+" port "+port);
			server = new ServerSocket(port, 1000, binder);
			//runner = new Thread(this);
			//runner.start();
			SynchronizedThreadManager.getInstance().init(new String[]{"TCPSERVER","WORKERS"}, false);
			SynchronizedThreadManager.getInstance().spin(this,"TCPSERVER");
		}
	}
	public synchronized void stopServer() throws IOException {
		if( server != null ) {
			shouldStop = true;
			server.close();
			server = null;
			SynchronizedThreadManager.getInstance().shutdown("TCPSERVER");
			SynchronizedThreadManager.getInstance().shutdown("WORKERS");
		}
	}

	public void shutdown() throws IOException {
		stopServer();
	}
}	

