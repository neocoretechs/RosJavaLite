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



import org.ros.internal.node.response.VoidResultFactory;
import org.ros.internal.node.server.RemoteRequestInterface;
import org.ros.internal.node.server.ThreadPoolManager;

/**
 * This class functions as client to the  remote node.
 * @author jg
 * Copyright (C) NeoCoreTechs 2014,2015
 */
public class RemoteClient implements Runnable {
	private static final boolean DEBUG = false;
	public static final boolean TEST = false; // true to run in local cluster test mode
	
	private int remotePort; // temp master port, accepts connection from remote server
	
	private InetAddress IPAddress = null;

	private Socket workerSocket = null; // socket assigned to slave port
	private SocketAddress workerSocketAddress; //address of slave

	private boolean shouldRun = true; // master service thread control
	private Object waitHalt = new Object(); 
	
	private VoidResultFactory handleNull = new VoidResultFactory();
	
	ArrayBlockingQueue<RemoteRequestInterface> requests = new ArrayBlockingQueue<RemoteRequestInterface>(1);
	ArrayBlockingQueue<Object> responses = new ArrayBlockingQueue<Object>(1);
	
	
	/**
	 * Start a relatrix client. Contact the boot time portion of server and queue a CommandPacket to open the desired
	 * database and get back the master and slave ports of the remote server. The main client thread then
	 * contacts the server master port, and the remote slave port contacts the master of the client. A WorkerRequestProcessor
	 * thread is created to handle the processing of payloads and a comm thread handles the bidirectional traffic to server
	 * @param dbName The name of the remote DB in full qualified path form
	 * @param remote The remote database name for cluster server
	 * @param bootNode The name of the remote server host
	 * @param bootPort Then name of the remote host port on which RelatrixServer is running
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
			System.out.println("RemoteClient constructed with Boot:"+IPAddress);
		}
		remotePort = bootPort;
		// spin up 'this' to receive connection request from remote server 'slave' to our 'master'
		ThreadPoolManager.getInstance().spin(this);
	}
	
	
	/**
	 * Look for messages coming back from the workers. Extract the UUID of the returned packet
	 * and get the real request from the ConcurrentHashTable buffer
	 */
	@Override
	public void run() {
 
		while(shouldRun ) {
			try {
				// sets workerSocket
				send(requests.take());
				InputStream ins = workerSocket.getInputStream();
				ObjectInputStream ois = new ObjectInputStream(ins);
				Object ret = ois.readObject();
				if( ret == null )
					ret = new Object(); // let voidResultFactory handle this, we cant put null to queue
				if( DEBUG )
					 System.out.println("FROM Remote, response:"+ret);
				 responses.put(ret);
			} catch (SocketException e) {
					System.out.println("RemoteClient: receive socket error "+e+" Address:"+IPAddress+" port:"+remotePort);
					break;
			} catch (IOException e) {
				// we lost the remote, try to close worker and wait for reconnect
				System.out.println("RemoteClient: receive IO error "+e+" Address:"+IPAddress+" port:"+remotePort);
			} catch (ClassNotFoundException e1) {
				System.out.println("Class not found for deserialization "+e1+" Address:"+IPAddress+" port:"+remotePort);
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
				System.out.println("Exception setting up socket to remote host:"+IPAddress+" port "+remotePort+" "+e);
		} catch (IOException e) {
				System.out.println("Socket send error "+e+" to address "+IPAddress+" on port "+remotePort);
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
