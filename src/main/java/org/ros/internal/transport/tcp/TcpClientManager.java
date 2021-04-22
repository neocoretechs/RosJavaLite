package org.ros.internal.transport.tcp;

import java.io.IOException;
import java.net.SocketAddress;
//import java.nio.channels.AsynchronousChannelGroup;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * TcpClientManager manages TCP clients which are the subscriber and service clients that communicate with
 * remote peers outside master domain. 
 * It requires the executor service constructed.
 * For each TcpClient constructed there will be an associated ChannelHandlerContext.
 * We maintain a list of the ChannelHandlerContexts here (as TcpClients) such that we may perform the necessary ops on them.
 * This class is set up as a singleton returning instances for each executor.
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2021
 */
public class TcpClientManager {
  public static boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(TcpClientManager.class);
  private final /*Asynchronous*/ChannelGroup channelGroup;
  private final Collection<TcpClient> tcpClients;
  private final List<NamedChannelHandler> namedChannelHandlers;
  
  //private static ConcurrentHashMap<ExecutorService, TcpClientManager> executors = new ConcurrentHashMap<ExecutorService, TcpClientManager>(1024);

  //public static TcpClientManager getInstance(ExecutorService exc) {
	//  synchronized(TcpClientManager.class) {
	//	  TcpClientManager tcm = executors.get(exc);
	//	  if( tcm == null ) {
	//		  tcm =  new TcpClientManager(exc);
	//		  executors.put(exc,  tcm);
	//	  }
	//	  return tcm;
	 // }
  //}
  
  public TcpClientManager(ExecutorService executor) {
    this.channelGroup = new ChannelGroupImpl(executor);/*AsynchronousChannelGroup.withThreadPool(executor);*/
    this.tcpClients = new ArrayList<TcpClient>();
    this.namedChannelHandlers = new ArrayList<NamedChannelHandler>();
    if( DEBUG )
    	log.info("TcpClientManager:"+executor+" "+channelGroup);
  }

  public void addNamedChannelHandler(NamedChannelHandler namedChannelHandler) {
    namedChannelHandlers.add(namedChannelHandler);
  }

  public void addAllNamedChannelHandlers(List<NamedChannelHandler> namedChannelHandlers) {
    this.namedChannelHandlers.addAll(namedChannelHandlers);
  }

  /**
   * Connects to a server.
   * <p>
   * This call blocks until the connection is established or fails.
   * 
   * @param connectionName
   *          the name of the new connection
   * @param socketAddress
   *          the {@link SocketAddress} to connect to
   * @return a new {@link TcpClient}
 * @throws IOException 
   */
  public TcpClient connect(String connectionName, SocketAddress socketAddress) throws Exception {
	if( DEBUG )
	    	log.info("TcpClient connect:"+connectionName+" "+socketAddress);
    TcpClient tcpClient = new TcpClient(channelGroup, namedChannelHandlers);
    tcpClient.connect(connectionName, socketAddress);
    tcpClients.add(tcpClient);
    return tcpClient;
  }

  /**
   * Sets all {@link TcpClientConnection}s as non-persistent and closes all open
   * {@link Channel}s.
   */
  public void shutdown() {
	if( DEBUG )
	    	log.info("TcpClient shutdown:");
    channelGroup.shutdown();
    tcpClients.clear();
    // We don't call channelFactory.releaseExternalResources() or
    // bootstrap.releaseExternalResources() since the only external resource is
    // the ExecutorService which must remain in the control of the overall
    // application.
  }
}
