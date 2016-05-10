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

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ChannelFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.EventExecutor;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class TcpClient {

  private static final boolean DEBUG = true;
  private static final Log log = LogFactory.getLog(TcpClient.class);

  private static final int DEFAULT_CONNECTION_TIMEOUT_DURATION = 5;
  private static final TimeUnit DEFAULT_CONNECTION_TIMEOUT_UNIT = TimeUnit.SECONDS;
  private static final boolean DEFAULT_KEEP_ALIVE = true;

  private final ChannelGroup channelGroup;
  private NioEventLoop executorServiceMain;
  private final Bootstrap bootstrap;
  private final List<NamedChannelHandler> namedChannelHandlers;
  
  private Channel channel;

  public TcpClient(ChannelGroup channelGroup, EventLoopGroup executor) {
    this.channelGroup = channelGroup;
    /*
    channelFactory = new NioClientSocketChannelFactory(executor, executor);
    channelBufferFactory = new HeapChannelBufferFactory(ByteOrder.LITTLE_ENDIAN);
    bootstrap = new ClientBootstrap(channelFactory);
    bootstrap.setOption("bufferFactory", channelBufferFactory);
    setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT_DURATION, DEFAULT_CONNECTION_TIMEOUT_UNIT);
    setKeepAlive(DEFAULT_KEEP_ALIVE);
    */
      bootstrap = new Bootstrap();
      bootstrap.group(executor).
      	channel(NioServerSocketChannel.class).
      	option(ChannelOption.SO_BACKLOG, 100).
      	option(ChannelOption.SO_TIMEOUT,DEFAULT_CONNECTION_TIMEOUT_DURATION).
      	//childOption("child.bufferFactory",new HeapChannelBufferFactory(ByteOrder.LITTLE_ENDIAN).
      	option(ChannelOption.SO_KEEPALIVE, true);
    namedChannelHandlers = new ArrayList<NamedChannelHandler>();
  }

  public void setConnectionTimeout(long duration, TimeUnit unit) {
    bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int)duration);
  }

  public void setKeepAlive(boolean value) {
    bootstrap.option(ChannelOption.SO_KEEPALIVE, value);
  }

  public void addNamedChannelHandler(NamedChannelHandler namedChannelHandler) {
    namedChannelHandlers.add(namedChannelHandler);
  }

  public void addAllNamedChannelHandlers(List<NamedChannelHandler> namedChannelHandlers) {
    this.namedChannelHandlers.addAll(namedChannelHandlers);
  }

  public Channel connect(String connectionName, SocketAddress socketAddress) {
    TcpClientPipelineFactory tcpClientPipelineFactory = new TcpClientPipelineFactory(channelGroup) {
      @Override
      public void initChannel(SocketChannel ch) {
        for (NamedChannelHandler namedChannelHandler : namedChannelHandlers) {
          ch.pipeline().addLast(namedChannelHandler.getName(), (ChannelHandler) namedChannelHandler);
        }
      }
    };
    bootstrap.handler(tcpClientPipelineFactory);
    ChannelFuture future = bootstrap.connect(socketAddress).awaitUninterruptibly();
    if (future.isSuccess()) {
      channel = future.channel();
      if (DEBUG) {
        log.info("TcpClient Connected to socket: " + socketAddress);
      }
    } else {
      // We expect the first connection to succeed. If not, fail fast.
      throw new RosRuntimeException("TcpClient socket connection exception: " + socketAddress, future.cause());
    }
    return channel;
  }

  public ChannelFuture write(ByteBuf buffer) {
    assert(channel != null);
    assert(buffer != null);
    return channel.write(buffer);
  }
}