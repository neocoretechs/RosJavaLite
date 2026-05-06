package org.ros.internal.node.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.StandardSocketOptions;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.internal.node.server.RemoteRequestInterface;
import org.ros.internal.node.server.SynchronizedThreadManager;


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

	private SocketChannel workerSocket = null; // socket assigned to slave port
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
		SynchronizedThreadManager.getInstance().spin(this);
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
				Object ret = receiveObject(workerSocket);
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
					shouldRun = false;
			} catch (IOException e) {
				// we lost the remote, try to close worker and wait for reconnect
				log.debug("RemoteClient: receive IO error "+e+" Address:"+IPAddress+" port:"+remotePort);
				try {
					if( workerSocket != null ) workerSocket.close();
				} catch (IOException e2) {}
				workerSocket = null;
				shouldRun = false;
			} catch (ClassNotFoundException e1) {
				log.error("Class not found for deserialization "+e1+" Address:"+IPAddress+" port:"+remotePort);
				shouldRun = false;
			} catch (InterruptedException e) {
				shouldRun = false;
			}
	    }// shouldRun
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
				if(workerSocket == null || !workerSocket.isOpen() || !workerSocket.isConnected()) {
					workerSocketAddress = new InetSocketAddress(IPAddress, remotePort);
					workerSocket = SocketChannel.open();
					workerSocket.connect(workerSocketAddress);
					workerSocket.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
					//workerSocket.setTcpNoDelay(true);
					workerSocket.setOption(StandardSocketOptions.SO_RCVBUF,32767);
					workerSocket.setOption(StandardSocketOptions.SO_SNDBUF,32767);
				}
				sendObject(workerSocket, iori);
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
	/**
	 * 
	 * @param channel
	 * @param obj
	 * @throws IOException
	 */
	public static void sendObject(SocketChannel channel, Object obj) throws IOException {
	    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
	    try (ObjectOutputStream objOut = new ObjectOutputStream(byteStream)) {
	        objOut.writeObject(obj);
	    }
	    byte[] bytes = byteStream.toByteArray();
	    ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
	    lengthBuffer.putInt(bytes.length);
	    lengthBuffer.flip();
	    while (lengthBuffer.hasRemaining()) {
	        //System.out.println("chan="+channel+" len buf="+lengthBuffer);
	        channel.write(lengthBuffer);
	    }
	    ByteBuffer dataBuffer = ByteBuffer.wrap(bytes);
	    while (dataBuffer.hasRemaining()) {
	    	//System.out.println("chan="+channel+" databuf="+dataBuffer);
	        channel.write(dataBuffer);
	    }
	}
	
	public static Object receiveObject(SocketChannel channel) throws IOException, ClassNotFoundException {
	    ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
	    while (lengthBuffer.hasRemaining()) {
	        if (channel.read(lengthBuffer) == -1) {
	            throw new EOFException("Connection closed prematurely");
	        }
	    }
	    lengthBuffer.flip();
	    int length = lengthBuffer.getInt();
	    // Read exactly 'length' bytes
	    ByteBuffer dataBuffer = ByteBuffer.allocate(length);
	    while (dataBuffer.hasRemaining()) {
	        if (channel.read(dataBuffer) == -1) {
	            throw new EOFException("Incomplete data received");
	        }
	    }
	    dataBuffer.flip();
	    byte[] bytes = new byte[length];
	    dataBuffer.get(bytes);
	    try (ObjectInputStream objIn = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
	        return (Object) objIn.readObject();
	    }
	}
}
