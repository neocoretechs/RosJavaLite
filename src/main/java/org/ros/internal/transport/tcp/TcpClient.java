package org.ros.internal.transport.tcp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.exception.RosRuntimeException;
import org.ros.internal.transport.ChannelHandlerContext;
import org.ros.internal.transport.ChannelHandlerContextImpl;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;

import java.util.List;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Creates the critical ChannelHandlerContext and the AsynchTCPWorker that services it.<br/>
 * Contains the Socket that is the channel.<br/>
 * These are all populated when the 'connect' method is called.
 * Add the named channel handlers beforehand using the supplied methods and they will
 * be injected into the pipeline of the ChannelHandlerContext when the channel is initialized.
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2021
 */
public class TcpClient {
  private static final boolean DEBUG = true;
  private static final Log log = LogFactory.getLog(TcpClient.class);

  private static final int DEFAULT_CONNECTION_TIMEOUT_DURATION = 5;
  private static final TimeUnit DEFAULT_CONNECTION_TIMEOUT_UNIT = TimeUnit.SECONDS;
  private static final boolean DEFAULT_KEEP_ALIVE = true;
  
  private ChannelHandlerContext ctx;
  private final List<NamedChannelHandler> namedChannelHandlers;
  
  private Socket channel;
  private ExecutorService executor;
  private ChannelInitializerFactoryStack factoryStack; // Stack of ChannelInitializer factories to load ChannelHandlers
  
  public TcpClient( ExecutorService executor, List<NamedChannelHandler> namedChannelHandlers) {
	this.executor = executor;
	this.namedChannelHandlers = namedChannelHandlers;
	this.factoryStack = new ChannelInitializerFactoryStack();
  }

  public void setConnectionTimeout(long duration, TimeUnit unit) {
	 log.error("Not impl");
  }

  public void setKeepAlive(boolean value) throws IOException {
	  //channel.setOption(StandardSocketOptions.SO_KEEPALIVE, value);
	  channel.setKeepAlive(value);
  }

  public void addNamedChannelHandler(NamedChannelHandler namedChannelHandler) {
	  if (DEBUG) {
	        log.info("TcpClient:"+this+" adding NamedChannelHandler:"+namedChannelHandler);
	  }
	  namedChannelHandlers.add(namedChannelHandler);
  }

  public void addAllNamedChannelHandlers(List<NamedChannelHandler> namedChannelHandlers) {
	  if (DEBUG) {
	        for(NamedChannelHandler n: namedChannelHandlers)
	  	        log.info("TcpClient:"+this+" will add NamedChannelHandler:"+n);
	  }
	  this.namedChannelHandlers.addAll(namedChannelHandlers);
  }

  public ChannelHandlerContext getContext() { return ctx; }
  /**
   * Connect despite possible initial failure by continual re-try until success.
   * Delay for 5 seconds then re-try connection. Assume eventual response of server.
   * @param connectionName then name of this connection
   * @param socketAddress the address we are attempting connection to
   * @return The successfully connected socket
   */
  public Socket connect(String connectionName, SocketAddress socketAddress) {
	  if (DEBUG) {
		  log.info("TcpClient:"+this+" attempting connection:"+connectionName+" to socket:" + socketAddress);
	  }
	  while(true) {
		  try {
			  channel = new Socket();
			  //channel.setTcpNoDelay(true);
			  channel.setSendBufferSize(4096000);
			  channel.setSendBufferSize(4096000);
			  ctx = new ChannelHandlerContextImpl(executor, channel);
			  // connect outbound to pub
			  ctx.connect(socketAddress);
			  //
			  TcpClientPipelineFactory tcpClientPipelineFactory = new TcpClientPipelineFactory(namedChannelHandlers);
			  // add handler pipeline factory to stack
			  factoryStack.addLast(tcpClientPipelineFactory);
			  // load the handlers from the pipeline factories
			  // inject calls initChannel on each ChannelInitializer in the factoryStack
			  factoryStack.inject(ctx);
			  // notify new handlers all loaded
			  //ctx.pipeline().fireChannelRegistered(); 
			  // connect outbound to pub
			  //ctx.connect(socketAddress);
			  AsynchTCPWorker uworker = new AsynchTCPWorker(ctx);
			  executor.execute(uworker);
			  // notify pipeline we connected (or failed via exceptionCaught and runtime exception)
			  ctx.pipeline().fireChannelActive();
			  // recall we keep the list of contexts in TcpClientManager  
			  if (DEBUG) {
				  log.info("TcpClient:"+this+" Connected with ChannelHandlerContext "+ctx);
			  }
			  return channel;
		  } catch(Exception e) {
			  log.error("Exception establishing connection to "+connectionName+" at address "+socketAddress+", Re-try connection in 5 seconds...");
			  try {
				  Thread.sleep(5000);
			  } catch (InterruptedException e1) {}
		  }
	  }
  }

}