package org.ros.internal.node.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.ros.address.AdvertiseAddress;
import org.ros.address.BindAddress;
import org.ros.namespace.GraphName;

import com.neocoretechs.relatrix.server.remoteiterator.RemoteIteratorTransactionServer;

/**
 * Remote invocation of methods consists of providing reflected classes here which are invoked via simple
 * serializable descriptions of the method and parameters. Providing additional resources involves adding
 * another static instance of ServerInvokeMethod and populating that at construction of this class.<p>
 * In the processing pipeline you must provide a 'process' implementation which will call 'invokeMethod'
 * and if the remote call is linked to an object instance on the server, as it 
 * is for non-serializable iterators, then you must maintain 
 * a mapping from session GUID to an instance of the object you are invoking on the server side.<p>
 * Static methods need no server side object in residence and can be called ad hoc.<br>
 * Functionally this class Extends {@link RpcServer}, which
 * Starts a {@link BaseServer}, which starts a {@link TCPWorker}.
 * Use this server for transaction context.
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015, 2021, 2022, 2024
 *
 */
public class RelatrixTransactionServer extends RpcServer {
	private static boolean DEBUG = false;
	private static boolean DEBUGCOMMAND = false;
	private final Map<GraphName, List<NodeIdentifier>> subscribers;
	private final GraphName masterName;	
	public static ServerInvokeMethod relatrixMethods = null; // Main Relatrix class methods
	public static ConcurrentHashMap<String, Object> sessionToObject = new ConcurrentHashMap<String,Object>();

	private ConcurrentHashMap<String, TCPWorker> dbToWorker = new ConcurrentHashMap<String, TCPWorker>();
	public static String[] iteratorServers = new String[]{
			"com.neocoretechs.relatrix.iterator.RelatrixIteratorTransaction",
			"com.neocoretechs.relatrix.iterator.RelatrixSubsetIteratorTransaction",
			"com.neocoretechs.relatrix.iterator.RelatrixHeadsetIteratorTransaction",
			"com.neocoretechs.relatrix.iterator.RelatrixTailsetIteratorTransaction",
			"com.neocoretechs.relatrix.iterator.RelatrixEntrysetIteratorTransaction",				
			"com.neocoretechs.relatrix.iterator.RelatrixKeysetIteratorTransaction"
	};				
	public static int[] iteratorPorts = new int[] {
			9080,9081,9082,9083,9084,9085
	};
	public static int findIteratorServerPort(String clazz) {
		return iteratorPorts[Arrays.asList(iteratorServers).indexOf(clazz)];
	}

	/**
	 * Construct the server bound to stated address
	 * @param binder bind address
	 * @param advertiseAddress advertise address
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public RelatrixTransactionServer(BindAddress binder, AdvertiseAddress advertiseAddress) throws IOException, ClassNotFoundException {
		super(binder, advertiseAddress);
		masterName = GraphName.of("/relatrix");
		subscribers = new ConcurrentHashMap<GraphName, List<NodeIdentifier>>();
		RelatrixTransactionServer.relatrixMethods = new ServerInvokeMethod("com.neocoretechs.relatrix.RelatrixTransaction", 0);
		for(int i = 0; i < iteratorServers.length; i++)
			new RemoteIteratorTransactionServer(iteratorServers[i], bindAddress.toInetSocketAddress().getAddress(), iteratorPorts[i]);
	}

	public void subscribe(GraphName name, NodeIdentifier nodeIdentifier) {
		List<NodeIdentifier> subs = subscribers.get(name);
		if( subs == null ) {
			subs = new ArrayList<NodeIdentifier>();
			subscribers.put(name, subs);
		}
		subs.add(nodeIdentifier);
	}

	@Override
	public synchronized Object invokeMethod(RemoteRequestInterface rri) throws Exception {
		return relatrixMethods.invokeMethod(rri, this);
	}

}
