package org.ros.internal.node.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.internal.node.server.RemoteRequestInterface;
import org.ros.internal.node.server.ThreadPoolManager;


/**
 * This class functions as client to the remote nodes.<p>
 * It functions as a generic serialization queue and wire level socket request/response processor.<br>
 * MasterRpcEndpoint has one of these each for master and parameter server.<br>
 * RelatrixServerRpcEndpoint has one for contacting the RelatrixTransactionServer.<br>
 * SlaveRpcEndpoint has but one for contacting the remote master and issuing commands via the remote invokable methods.<br>
 * The transport is accomplished via the RemoteRequestinterface implementors.<br>
 * {@link org.ros.internal.node.rpc.MasterRpcEndpointImpl}<br>
 * {@link org.ros.internal.node.rpc.SlaveRpcEndpointImpl}<br>
 * {@link org.ros.internal.node.rpc.RelatrixServerRpcEndpointImpl}<br>
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2014,2015,2021
 */
public class RemoteClient implements Runnable {
	private static final boolean DEBUG = false;
	public static final boolean TEST = false; // true to run in local cluster test mode
	private static final Log log = LogFactory.getLog(RemoteClient.class);
	private int remotePort; // temp master port, accepts connection from remote server
	
	private InetAddress IPAddress = null;

	private Socket workerSocket = null; // socket assigned to slave port
	private SocketAddress workerSocketAddress; //address of slave

	private volatile boolean shouldRun = true; // master service thread control
	private Object waitHalt = new Object(); 
	
	ArrayBlockingQueue<RemoteRequestInterface> requests = new ArrayBlockingQueue<RemoteRequestInterface>(1);
	ArrayBlockingQueue<Object> responses = new ArrayBlockingQueue<Object>(1);
	
	
	/**
	 * Start a remote client.
	 */
	public RemoteClient(String bootNode, int bootPort)  throws IOException {
		if( TEST ) {
			IPAddress = InetAddress.getLocalHost();
		} else {
			if( bootNode != null ) {
				IPAddress = InetAddress.getByName(bootNode);
			} else {
				throw new IOException("Remote node is null for RemoteClient");
			}
		}
		if( DEBUG ) {
			log.debug("RemoteClient constructed with Boot:"+IPAddress);
		}
		remotePort = bootPort;
		// spin up 'this' to receive connection request from remote server 'slave' to our 'master'
		ThreadPoolManager.getInstance().spin(this);
	}
	
	/**
	 * Look for messages coming back from the workers. 
	 */
	@Override
	public void run() {
 
		while(shouldRun) {
			try {
				// sets workerSocket
				send(requests.take());
				InputStream ins = workerSocket.getInputStream();
				ObjectInputStream ois = new ObjectInputStream(ins);
				Object ret = ois.readObject();
				if( ret == null )
					ret = new Object(); // let voidResultFactory handle this, we cant put null to queue
				if( DEBUG )
					 log.debug("FROM Remote, response:"+ret);
				 responses.put(ret);
			} catch (SocketException e) {
					log.error("RemoteClient: receive socket error "+e+" Address:"+IPAddress+" port:"+remotePort);
					try {
						if( workerSocket != null ) workerSocket.close();
					} catch (IOException e2) {}
					workerSocket = null;
			} catch (IOException e) {
				// we lost the remote, try to close worker and wait for reconnect
				log.debug("RemoteClient: receive IO error "+e+" Address:"+IPAddress+" port:"+remotePort);
				try {
					if( workerSocket != null ) workerSocket.close();
				} catch (IOException e2) {}
				workerSocket = null;
			} catch (ClassNotFoundException e1) {
				log.error("Class not found for deserialization "+e1+" Address:"+IPAddress+" port:"+remotePort);
				break;
			} catch (InterruptedException e) {
				break;
			}
	    }	// shouldRun
		try {
				if( workerSocket != null ) workerSocket.close();
		} catch (IOException e2) {}
		synchronized(waitHalt) {
				waitHalt.notify();
		}
	}
	
	public Object queue(RemoteRequestInterface rri) {
		try {
			requests.put(rri);
			return responses.take();
		} catch (InterruptedException e) {
			return null;
		}
	}
	
	/**
	 * Send request to remote worker, if workerSocket is null open SLAVEPORT connection to remote master
	 * @param iori
	 */
	private void send(RemoteRequestInterface iori) {
		try {
			if(workerSocket == null ) {
				workerSocketAddress = new InetSocketAddress(IPAddress, remotePort);
				workerSocket = new Socket();
				workerSocket.connect(workerSocketAddress);
				workerSocket.setKeepAlive(true);
				//workerSocket.setTcpNoDelay(true);
				workerSocket.setReceiveBufferSize(32767);
				workerSocket.setSendBufferSize(32767);
			}
			ObjectOutputStream oos = new ObjectOutputStream(workerSocket.getOutputStream());
			oos.writeObject(iori);
			oos.flush();
			
		} catch (SocketException e) {
				log.error("Exception setting up socket to remote host:"+IPAddress+" port "+remotePort+" "+e);
		} catch (IOException e) {
				log.error("Socket send error "+e+" to address "+IPAddress+" on port "+remotePort);
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
