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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

/**
 * The TCP server which is used for data communication between publishers and
 * subscribers or between a service and a service client.
 * 
 * <p>
 * This server is used after publishers, subscribers, services and service
 * clients have been told about each other by the master.
 * It creates an AsynchBaseServer which handles the lower level TCP communications
 * while higher level channel abstractions are dealt with here.
 * ChannelHandlerContext is created in AsynchBaseServer and populates ArrayBlockingQueue here.
 * @author jg
 */
public class TcpRosServer implements Serializable {
  private static final long serialVersionUID = 1298495789043968855L;
  private static final boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(TcpRosServer.class);

  private BindAddress bindAddress;
  private AdvertiseAddress advertiseAddress;
  private transient TopicParticipantManager topicParticipantManager;
  private transient ServiceManager serviceManager;
  private transient ScheduledExecutorService executorService;

  //private transient AsynchronousChannelGroup outgoingChannelGroup; // publisher with connected subscribers
  //private transient AsynchronousChannelGroup incomingChannelGroup; // subscriber connected to publishers
  private transient ChannelGroup outgoingChannelGroup; // publisher with connected subscribers
  private transient ChannelGroup incomingChannelGroup; // subscriber connected to publishers
  private transient TcpServerPipelineFactory serverPipelineFactory;
  private transient ChannelInitializerFactoryStack factoryStack; // Stack of ChannelInitializer factories to load ChannelHandlers
  private transient ArrayBlockingQueue<ChannelHandlerContext> contexts;
  
  private transient AsynchBaseServer server = null;
  
  public TcpRosServer() {}

  public TcpRosServer(BindAddress bindAddress, AdvertiseAddress advertiseAddress,
      TopicParticipantManager topicParticipantManager, ServiceManager serviceManager,
      ScheduledExecutorService executorService) {
    this.bindAddress = bindAddress;
    this.advertiseAddress = advertiseAddress;
    this.topicParticipantManager = topicParticipantManager;
    this.serviceManager = serviceManager;
    this.executorService = executorService;
    this.contexts = new ArrayBlockingQueue<ChannelHandlerContext>(1024);
  }

  public void start() {
    //assert(outgoingChannel == null);
	  try {
		  incomingChannelGroup = new ChannelGroupImpl(executorService);//AsynchronousChannelGroup.withThreadPool(executorService);
		  advertiseAddress.setPort(bindAddress.toInetSocketAddress().getPort());
		  factoryStack = new ChannelInitializerFactoryStack();
		  serverPipelineFactory =
			        new TcpServerPipelineFactory(incomingChannelGroup, topicParticipantManager, serviceManager); 
		  factoryStack.addLast(serverPipelineFactory);	    
		  server = new AsynchBaseServer(this);
		  server.startServer(incomingChannelGroup, bindAddress.toInetSocketAddress());
	      if (DEBUG) {
		 	     log.info("TcpRosServer starting and Bound to:" + bindAddress + " with advertise address:"+advertiseAddress);
		  }		  
      } catch (Exception e) {
    		try {
    			shutdown();
    		} catch (IOException e1) {}
    	  	  throw new RosRuntimeException(e);
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
	if (DEBUG) {
  	      log.info("TcpRosServer shut down for:" + bindAddress + " with advertise address:"+advertiseAddress);
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
  public ArrayBlockingQueue<ChannelHandlerContext> getSubscribers() {
	  return contexts;
  }
  
  public ChannelInitializerFactoryStack getFactoryStack() { return factoryStack; }
  
  public ExecutorService getExecutor() { return executorService; }
  
  public /*Asynchronous*/ChannelGroup getChannelGroup() { return incomingChannelGroup; }


}
