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
import org.ros.exception.RosRuntimeException;
import org.ros.internal.node.service.ServiceManager;
import org.ros.internal.node.topic.TopicParticipantManager;
import org.ros.internal.transport.ChannelHandlerContext;
import org.ros.internal.transport.ChannelHandlerContextImpl;
import org.ros.internal.transport.ChannelPipeline;
import org.ros.internal.transport.ChannelPipelineImpl;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
  private transient ScheduledExecutorService executorService;

  private transient AsynchronousChannelGroup outgoingChannelGroup; // publisher with connected subscribers
  private transient AsynchronousChannelGroup incomingChannelGroup; // subscriber connected to publishers
  private transient TcpServerPipelineFactory serverPipelineFactory;
  private transient ChannelPipeline pipeline;
  private transient List<ChannelHandlerContext> contexts;
  
  public static final String LENGTH_FIELD_BASED_FRAME_DECODER = "LengthFieldBasedFrameDecoder";
  public static final String LENGTH_FIELD_PREPENDER = "LengthFieldPrepender";
  public static final String HANDSHAKE_HANDLER = "HandshakeHandler";
  
  public TcpRosServer() {}

  public TcpRosServer(BindAddress bindAddress, AdvertiseAddress advertiseAddress,
      TopicParticipantManager topicParticipantManager, ServiceManager serviceManager,
      ScheduledExecutorService executorService) throws IOException {
    this.bindAddress = bindAddress;
    this.advertiseAddress = advertiseAddress;
    this.topicParticipantManager = topicParticipantManager;
    this.serviceManager = serviceManager;
    this.executorService = executorService;
    this.incomingChannelGroup = AsynchronousChannelGroup.withThreadPool(executorService);
    this.incomingChannelGroup = AsynchronousChannelGroup.withThreadPool(executorService);
    this.advertiseAddress.setPort(bindAddress.toInetSocketAddress().getPort());
    this.pipeline = new ChannelPipelineImpl();
    this.contexts = new ArrayList<ChannelHandlerContext>();
    this.serverPipelineFactory =
	        new TcpServerPipelineFactory(incomingChannelGroup, topicParticipantManager, serviceManager); 
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
	  try {
		  /*
	
	    	localAddress(bindAddress.toInetSocketAddress()).
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
	        };
	       handler((ChannelHandler) new LoggingHandler(LogLevel.INFO)).
	       childHandler(new ChannelInitializer<SocketChannel>() {
               @Override
               public void initChannel(SocketChannel ch) throws Exception {
                   if( DEBUG )
               		log.info("TcpServerPipelineFactory initChannel:"+ch);
                   ChannelPipeline pipeline = ch.pipeline();
                   pipeline.addLast(new ObjectEncoder());
                   pipeline.addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
                   pipeline.addLast(LENGTH_FIELD_PREPENDER, new LengthFieldPrepender(4));
                   pipeline.addLast(LENGTH_FIELD_BASED_FRAME_DECODER, new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                   pipeline.addLast(HANDSHAKE_HANDLER, new TcpServerHandshakeHandler(topicParticipantManager,serviceManager));
               }
           });
	       */
	       //childHandler(serverPipelineFactory);
	       //ChannelFuture f = bootstrap.bind().sync();
	       //outgoingChannel = f.channel();
          pipeline.addLast(HANDSHAKE_HANDLER, new TcpServerHandshakeHandler(topicParticipantManager,serviceManager));
		  final AsynchronousServerSocketChannel listener = AsynchronousServerSocketChannel.open(incomingChannelGroup);
		  listener.bind(bindAddress.toInetSocketAddress());
	      if (DEBUG) {
		 	     log.info("TcpRosServer starting and Bound to:" + bindAddress + " with advertise address:"+advertiseAddress);
		  }
		  while(true) {
			  Future<AsynchronousSocketChannel> channel = listener.accept();
			  if( DEBUG ) {
				  log.debug("Accept "+channel);
			  }
			  contexts.add(new ChannelHandlerContextImpl(incomingChannelGroup, pipeline, channel.get(), executorService));
		  }
	       //outgoingChannel.closeFuture().sync();
      } catch (IOException | InterruptedException | ExecutionException e) {
    	  throw new RosRuntimeException(e);
	  } finally {
		try {
			shutdown();
		} catch (IOException e) {}
	    if (DEBUG) {
	    	      log.info("TcpRosServer shut down for:" + bindAddress + " with advertise address:"+advertiseAddress);
	    }
      }
  }

  /**
   * Close all incoming connections and the server socket.
   * only external resources are
   * the ExecutorService and control of that must remain with the overall
   * application.
   * <p>
   * Calling this method more than once has no effect.
   * @throws IOException 
   */
  public void shutdown() throws IOException {
    if (DEBUG) {
      log.info("TcpRosServer Shutting down address: " + getAddress());
    }
    if (outgoingChannelGroup != null) {
      outgoingChannelGroup.shutdown();
      outgoingChannelGroup = null;
    }
    if( incomingChannelGroup != null) {
    	incomingChannelGroup.shutdown();
    	incomingChannelGroup = null;
    }
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
  /**
   * 
   * @return the array of contexts from the remote connections that have attached for subscription
   */
  public List<ChannelHandlerContext> getSubscribers() {
	  return contexts;
  }
}
