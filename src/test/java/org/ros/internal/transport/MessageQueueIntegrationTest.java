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
import org.ros.exception.RosRuntimeException;
import org.ros.internal.message.Message;
import org.ros.internal.message.definition.MessageDefinitionReflectionProvider;
import org.ros.internal.message.topic.TopicMessageFactory;
import org.ros.internal.node.service.ServiceManager;
import org.ros.internal.node.topic.TopicParticipantManager;
import org.ros.internal.transport.queue.IncomingMessageQueue;
import org.ros.internal.transport.queue.OutgoingMessageQueue;
import org.ros.internal.transport.tcp.ChannelGroup;
import org.ros.internal.transport.tcp.ChannelGroupImpl;
import org.ros.internal.transport.tcp.ChannelInitializerFactoryStack;
import org.ros.internal.transport.tcp.NamedChannelHandler;
import org.ros.internal.transport.tcp.TcpClient;
import org.ros.internal.transport.tcp.TcpClientManager;
import org.ros.internal.transport.tcp.TcpServerPipelineFactory;
import org.ros.message.MessageDefinitionProvider;
import org.ros.message.MessageIdentifier;
import org.ros.message.MessageListener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteOrder;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author jg
 */
public class MessageQueueIntegrationTest {

  private static final boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(MessageQueueIntegrationTest.class);

  private static final int QUEUE_CAPACITY = 128;

  private ExecutorService executorService;
  private TcpClientManager firstTcpClientManager;
  private TcpClientManager secondTcpClientManager;
  private OutgoingMessageQueue<Message> outgoingMessageQueue;
  private IncomingMessageQueue<std_msgs.String> firstIncomingMessageQueue;
  private IncomingMessageQueue<std_msgs.String> secondIncomingMessageQueue;
  private std_msgs.String expectedMessage;
  private ArrayBlockingQueue<ChannelHandlerContext> channelHandlerContexts = new ArrayBlockingQueue<ChannelHandlerContext>(1024);
  
  private InetSocketAddress isock = null;

