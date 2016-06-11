package org.ros.internal.transport.tcp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.exception.RosRuntimeException;
import org.ros.internal.transport.ChannelHandlerContext;
import org.ros.internal.transport.ChannelHandlerContextImpl;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Add the named channel handlers beforehand using the supplied methods and they will
 * be injected into the pipeline of the ChannelHandlerContext when the channel is initialized
 * @author jg
 */
public class TcpClient {

  private static final boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(TcpClient.class);

  private static final int DEFAULT_CONNECTION_TIMEOUT_DURATION = 5;
  private static final TimeUnit DEFAULT_CONNECTION_TIMEOUT_UNIT = TimeUnit.SECONDS;
  private static final boolean DEFAULT_KEEP_ALIVE = true;
  
  private ChannelHandlerContext ctx;
  private final List<NamedChannelHandler> namedChannelHandlers;
  private Executor executor;
  
  private Socket channel;
  
  private /*Asynchronous*/ChannelGroup channelGroup;
  
  private ChannelInitializerFactoryStack factoryStack; // Stack of ChannelInitializer factories to load ChannelHandlers
  
  public TcpClient( Executor executor, /*Asynchronous*/ChannelGroup channelGroup, List<NamedChannelHandler> namedChannelHandlers) {
	this.executor = executor;
	this.channelGroup = channelGroup;
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
    namedChannelHandlers.add(namedChannelHandler);
  }

  public void addAllNamedChannelHandlers(List<NamedChannelHandler> namedChannelHandlers) {
    this.namedChannelHandlers.addAll(namedChannelHandlers);
  }

  public ChannelHandlerContext getContext() { return ctx; }
  
  public Socket connect(String connectionName, SocketAddress socketAddress) throws Exception {
	//channel = /*Asynchronous*/SocketChannel.open(/*channelGroup*/);
	  channel = new Socket();
	  //channel.setTcpNoDelay(true);
	  channel.setSendBufferSize(4096000);
	  channel.setSendBufferSize(4096000);
	//((/*Asynchronous*/SocketChannel)channel).setOption(StandardSocketOptions.SO_RCVBUF, 4096000);
	//((/*Asynchronous*/SocketChannel)channel).setOption(StandardSocketOptions.SO_SNDBUF, 4096000);
	//((/*Asynchronous*/SocketChannel)channel).setOption(StandardSocketOptions.TCP_NODELAY, false);
	ctx = new ChannelHandlerContextImpl(channelGroup, channel, executor);
    TcpClientPipelineFactory tcpClientPipelineFactory = new TcpClientPipelineFactory(ctx.getChannelGroup(), namedChannelHandlers);
    // add handler pipeline factory to stack
    factoryStack.addLast(tcpClientPipelineFactory);
    // load the handlers from the pipeline factories
    // inject calls initChannel on each ChannelInitializer in the factoryStack
    factoryStack.inject(ctx);
    // notify new handlers all loaded
    ctx.pipeline().fireChannelRegistered(); 
    // connect outbound to pub
    ctx.connect(socketAddress);
  
    AsynchTCPWorker uworker = new AsynchTCPWorker(ctx);
    executor.execute(uworker); 
    // notify pipeline we connected (or failed via exceptionCaught and runtime exception)
    ctx.pipeline().fireChannelActive();
	// recall we keep the list of contexts in TcpClientManager
    
    if (DEBUG) {
        log.info("TcpClient Connected to socket: " + socketAddress+" with worker "+uworker);
    }
    //} else {
      // We expect the first connection to succeed. If not, fail fast.
      //throw new RosRuntimeException("TcpClient socket connection exception: " + socketAddress, future.cause());
    //}
    return channel;
  }


}