package org.ros.internal.node.topic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.concurrent.ListenerGroup;
import org.ros.concurrent.SignalRunnable;
import org.ros.internal.node.server.NodeIdentifier;
import org.ros.internal.transport.ProtocolNames;
import org.ros.internal.transport.queue.IncomingMessageQueue;
import org.ros.internal.transport.tcp.TcpClientManager;
import org.ros.message.MessageListener;
import org.ros.node.topic.DefaultSubscriberListener;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;
import org.ros.node.topic.SubscriberListener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of a {@link Subscriber}.<br/>
 * Primary players are knownPublishers, which is a Set of PublisherIdentifiers,<br/>
 * and TcpClientManager, which has the NamedChannelHandlers.<br/>
 * Here, we also maintain the incomingMessageQueue, which contains MessageListeners of the type this
 * class is parameterized with.<br/>
 *  
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class DefaultSubscriber<T> extends DefaultTopicParticipant implements Subscriber<T> {
	private static boolean DEBUG = true;
	private static final Log log = LogFactory.getLog(DefaultSubscriber.class);

  /**
   * The maximum delay before shutdown will begin even if all
   * {@link SubscriberListener}s have not yet returned from their
   * {@link SubscriberListener#onShutdown(Subscriber)} callback.
   */
  private static final int DEFAULT_SHUTDOWN_TIMEOUT = 5;
  private static final TimeUnit DEFAULT_SHUTDOWN_TIMEOUT_UNITS = TimeUnit.SECONDS;

  private final NodeIdentifier nodeIdentifier;
  private final ScheduledExecutorService executorService;
  private final IncomingMessageQueue<T> incomingMessageQueue;
 // private final Set<PublisherIdentifier> knownPublishers;
  private final TcpClientManager tcpClientManager;
  private final TopicParticipantManager topicParticipantManager;
  private final Object mutex;

  /**
   * Manages the {@link SubscriberListener}s for this {@link Subscriber}.
   */
  private final ListenerGroup<SubscriberListener<T>> subscriberListeners;

  //public static <S> DefaultSubscriber<S> newDefault(NodeIdentifier nodeIdentifier,
  //    TopicDeclaration description, ScheduledExecutorService executorService) throws IOException {
  //  return new DefaultSubscriber<S>(nodeIdentifier, description, executorService);
  //}
  public static <S> DefaultSubscriber<S> newDefault(NodeIdentifier nodeIdentifier,
			TopicDeclaration description,
			TopicParticipantManager topicParticipantManager,
			ScheduledExecutorService executorService) throws IOException {
	  return new DefaultSubscriber<S>(nodeIdentifier, description, topicParticipantManager, executorService);
  }
  private DefaultSubscriber(NodeIdentifier nodeIdentifier, TopicDeclaration topicDeclaration, 
		  TopicParticipantManager topicParticipantManager, ScheduledExecutorService executorService) throws IOException {
    super(topicDeclaration);
    this.nodeIdentifier = nodeIdentifier;
    this.executorService = executorService;
    incomingMessageQueue = new IncomingMessageQueue<T>(executorService);
    //knownPublishers = new HashSet<PublisherIdentifier>();
    tcpClientManager = new TcpClientManager/*.getInstance*/(executorService);
    this.topicParticipantManager = topicParticipantManager;
    mutex = new Object();
    SubscriberHandshakeHandler<T> subscriberHandshakeHandler =
        new SubscriberHandshakeHandler<T>(toDeclaration().toConnectionHeader(),
            incomingMessageQueue, executorService);
    tcpClientManager.addNamedChannelHandler(subscriberHandshakeHandler);
    subscriberListeners = new ListenerGroup<SubscriberListener<T>>(executorService);
    subscriberListeners.add(new DefaultSubscriberListener<T>() {
      @Override
      public void onMasterRegistrationSuccess(Subscriber<T> registrant) {
    	  if(DEBUG)
    		  log.info("Subscriber registered: " + DefaultSubscriber.this);
      }

      @Override
      public void onMasterRegistrationFailure(Subscriber<T> registrant) {
    	  if(DEBUG)
    		  log.info("Subscriber registration failed: " + DefaultSubscriber.this);
      }

      @Override
      public void onMasterUnregistrationSuccess(Subscriber<T> registrant) {
    	  if(DEBUG)
    		  log.info("Subscriber unregistered: " + DefaultSubscriber.this);
      }

      @Override
      public void onMasterUnregistrationFailure(Subscriber<T> registrant) {
    	  if(DEBUG)
    		  log.info("Subscriber unregistration failed: " + DefaultSubscriber.this);
      }
    });
  }

  public SubscriberIdentifier toIdentifier() {
    return new SubscriberIdentifier(nodeIdentifier, getTopicDeclaration().getIdentifier());
  }

  public SubscriberDeclaration toDeclaration() {
    return new SubscriberDeclaration(toIdentifier(), getTopicDeclaration());
  }

  public Collection<String> getSupportedProtocols() {
    return ProtocolNames.SUPPORTED;
  }

  @Override
  public boolean getLatchMode() {
    return incomingMessageQueue.getLatchMode();
  }

  @Override
  public void addMessageListener(MessageListener<T> messageListener, int limit) {
    incomingMessageQueue.addListener(messageListener, limit);
  }

  @Override
  public void addMessageListener(MessageListener<T> messageListener) {
    addMessageListener(messageListener, 1);
  }
  /**
   * When the SlaveClient requests a topic from the publisher in UpdatePublisherRunnable, as
   * happens when the method updatePublishers is called here, this method is called back on reply from master.
   * TcpClientManager calls connect to the passed InetSocketAddress. After that, all the SubscriberListeners are
   * signaled with the new Publisher.
   * @param publisherIdentifier
   * @param address
   * @throws Exception
   */
  public void addPublisher(PublisherIdentifier publisherIdentifier, InetSocketAddress address) throws Exception {
    synchronized (mutex) {
      // TODO(damonkohler): If the connection is dropped, knownPublishers should
      // be updated.
      //if (knownPublishers.contains(publisherIdentifier)) {
      //  return;
      //}
    	Collection<PublisherIdentifier> pubs = topicParticipantManager.getSubscriberConnections(this);
    	if(pubs != null && pubs.contains(publisherIdentifier)) {
    		log.info("Defaultsubscriber addPublisher topicParticipantManager CONTAINS "+publisherIdentifier+" at "+address);
    	} else {
      		log.info("Defaultsubscriber addPublisher topicParticipantManager DOES NOT CONTAIN "+publisherIdentifier+" at "+address);
      		topicParticipantManager.addSubscriberConnection(this, publisherIdentifier);
    	}
      tcpClientManager.connect(toString(), address);
      signalOnNewPublisher(publisherIdentifier);
    }
  }

  /**
   * Updates the list of {@link Publisher}s for the topic that this
   * {@link Subscriber} is interested in.<p/>
   * Creates UpdatePublisherRunnable of this classes generic type for each PublisherIdentifier.
   * Using executorService, spin the runnable which creates SlaveClient of type SlaveRpcEndpoint.
   * This is invoked from client Registrar when the onSubscriberAdded event occurs, and
   * from the SlaveServer when publisherUpdate is called.<br/>
   * @param publisherIdentifiers
   *          {@link Collection} of {@link PublisherIdentifier}s for the
   *          subscribed topic
   */
  public void updatePublishers(Collection<PublisherIdentifier> publisherIdentifiers) {
    for (final PublisherIdentifier publisherIdentifier : publisherIdentifiers) {
      executorService.execute(new UpdatePublisherRunnable<T>(this, nodeIdentifier,
          publisherIdentifier));
    }
  }

  @Override
  public void shutdown(long timeout, TimeUnit unit) {
    signalOnShutdown(timeout, unit);
    incomingMessageQueue.shutdown();
    tcpClientManager.shutdown();
    subscriberListeners.shutdown();
  }

  @Override
  public void shutdown() {
    shutdown(DEFAULT_SHUTDOWN_TIMEOUT, DEFAULT_SHUTDOWN_TIMEOUT_UNITS);
  }

  @Override
  public void addSubscriberListener(SubscriberListener<T> listener) {
    subscriberListeners.add(listener);
  }

  /**
   * Signal all {@link SubscriberListener}s that the {@link Subscriber} has
   * successfully registered with the master.
   * <p>
   * Each listener is called in a separate thread.
   */
  @Override
  public void signalOnMasterRegistrationSuccess() {
    final Subscriber<T> subscriber = this;
    subscriberListeners.signal(new SignalRunnable<SubscriberListener<T>>() {
      @Override
      public void run(SubscriberListener<T> listener) {
        listener.onMasterRegistrationSuccess(subscriber);
      }
    });
  }

  /**
   * Signal all {@link SubscriberListener}s that the {@link Subscriber} has
   * failed to register with the master.
   * 
   * <p>
   * Each listener is called in a separate thread.
   */
  @Override
  public void signalOnMasterRegistrationFailure() {
    final Subscriber<T> subscriber = this;
    subscriberListeners.signal(new SignalRunnable<SubscriberListener<T>>() {
      @Override
      public void run(SubscriberListener<T> listener) {
        listener.onMasterRegistrationFailure(subscriber);
      }
    });
  }

  /**
   * Signal all {@link SubscriberListener}s that the {@link Subscriber} has
   * successfully unregistered with the master.
   * <p>
   * Each listener is called in a separate thread.
   */
  @Override
  public void signalOnMasterUnregistrationSuccess() {
    final Subscriber<T> subscriber = this;
    subscriberListeners.signal(new SignalRunnable<SubscriberListener<T>>() {
      @Override
      public void run(SubscriberListener<T> listener) {
        listener.onMasterUnregistrationSuccess(subscriber);
      }
    });
  }

  /**
   * Signal all {@link SubscriberListener}s that the {@link Subscriber} has
   * failed to unregister with the master.
   * <p>
   * Each listener is called in a separate thread.
   */
  @Override
  public void signalOnMasterUnregistrationFailure() {
    final Subscriber<T> subscriber = this;
    subscriberListeners.signal(new SignalRunnable<SubscriberListener<T>>() {
      @Override
      public void run(SubscriberListener<T> listener) {
        listener.onMasterUnregistrationFailure(subscriber);
      }
    });
  }

  /**
   * Signal all {@link SubscriberListener}s that a new {@link Publisher} has
   * connected.
   * <p>
   * Each listener is called in a separate thread.
   */
  public void signalOnNewPublisher(final PublisherIdentifier publisherIdentifier) {
    final Subscriber<T> subscriber = this;
    subscriberListeners.signal(new SignalRunnable<SubscriberListener<T>>() {
      @Override
      public void run(SubscriberListener<T> listener) {
        listener.onNewPublisher(subscriber, publisherIdentifier);
      }
    });
  }

  /**
   * Signal all {@link SubscriberListener}s that the {@link Subscriber} has shut
   * down.
   * <p>
   * Each listener is called in a separate thread.
   */
  private void signalOnShutdown(long timeout, TimeUnit unit) {
    final Subscriber<T> subscriber = this;
    try {
      subscriberListeners.signal(new SignalRunnable<SubscriberListener<T>>() {
        @Override
        public void run(SubscriberListener<T> listener) {
          listener.onShutdown(subscriber);
        }
      }, timeout, unit);
    } catch (InterruptedException e) {
      // Ignored since we do not guarantee that all listeners will finish before
      // shutdown begins.
    }
  }

  @Override
  public String toString() {
    return "Subscriber<" + getTopicDeclaration() + ">";
  }

}
