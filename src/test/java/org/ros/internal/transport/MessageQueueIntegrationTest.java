/*
 * Copyright (C) 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.internal.transport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.jboss.netty.bootstrap.ServerBootstrap;
//import org.jboss.netty.buffer.HeapChannelBufferFactory;
//import org.jboss.netty.channel.Channel;
//import org.jboss.netty.channel.ChannelHandlerContext;
//import org.jboss.netty.channel.ChannelPipeline;
//import org.jboss.netty.channel.ChannelStateEvent;
//import org.jboss.netty.channel.ExceptionEvent;
//import org.jboss.netty.channel.SimpleChannelHandler;
//import org.jboss.netty.channel.group.ChannelGroup;
//import org.jboss.netty.channel.group.DefaultChannelGroup;
//import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ros.concurrent.CancellableLoop;
import org.ros.internal.message.Message;
import org.ros.internal.message.definition.MessageDefinitionReflectionProvider;
import org.ros.internal.message.topic.TopicMessageFactory;
import org.ros.internal.node.service.ServiceManager;
import org.ros.internal.node.topic.TopicParticipantManager;
import org.ros.internal.transport.queue.IncomingMessageQueue;
import org.ros.internal.transport.queue.OutgoingMessageQueue;
import org.ros.internal.transport.tcp.NamedChannelHandler;
import org.ros.internal.transport.tcp.TcpClient;
import org.ros.internal.transport.tcp.TcpClientManager;
import org.ros.internal.transport.tcp.TcpServerPipelineFactory;
import org.ros.message.MessageDefinitionProvider;
import org.ros.message.MessageIdentifier;
import org.ros.message.MessageListener;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.EventExecutor;

import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class MessageQueueIntegrationTest {

  private static final boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(MessageQueueIntegrationTest.class);

  private static final int QUEUE_CAPACITY = 128;

  //private ExecutorService executorService;
  private NioEventLoopGroup executorService;
  private TcpClientManager firstTcpClientManager;
  private TcpClientManager secondTcpClientManager;
  private OutgoingMessageQueue<Message> outgoingMessageQueue;
  private IncomingMessageQueue<std_msgs.String> firstIncomingMessageQueue;
  private IncomingMessageQueue<std_msgs.String> secondIncomingMessageQueue;
  private std_msgs.String expectedMessage;
  
  private InetSocketAddress isock = null;

  private class ServerHandler extends ChannelInboundHandlerAdapter /*SimpleChannelHandler*/ {
    @Override
    //public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      if (DEBUG) {
        log.info("Channel connected: " + ctx.channel().toString());
      }
      //Channel channel = e.getChannel();
      outgoingMessageQueue.addChannel(ctx.channel());
      super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx)
        throws Exception {
      if (DEBUG) {
        log.info("Channel disconnected: " + ctx.channel().toString());
      }
      super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
      if (DEBUG) {
        log.info("Channel exception: " + ctx.channel().toString());
      }
      ctx.channel().close();
      throw new RuntimeException(e.getCause());
    }
  }

  @Before
  public void setup() {
    //executorService = Executors.newCachedThreadPool();
	executorService = new NioEventLoopGroup();
    MessageDefinitionProvider messageDefinitionProvider = new MessageDefinitionReflectionProvider();
    TopicMessageFactory topicMessageFactory = new TopicMessageFactory(messageDefinitionProvider);
    expectedMessage = topicMessageFactory.newFromType(std_msgs.String._TYPE);
    expectedMessage.setData("Would you like to play a game?");
    outgoingMessageQueue =
        new OutgoingMessageQueue<Message>( executorService);
    firstIncomingMessageQueue =
        new IncomingMessageQueue<std_msgs.String>(executorService);
    secondIncomingMessageQueue =
        new IncomingMessageQueue<std_msgs.String>(executorService);
    firstTcpClientManager = new TcpClientManager(executorService.next());
    firstTcpClientManager.addNamedChannelHandler(firstIncomingMessageQueue.getMessageReceiver());
    secondTcpClientManager = new TcpClientManager(executorService.next());
    secondTcpClientManager.addNamedChannelHandler(secondIncomingMessageQueue.getMessageReceiver());
  }

  @After
  public void tearDown() {
    outgoingMessageQueue.shutdown();
    executorService.shutdown();
  
  }

  private void startRepeatingPublisher() {
    executorService.execute(new CancellableLoop() {
      @Override
      protected void loop() throws InterruptedException {
        outgoingMessageQueue.add(expectedMessage);
        Thread.sleep(100);
      }
    });
  }

  private ServerBootstrap buildServerChannel() {
	  isock = new InetSocketAddress(0);
    TopicParticipantManager topicParticipantManager = new TopicParticipantManager();
    ServiceManager serviceManager = new ServiceManager();
    //NioServerSocketChannelFactory channelFactory =
    //    new NioServerSocketChannelFactory(executorService, executorService);
    //ServerBootstrap bootstrap = new ServerBootstrap(channelFactory);
    ServerBootstrap bootstrap = new ServerBootstrap();
    bootstrap.group(executorService).
    	channel(NioServerSocketChannel.class).
    	option(ChannelOption.SO_BACKLOG, 100).
    	option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT).
    	localAddress(isock).
    	//childOption("child.bufferFactory",new HeapChannelBufferFactory(ByteOrder.LITTLE_ENDIAN).
    	childOption(ChannelOption.SO_KEEPALIVE, true);
    ChannelGroup serverChannelGroup = new DefaultChannelGroup(executorService.next());
    TcpServerPipelineFactory serverPipelineFactory =
        new TcpServerPipelineFactory(serverChannelGroup, topicParticipantManager, serviceManager) {
            @Override
            protected void initChannel(Channel ch) {
                ch.pipeline().remove(TcpServerPipelineFactory.HANDSHAKE_HANDLER);
                ch.pipeline().addLast( new ServerHandler());
            
            }
            /*
          //@Override
          public ChannelPipeline pipeline() {
            ChannelPipeline pipeline = super.getPipeline();
            // We're not interested firstIncomingMessageQueue testing the
            // handshake here. Removing it means connections are established
            // immediately.
            pipeline.remove(TcpServerPipelineFactory.HANDSHAKE_HANDLER);
            pipeline.addLast( new ServerHandler());
            return pipeline;
          }*/
        };
       bootstrap.childHandler(serverPipelineFactory);
    //bootstrap.setPipelineFactory(serverPipelineFactory);
    //Channel serverChannel = bootstrap.bind(new InetSocketAddress(0));
    
    return bootstrap;
  }

  private TcpClient connect(TcpClientManager TcpClientManager, ServerBootstrap serverChannel) {
    return TcpClientManager.connect("Foo", isock);
  }

  private CountDownLatch expectMessage(IncomingMessageQueue<std_msgs.String> incomingMessageQueue)
      throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    incomingMessageQueue.addListener(new MessageListener<std_msgs.String>() {
      @Override
      public void onNewMessage(std_msgs.String message) {
        assertEquals(message, expectedMessage);
        latch.countDown();
      }
    }, QUEUE_CAPACITY);
    return latch;
  }

  private void expectMessages() throws InterruptedException {
    CountDownLatch firstLatch = expectMessage(firstIncomingMessageQueue);
    CountDownLatch secondLatch = expectMessage(secondIncomingMessageQueue);
    assertTrue(firstLatch.await(3, TimeUnit.SECONDS));
    assertTrue(secondLatch.await(3, TimeUnit.SECONDS));
  }

  @Test
  public void testSendAndReceiveMessage() throws InterruptedException {
    startRepeatingPublisher();
    ServerBootstrap serverChannel = buildServerChannel();
    try {
    connect(firstTcpClientManager, serverChannel);
    connect(secondTcpClientManager, serverChannel);
    expectMessages();
    ChannelFuture f = serverChannel.bind().sync();
    // Wait until the server socket is closed.
    f.channel().closeFuture().sync();
    } finally {
        // Shut down all event loops to terminate all threads.
        executorService.shutdownGracefully();
        // Wait until all threads are terminated.
        executorService.terminationFuture().sync();
    }
  }

  @Test
  public void testSendAndReceiveLatchedMessage() throws InterruptedException {
    // Setting latched mode and writing a message should cause any
    // IncomingMessageQueues that connect in the future to receive the message.
    outgoingMessageQueue.setLatchMode(true);
    outgoingMessageQueue.add(expectedMessage);
    ServerBootstrap serverChannel = buildServerChannel();
    try {
    firstIncomingMessageQueue.setLatchMode(true);
    secondIncomingMessageQueue.setLatchMode(true);
    connect(firstTcpClientManager, serverChannel);
    connect(secondTcpClientManager, serverChannel);
    // The first set of incoming messages could either be from the
    // OutgoingMessageQueue latching or the Subscriber latching. This is
    // equivalent to waiting for the message to arrive and ensures that we've
    // latched it in.
    expectMessages();
    // The second set of incoming messages can only be from the
    // IncomingMessageQueue latching since we only sent one message.
    expectMessages();
    ChannelFuture f = serverChannel.bind().sync();
    // Wait until the server socket is closed.
    f.channel().closeFuture().sync();
    } finally {
        // Shut down all event loops to terminate all threads.
        executorService.shutdownGracefully();
        // Wait until all threads are terminated.
        executorService.terminationFuture().sync();
       
    }
  }

  @Test
  public void testSendAfterIncomingQueueShutdown() throws InterruptedException {
    startRepeatingPublisher();
    ServerBootstrap serverChannel = buildServerChannel();
    try {
    connect(firstTcpClientManager, serverChannel);
    firstTcpClientManager.shutdown();
    outgoingMessageQueue.add(expectedMessage);
    ChannelFuture f = serverChannel.bind().sync();
    // Wait until the server socket is closed.
    f.channel().closeFuture().sync();
    } finally {
        // Shut down all event loops to terminate all threads.
        executorService.shutdownGracefully();     
        // Wait until all threads are terminated.
        executorService.terminationFuture().sync();
 
    }
  }

  @Test
  public void testSendAfterServerChannelClosed() throws InterruptedException {
    startRepeatingPublisher();
    ServerBootstrap serverChannel = buildServerChannel();
    connect(firstTcpClientManager, serverChannel);
    //assertTrue(serverChannel.close().await(1, TimeUnit.SECONDS));
    // Start the server.
    ChannelFuture f = serverChannel.bind().sync();
    // Shut down all event loops to terminate all threads.
    executorService.shutdownGracefully();
    // Wait until all threads are terminated.
    executorService.terminationFuture().sync();

    // Wait until the server socket is closed.
    f.channel().closeFuture().sync().await(1, TimeUnit.SECONDS);
    outgoingMessageQueue.add(expectedMessage);
  }

  @Test
  public void testSendAfterOutgoingQueueShutdown() throws InterruptedException {
    startRepeatingPublisher();
    try {
    ServerBootstrap serverChannel = buildServerChannel();
    connect(firstTcpClientManager, serverChannel);
    outgoingMessageQueue.shutdown();
    outgoingMessageQueue.add(expectedMessage);
    ChannelFuture f = serverChannel.bind().sync();
    // Wait until the server socket is closed.
    f.channel().closeFuture().sync();
    } finally {
        // Shut down all event loops to terminate all threads.
        executorService.shutdownGracefully();
        // Wait until all threads are terminated.
        executorService.terminationFuture().sync();
    }
  }
}
