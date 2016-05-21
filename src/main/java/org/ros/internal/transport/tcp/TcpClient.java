package org.ros.internal.transport.tcp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.jboss.netty.bootstrap.ClientBootstrap;
//import org.jboss.netty.buffer.ChannelBuffer;
//import org.jboss.netty.buffer.ChannelBufferFactory;
//import org.jboss.netty.buffer.HeapChannelBufferFactory;
//import org.jboss.netty.channel.Channel;
//import org.jboss.netty.channel.ChannelFactory;
//import org.jboss.netty.channel.ChannelFuture;
//import org.jboss.netty.channel.ChannelPipeline;
//import org.jboss.netty.channel.group.ChannelGroup;
//import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

import org.ros.exception.RosRuntimeException;
import org.ros.internal.node.service.ServiceManager;
import org.ros.internal.node.topic.TopicParticipantManager;
import org.ros.internal.transport.ChannelHandlerContext;
import org.ros.internal.transport.ChannelHandlerContextImpl;
import org.ros.internal.transport.ChannelPipeline;
import org.ros.internal.transport.ChannelPipelineImpl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Add the named channel handlers beforehand using the supplied methods and they will
 * be injected into the pipeline of the handlercontext when the channel is initialized
 * @author jg
 */
public class TcpClient {

  private static final boolean DEBUG = true;
  private static final Log log = LogFactory.getLog(TcpClient.class);

  private static final int DEFAULT_CONNECTION_TIMEOUT_DURATION = 5;
  private static final TimeUnit DEFAULT_CONNECTION_TIMEOUT_UNIT = TimeUnit.SECONDS;
  private static final boolean DEFAULT_KEEP_ALIVE = true;
  
  private ChannelHandlerContext ctx;
  private final List<NamedChannelHandler> namedChannelHandlers;
  private Executor executor;
  
  private AsynchronousSocketChannel channel;
  private ChannelPipeline pipeline;
  private AsynchronousChannelGroup channelGroup;

  public TcpClient( Executor executor, ChannelPipeline pipeline, AsynchronousChannelGroup channelGroup) {
	  this.executor = executor;
	  this.pipeline = pipeline;
	  this.channelGroup = channelGroup;
    /*
    channelFactory = new NioClientSocketChannelFactory(executor, executor);
    channelBufferFactory = new HeapChannelBufferFactory(ByteOrder.LITTLE_ENDIAN);
    bootstrap = new ClientBootstrap(channelFactory);
    bootstrap.setOption("bufferFactory", channelBufferFactory);
    setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT_DURATION, DEFAULT_CONNECTION_TIMEOUT_UNIT);
    setKeepAlive(DEFAULT_KEEP_ALIVE);
    
      bootstrap = new Bootstrap();
      bootstrap.group(executor).
      	channel(NioServerSocketChannel.class).
      	option(ChannelOption.SO_BACKLOG, 100).
      	option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT).
      	option(ChannelOption.SO_TIMEOUT,DEFAULT_CONNECTION_TIMEOUT_DURATION).
      	//childOption("child.bufferFactory",new HeapChannelBufferFactory(ByteOrder.LITTLE_ENDIAN).
      	option(ChannelOption.SO_KEEPALIVE, DEFAULT_KEEP_ALIVE);
      	*/
    namedChannelHandlers = new ArrayList<NamedChannelHandler>();
  }

  public void setConnectionTimeout(long duration, TimeUnit unit) {
	 log.error("Not impl");
  }

  public void setKeepAlive(boolean value) throws IOException {
    //bootstrap.option(ChannelOption.SO_KEEPALIVE, value);
	  channel.setOption(StandardSocketOptions.SO_KEEPALIVE, value);
  }

  public void addNamedChannelHandler(NamedChannelHandler namedChannelHandler) {
    namedChannelHandlers.add(namedChannelHandler);
  }

  public void addAllNamedChannelHandlers(List<NamedChannelHandler> namedChannelHandlers) {
    this.namedChannelHandlers.addAll(namedChannelHandlers);
  }

  public Channel connect(String connectionName, SocketAddress socketAddress) throws Exception {
	ctx = new ChannelHandlerContextImpl(channelGroup, pipeline, null, executor);
	channel = AsynchronousSocketChannel.open(ctx.getChannelGroup());
    TcpClientPipelineFactory tcpClientPipelineFactory = new TcpClientPipelineFactory(ctx.getChannelGroup()) {
      //@Override
      public void initChannel(ChannelHandlerContext ch) {
        for (NamedChannelHandler namedChannelHandler : namedChannelHandlers) {
          ch.pipeline().addLast(namedChannelHandler.getName(), namedChannelHandler);
        }
      }
    };
    //bootstrap.handler(tcpClientPipelineFactory);
    //ChannelFuture future = bootstrap.connect(socketAddress).awaitUninterruptibly();
    //if (future.isSuccess()) {
    //  channel = future.channel();
    ctx.connect(socketAddress);
    ((ChannelPipelineImpl)ctx.pipeline()).inject(tcpClientPipelineFactory);
    if (DEBUG) {
        log.info("TcpClient Connected to socket: " + socketAddress);
    }
    //} else {
      // We expect the first connection to succeed. If not, fail fast.
      //throw new RosRuntimeException("TcpClient socket connection exception: " + socketAddress, future.cause());
    //}
    return channel;
  }

  public Future<Integer> write(ByteBuffer buffer) {
    assert(channel != null);
    assert(buffer != null);
    return channel.write(buffer);
  }
}