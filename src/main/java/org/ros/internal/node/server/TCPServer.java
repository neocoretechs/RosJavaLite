package org.ros.internal.node.server;
import java.net.*;
import java.io.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
* TCPServer is the superclass of all objects using ServerSockets.
*/
public abstract class TCPServer implements Cloneable, Runnable {
	private static boolean DEBUG = false;
	private static final Log log = LogFactory.getLog(TCPServer.class);
	ServerSocket server = null;
	Socket data = null;
	boolean shouldStop = false;
	public synchronized void startServer(int port) throws IOException {
		if( server == null ) {
			server = new ServerSocket(port);
			//runner = new Thread(this);
			//runner.start();
			ThreadPoolManager.init(new String[]{"TCPSERVER"}, false);
			ThreadPoolManager.getInstance().spin(this,"TCPSERVER");
		}
	}
	public synchronized void startServer(int port, InetAddress binder) throws IOException {
		if( server == null ) {
			if( DEBUG )
				log.info("TCPServer attempt local bind "+binder+" port "+port);
			server = new ServerSocket(port, 1000, binder);
			//runner = new Thread(this);
			//runner.start();
			ThreadPoolManager.init(new String[]{"TCPSERVER"}, false);
			ThreadPoolManager.getInstance().spin(this,"TCPSERVER");
		}
	}
	public synchronized void stopServer() throws IOException {
		if( server != null ) {
			shouldStop = true;
			server.close();
			server = null;
			ThreadPoolManager.getInstance().shutdown("TCPSERVER");
		}
	}

    public void reInit() throws IOException {
         	if( data != null ) data.close();
    }
}	