  private class ServerHandler implements ChannelHandler {
    @Override
    //public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      if (DEBUG) {
        log.info("Channel connected: " + ctx.channel().toString());
      }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx)
        throws Exception {
      if (DEBUG) {
        log.info("Channel disconnected: " + ctx.channel().toString());
      }
      
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
      if (DEBUG) {
        log.info("Channel exception: " + ctx.channel().toString());
      }
      ctx.channel().close();
      throw new RuntimeException(e.getCause());
    }

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object event)
			throws Exception {
		// TODO Auto-generated method stub
		
	}
  }

  @Before
  public void setup() {
	try {
    executorService = Executors.newCachedThreadPool();
    MessageDefinitionProvider messageDefinitionProvider = new MessageDefinitionReflectionProvider();
    TopicMessageFactory topicMessageFactory = new TopicMessageFactory(messageDefinitionProvider);
    expectedMessage = topicMessageFactory.newFromType(std_msgs.String._TYPE);
    expectedMessage.setData("Would you like to play a game?");
    outgoingMessageQueue =
        new OutgoingMessageQueue<Message>( executorService, channelHandlerContexts);
    firstIncomingMessageQueue =
        new IncomingMessageQueue<std_msgs.String>(executorService);
    secondIncomingMessageQueue =
        new IncomingMessageQueue<std_msgs.String>(executorService);
    firstTcpClientManager = TcpClientManager.getInstance(executorService);
    firstTcpClientManager.addNamedChannelHandler(firstIncomingMessageQueue.getMessageReceiver());
    secondTcpClientManager = TcpClientManager.getInstance(executorService);
    secondTcpClientManager.addNamedChannelHandler(secondIncomingMessageQueue.getMessageReceiver());
	} catch(Exception e) { throw new RosRuntimeException(e); }
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

  private ChannelHandlerContext buildServerChannel() {
	isock = new InetSocketAddress(0);
    TopicParticipantManager topicParticipantManager = new TopicParticipantManager();
    ServiceManager serviceManager = new ServiceManager();
	/*Asynchronous*/ChannelGroup incomingChannelGroup = null;
	incomingChannelGroup = new ChannelGroupImpl(executorService);/* AsynchronousChannelGroup.withThreadPool(executorService);*/
	ChannelInitializerFactoryStack  factoryStack = new ChannelInitializerFactoryStack();
  
    TcpServerPipelineFactory serverPipelineFactory =
        new TcpServerPipelineFactory(incomingChannelGroup, topicParticipantManager, serviceManager) {
            @Override
            protected void initChannel(ChannelHandlerContext ch) {
                ch.pipeline().remove(TcpServerPipelineFactory.HANDSHAKE_HANDLER);
                ch.pipeline().addLast(ServerHandler.class.getSimpleName(), new ServerHandler());
            }
        };
     factoryStack.addLast(serverPipelineFactory);
	  /*AsynchronousServer*/ServerSocket listener = null;
	try {
		listener = /*Asynchronous*/new ServerSocket(/*incomingChannelGroup*/);
	} catch (IOException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
	  try {
		listener.bind(isock);
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

     /*Future<AsynchronousSocketChannel*/ Socket channel = null;
	try {
		channel = listener.accept();
	} catch (IOException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
	  if( DEBUG ) {
		  log.debug("Accept "+channel);
	  }
     ChannelHandlerContextImpl ctx = null;
	ctx = new ChannelHandlerContextImpl(incomingChannelGroup,channel/*.get()*/ , executorService);
     return ctx;
  }

  private void connect(TcpClientManager TcpClientManager) throws Exception {
   TcpClientManager.connect("Foo", isock);
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
    channelHandlerContexts.add(buildServerChannel());
    try {
    connect(firstTcpClientManager);
    connect(secondTcpClientManager);
    expectMessages();
 
    } catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} finally {
        // Shut down all event loops to terminate all threads.
        executorService.shutdown();
        // Wait until all threads are terminated.
        executorService.awaitTermination(10, TimeUnit.SECONDS);
    }
  }

  @Test
  public void testSendAndReceiveLatchedMessage() throws InterruptedException {
    // Setting latched mode and writing a message should cause any
    // IncomingMessageQueues that connect in the future to receive the message.
    outgoingMessageQueue.setLatchMode(true);
    outgoingMessageQueue.add(expectedMessage);
    channelHandlerContexts.add(buildServerChannel());
    try {
    firstIncomingMessageQueue.setLatchMode(true);
    secondIncomingMessageQueue.setLatchMode(true);
    connect(firstTcpClientManager);
    connect(secondTcpClientManager);
    // The first set of incoming messages could either be from the
    // OutgoingMessageQueue latching or the Subscriber latching. This is
    // equivalent to waiting for the message to arrive and ensures that we've
    // latched it in.
    expectMessages();
    // The second set of incoming messages can only be from the
    // IncomingMessageQueue latching since we only sent one message.
    expectMessages();
  
    } catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} finally {
        // Shut down all event loops to terminate all threads.
        executorService.shutdown();
        // Wait until all threads are terminated.
        executorService.awaitTermination(10, TimeUnit.SECONDS);
       
    }
  }

  @Test
  public void testSendAfterIncomingQueueShutdown() throws InterruptedException {
    startRepeatingPublisher();
    channelHandlerContexts.add(buildServerChannel());
    try {
    connect(firstTcpClientManager);
    firstTcpClientManager.shutdown();
    outgoingMessageQueue.add(expectedMessage);
  
    // Wait until the server socket is closed.
    //f.channel().closeFuture().sync();
    } catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} finally {
        // Shut down all event loops to terminate all threads.
        executorService.shutdown();     
        // Wait until all threads are terminated.
        executorService.awaitTermination(10, TimeUnit.SECONDS);
 
    }
  }

  @Test
  public void testSendAfterServerChannelClosed() throws Exception {
    startRepeatingPublisher();
    channelHandlerContexts.add(buildServerChannel());
    connect(firstTcpClientManager);
    //assertTrue(serverChannel.close().await(1, TimeUnit.SECONDS));
    // Start the server.
    // Shut down all event loops to terminate all threads.
    executorService.shutdown();
    // Wait until all threads are terminated.
    executorService.awaitTermination(10, TimeUnit.SECONDS);

    // Wait until the server socket is closed.
    //f.channel().closeFuture().sync().await(1, TimeUnit.SECONDS);
    outgoingMessageQueue.add(expectedMessage);
  }

  @Test
  public void testSendAfterOutgoingQueueShutdown() throws Exception {
    startRepeatingPublisher();
    try {
    channelHandlerContexts.add(buildServerChannel());
    connect(firstTcpClientManager);
    outgoingMessageQueue.shutdown();
    outgoingMessageQueue.add(expectedMessage);
    // Wait until the server socket is closed.
    //f.channel().closeFuture().sync();
    } finally {
        // Shut down all event loops to terminate all threads.
        executorService.shutdown();
        // Wait until all threads are terminated.
        executorService.awaitTermination(10, TimeUnit.SECONDS);
    }
  }
}
