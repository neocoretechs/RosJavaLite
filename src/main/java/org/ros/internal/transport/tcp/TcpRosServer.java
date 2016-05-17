package org.ros.internal.transport.tcp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.jboss.netty.bootstrap.ServerBootstrap;
//import org.jboss.netty.buffer.HeapChannelBufferFactory;
//import org.jboss.netty.channel.Channel;
//import org.jboss.netty.channel.ChannelFactory;
//import org.jboss.netty.channel.group.ChannelGroup;
//import org.jboss.netty.channel.group.DefaultChannelGroup;
//import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.ros.address.AdvertiseAddress;
import org.ros.address.BindAddress;
import org.ros.internal.node.service.ServiceManager;
import org.ros.internal.node.topic.TopicParticipantManager;

import io.netty.bootstrap.ChannelFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;

/**
 * The TCP server which is used for data communication between publishers and
 * subscribers or between a service and a service client.
 * 
 * <p>
 * This server is used after publishers, subscribers, services and service
 * clients have been told about each other by the master.
 * 
 * @author jg
 */
public class TcpRosServer implements Serializable {
  private static final long serialVersionUID = 1298495789043968855L;
  private static final boolean DEBUG = true ;
  private static final Log log = LogFactory.getLog(TcpRosServer.class);

  private BindAddress bindAddress;
  private AdvertiseAddress advertiseAddress;
  private transient TopicParticipantManager topicParticipantManager;
  private transient ServiceManager serviceManager;
  private transient NioEventLoop executorService;

  private transient ServerBootstrap bootstrap;
  private transient Channel outgoingChannel;
  private transient ChannelGroup incomingChannelGroup;
  
  public TcpRosServer() {}

  public TcpRosServer(BindAddress bindAddress, AdvertiseAddress advertiseAddress,
      TopicParticipantManager topicParticipantManager, ServiceManager serviceManager,
      ScheduledExecutorService executorService) {
    this.bindAddress = bindAddress;
    this.advertiseAddress = advertiseAddress;
    this.topicParticipantManager = topicParticipantManager;
    this.serviceManager = serviceManager;
    this.executorService = (NioEventLoop) executorService;
  }

  public void start() {
	  /*
    assert(outgoingChannel == null);
    channelFactory = new NioServerSocketChannelFactory(executorService, executorService);
    bootstrap = new ServerBootstrap(channelFactory);
    bootstrap.setOption("child.bufferFactory",
        new HeapChannelBufferFactory(ByteOrder.LITTLE_ENDIAN));
    bootstrap.setOption("child.keepAlive", true);
    incomingChannelGroup = new DefaultChannelGroup();
    bootstrap.setPipelineFactory(new TcpServerPipelineFactory(incomingChannelGroup,
        topicParticipantManager, serviceManager));

    outgoingChannel = bootstrap.bind(bindAddress.toInetSocketAddress());
    advertiseAddress.setPort(((InetSocketAddress)(outgoingChannel.getLocalAddress())).getPort());
    */
    /*
    advertiseAddress.setPortCallable(new Callable<Integer>() {
      @Override
      public Integer call() throws Exception {
        return ((InetSocketAddress) outgoingChannel.getLocalAddress()).getPort();
      }
    });
    */
	
		//InetSocketAddress isock = new InetSocketAddress(0);
	    //topicParticipantManager = new TopicParticipantManager();
	    //serviceManager = new ServiceManager();
	    //NioServerSocketChannelFactory channelFactory =
	    //    new NioServerSocketChannelFactory(executorService, executorService);
	    //ServerBootstrap bootstrap = new ServerBootstrap(channelFactory);
	  try {
		NioEventLoopGroup mainExec = new NioEventLoopGroup(1);
	    bootstrap = new ServerBootstrap();
	    bootstrap.group(mainExec,executorService).
	    	channel(NioServerSocketChannel.class).
	    	option(ChannelOption.SO_BACKLOG, 100).
	    	option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT).
	    	localAddress(bindAddress.toInetSocketAddress()).
	    	//childOption("child.bufferFactory",new HeapChannelBufferFactory(ByteOrder.LITTLE_ENDIAN).
	    	childOption(ChannelOption.SO_KEEPALIVE, true);
	    incomingChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	    TcpServerPipelineFactory serverPipelineFactory =
	        new TcpServerPipelineFactory(incomingChannelGroup, topicParticipantManager, serviceManager); /*{
	          //@Override
	          public ChannelPipeline pipeline() {
	            ChannelPipeline pipeline = super.getPipeline();
	            // We're not interested firstIncomingMessageQueue testing the
	            // handshake here. Removing it means connections are established
	            // immediately.
	            pipeline.remove(TcpServerPipelineFactory.HANDSHAKE_HANDLER);
	            pipeline.addLast( new ServerHandler());
	            return pipeline;
	          }
	        };*/
	       bootstrap.handler((ChannelHandler) new LoggingHandler(LogLevel.INFO)).
	       childHandler(serverPipelineFactory);
	       advertiseAddress.setPort(bindAddress.toInetSocketAddress().getPort());
	       outgoingChannel = bootstrap.bind().sync().channel();
	       if (DEBUG) {
	    	      log.info("TcpRosServer starting and Bound to:" + bindAddress + " with advertise address:"+advertiseAddress);
	       }
	       outgoingChannel.closeFuture().await();
      } catch (InterruptedException e) {
      } finally {
          executorService.shutdownGracefully();
	       if (DEBUG) {
	    	      log.info("TcpRosServer shut down for:" + bindAddress + " with advertise address:"+advertiseAddress);
	       }
      }
	  // old v.3:
	    //bootstrap.setPipelineFactory(serverPipelineFactory);
	    //Channel serverChannel = bootstrap.bind(new InetSocketAddress(0))
  }

  /**
   * Close all incoming connections and the server socket.
   * 
   * <p>
   * Calling this method more than once has no effect.
   */
  public void shutdown() {
    if (DEBUG) {
      log.info("TcpRosServer Shutting down address: " + getAddress());
    }
    if (outgoingChannel != null) {
      outgoingChannel.close().awaitUninterruptibly();
    }
    incomingChannelGroup.close().awaitUninterruptibly();
    // NOTE(damonkohler): We are purposely not calling
    // channelFactory.releaseExternalResources() or
    // bootstrap.releaseExternalResources() since only external resources are
    // the ExecutorService and control of that must remain with the overall
    // application.
    outgoingChannel = null;
  }

  /**
   * @return the advertise-able {@link InetSocketAddress} of this
   *         {@link TcpRosServer}
   */
  public InetSocketAddress getAddress() {
    return advertiseAddress.toInetSocketAddress();
  }

  /**
   * @return the {@link AdvertiseAddress} of this {@link TcpRosServer}
   */
  public AdvertiseAddress getAdvertiseAddress() {
    return advertiseAddress;
  }
}
