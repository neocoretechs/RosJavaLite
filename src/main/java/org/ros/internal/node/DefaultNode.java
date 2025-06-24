package org.ros.internal.node;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.Parameters;
import org.ros.concurrent.CancellableLoop;
import org.ros.concurrent.ListenerGroup;
import org.ros.concurrent.SignalRunnable;
import org.ros.exception.RemoteException;
import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.internal.message.service.ServiceDescription;
import org.ros.internal.message.topic.TopicDescription;
import org.ros.internal.node.client.MasterClient;
import org.ros.internal.node.client.Registrar;
import org.ros.internal.node.client.RelatrixClient;
import org.ros.internal.node.parameter.DefaultParameterTree;
import org.ros.internal.node.parameter.ParameterManager;
import org.ros.internal.node.response.Response;
import org.ros.internal.node.response.StatusCode;
import org.ros.internal.node.server.NodeIdentifier;
import org.ros.internal.node.server.SlaveServer;
import org.ros.internal.node.service.ServiceDeclaration;
import org.ros.internal.node.service.ServiceFactory;
import org.ros.internal.node.service.ServiceIdentifier;
import org.ros.internal.node.service.ServiceManager;
import org.ros.internal.node.topic.PublisherFactory;
import org.ros.internal.node.topic.SubscriberFactory;
import org.ros.internal.node.topic.TopicDeclaration;
import org.ros.internal.node.topic.TopicParticipantManager;

import org.ros.message.MessageFactory;
import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.namespace.NodeNameResolver;
import org.ros.node.ConnectedNode;
import org.ros.node.DefaultNodeFactory;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeListener;
import org.ros.node.parameter.ParameterTree;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseBuilder;
import org.ros.node.service.ServiceServer;
import org.ros.node.topic.DefaultPublisherListener;
import org.ros.node.topic.DefaultSubscriberListener;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;
import org.ros.time.ClockTopicTimeProvider;
import org.ros.time.TimeProvider;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The default implementation of a {@link Node}.
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2021
 * 
 */
public class DefaultNode implements ConnectedNode {
  private static final boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(DefaultNode.class);
  /**
   * The maximum delay before shutdown will begin even if all
   * {@link NodeListener}s have not yet returned from their
   * {@link NodeListener#onShutdown(Node)} callback.
   */
  private static final int MAX_SHUTDOWN_DELAY_DURATION = 5;
  private static final TimeUnit MAX_SHUTDOWN_DELAY_UNITS = TimeUnit.SECONDS;

  private final NodeConfiguration nodeConfiguration;
  private final ListenerGroup<NodeListener> nodeListeners;
  private final ScheduledExecutorService scheduledExecutorService;
  private final InetSocketAddress masterUri;
  private final MasterClient masterClient;
  private final TopicParticipantManager topicParticipantManager;
  private final ServiceManager serviceManager;
  private final ParameterManager parameterManager;
  private final GraphName nodeName;
  private final NodeNameResolver resolver;
  private final SlaveServer slaveServer;
  private ParameterTree parameterTree = null;
  private final PublisherFactory publisherFactory;
  private final SubscriberFactory subscriberFactory;
  private final ServiceFactory serviceFactory;
  private final Registrar registrar;
  private final RelatrixClient relatrixClient;

  private RosoutLogger rlog;
  private TimeProvider timeProvider;

  /**
   * {@link DefaultNode}s should only be constructed using the {@link DefaultNodeFactory}.
   * 
   * @param nodeConfiguration the {@link NodeConfiguration} for this {@link Node}, rpc bind/advertise and tcpros bind/advertise from here
   * @param nodeListeners a {@link Collection} of {@link NodeListener}s that will be added to this {@link Node} before it starts
   */
  public DefaultNode(NodeConfiguration nodeConfiguration, Collection<NodeListener> nodeListeners,
      ScheduledExecutorService scheduledExecutorService) {
    this.nodeConfiguration = NodeConfiguration.copyOf(nodeConfiguration);
    this.nodeListeners = new ListenerGroup<NodeListener>(scheduledExecutorService);
    this.nodeListeners.addAll(nodeListeners);
    this.scheduledExecutorService = scheduledExecutorService;
    this.masterUri = nodeConfiguration.getMasterUri();
    NodeIdentifier nodeIdentifier = null;
	this.masterClient = nodeConfiguration.getMasterClient();
	this.topicParticipantManager = nodeConfiguration.getTopicParticipantManager();
    this.serviceManager = nodeConfiguration.getServiceManager();
    this.relatrixClient = nodeConfiguration.getRelatrixClient();
    try {
    	this.slaveServer = nodeConfiguration.getSlaveServer();
    	this.parameterManager = nodeConfiguration.getParameterManager();
    	GraphName basename = nodeConfiguration.getNodeName();
    	this.nodeName = nodeConfiguration.getParentResolver().getNamespace().join(basename);
    	this.nodeConfiguration.setNodeName(nodeName);
    	this.resolver = new NodeNameResolver(nodeName, nodeConfiguration.getParentResolver());
    	// assign the slaveServer a nodename, now that we have one
    	this.nodeConfiguration.getSlaveServer().setNodeName(nodeName);
    	nodeIdentifier = this.slaveServer.toNodeIdentifier();
		this.parameterTree =
		    DefaultParameterTree.newFromNodeIdentifier(nodeIdentifier, masterClient.getRemoteUri(),
		        this.resolver, this.parameterManager);
		this.publisherFactory =
			        new PublisherFactory(nodeIdentifier, this.topicParticipantManager,
			            this.nodeConfiguration.getTopicMessageFactory(), this.scheduledExecutorService);
		this.subscriberFactory =
			        new SubscriberFactory(nodeIdentifier, this.topicParticipantManager, this.scheduledExecutorService);
		this.serviceFactory =
			        new ServiceFactory(this.nodeName, this.slaveServer, this.serviceManager, this.scheduledExecutorService);
	} catch (IOException e) {
		log.error("Cannot construct new node due to "+e,e);
		throw new RuntimeException(e);
	}
    /*
    try {
		masterClient = new MasterClient(masterUri, 60000, 60000);
	} catch (IOException e1) {
		log.error("Unknown host for master client:"+masterUri,e1);
		//e1.printStackTrace();
		throw new RosRuntimeException(e1);
	}
    topicParticipantManager = new TopicParticipantManager();
    serviceManager = new ServiceManager();
    parameterManager = new ParameterManager(scheduledExecutorService);

    GraphName basename = nodeConfiguration.getNodeName();
    NameResolver parentResolver = nodeConfiguration.getParentResolver();
    nodeName = parentResolver.getNamespace().join(basename);
    resolver = new NodeNameResolver(nodeName, parentResolver);
    try {
		slaveServer =
		    new SlaveServer(nodeName, nodeConfiguration.getTcpRosBindAddress(),
		        nodeConfiguration.getTcpRosAdvertiseAddress(),
		        nodeConfiguration.getRpcBindAddress(),
		        nodeConfiguration.getRpcAdvertiseAddress(), masterClient, topicParticipantManager,
		        serviceManager, parameterManager, scheduledExecutorService);
	} catch (IOException e) {
		log.error("Can not configure slave server from DefaultNode "+e,e);
		throw new RosRuntimeException(e);
	}
    // start TcpRosServer and SlaveServer
    slaveServer.start();

    NodeIdentifier nodeIdentifier = slaveServer.toNodeIdentifier();

    try {
		parameterTree =
		    DefaultParameterTree.newFromNodeIdentifier(nodeIdentifier, masterClient.getRemoteUri(),
		        resolver, parameterManager);
	} catch (IOException e) {
		log.error("Cannot construct parameter tree due to "+e,e);
	}
    */
  
    registrar = new Registrar(masterClient, scheduledExecutorService);
    topicParticipantManager.setListener(registrar);
    serviceManager.setListener(registrar);

    scheduledExecutorService.execute(new Runnable() {
      @Override
      public void run() {
        start();
      }
    });
  }
  
  private void start() {
    // The Registrar must be started first so that master registration is
    // possible during startup.
    registrar.start(slaveServer.toNodeIdentifier());

    // During startup, we wait for 1) the RosoutLogger and 2) the TimeProvider.
    final CountDownLatch latch = new CountDownLatch(2);

    try {
		rlog = new RosoutLogger(this);
	    rlog.getPublisher().addListener(new DefaultPublisherListener<rosgraph_msgs.Log>() {
	        @Override
	        public void onMasterRegistrationSuccess(Publisher<rosgraph_msgs.Log> registrant) {
	          latch.countDown();
	        }
	    });
	} catch (IOException e1) {
		log.error("The Ros Logger has failed to start.",e1);
		throw new RuntimeException(e1);
	}
    boolean useSimTime = false;
    try {
      useSimTime =
          parameterTree.has(Parameters.USE_SIM_TIME)
              && (Boolean)parameterTree.get(Parameters.USE_SIM_TIME, Boolean.FALSE);
    } catch (Exception e) {
      signalOnError(e);
    }
    if (useSimTime) {
      ClockTopicTimeProvider clockTopicTimeProvider = new ClockTopicTimeProvider(this);
      clockTopicTimeProvider.getSubscriber().addSubscriberListener(
          new DefaultSubscriberListener<rosgraph_msgs.Clock>() {
            @Override
            public void onMasterRegistrationSuccess(Subscriber<rosgraph_msgs.Clock> subscriber) {
              latch.countDown();
            }
          });
      timeProvider = clockTopicTimeProvider;
    } else {
      timeProvider = nodeConfiguration.getTimeProvider();
      latch.countDown();
    }

    try {
      latch.await();
    } catch (InterruptedException e) {
      signalOnError(e);
      shutdown();
      return;
    }

    signalOnStart();
    if(DEBUG)
    	log.debug("DefaultNode start signaled");
    	
  }

  Registrar getRegistrar() {
    return registrar;
  }

  
  @Override
  public <T> Publisher<T> newPublisher(GraphName topicName, String messageType) {
    GraphName resolvedTopicName = resolveName(topicName);
    TopicDescription topicDescription =
        nodeConfiguration.getTopicDescriptionFactory().newFromType(messageType);
    TopicDeclaration topicDeclaration =
        TopicDeclaration.newFromTopicName(resolvedTopicName, topicDescription);
    Publisher<T> publisher = null;
    try {
     publisher = publisherFactory.newOrExisting(topicDeclaration);
    } catch(IOException e) { throw new RosRuntimeException(e); }
    return publisher;
  }

  @Override
  public <T> Publisher<T> newPublisher(String topicName, String messageType) {
    return newPublisher(GraphName.of(topicName), messageType);
  }

  @Override
  public <T> Subscriber<T> newSubscriber(GraphName topicName, String messageType) {
    GraphName resolvedTopicName = resolveName(topicName);
    TopicDescription topicDescription =
        nodeConfiguration.getTopicDescriptionFactory().newFromType(messageType);
    TopicDeclaration topicDeclaration =
        TopicDeclaration.newFromTopicName(resolvedTopicName, topicDescription);
    Subscriber<T> subscriber = null;
    try {
    	subscriber = subscriberFactory.newOrExisting(topicDeclaration);
    } catch(IOException e) { throw new RosRuntimeException(e); }
    return subscriber;
  }

  @Override
  public <T> Subscriber<T> newSubscriber(String topicName, String messageType) {
    return newSubscriber(GraphName.of(topicName), messageType);
  }

  @Override
  public <T, S> ServiceServer<T, S> newServiceServer(GraphName serviceName, String serviceType,
      ServiceResponseBuilder<T, S> responseBuilder) {
    GraphName resolvedServiceName = resolveName(serviceName);
    // TODO(damonkohler): It's rather non-obvious that the URI will be
    // created later on the fly.
    ServiceIdentifier identifier = new ServiceIdentifier(resolvedServiceName, null);
    ServiceDescription serviceDescription =
        nodeConfiguration.getServiceDescriptionFactory().newFromType(serviceType);
    ServiceDeclaration definition = new ServiceDeclaration(identifier, serviceDescription);
    return serviceFactory.newServer(definition, responseBuilder, nodeConfiguration.getServiceResponseMessageFactory());
  }

  @Override
  public <T, S> ServiceServer<T, S> newServiceServer(String serviceName, String serviceType,
      ServiceResponseBuilder<T, S> responseBuilder) {
    return newServiceServer(GraphName.of(serviceName), serviceType, responseBuilder);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T, S> ServiceServer<T, S> getServiceServer(GraphName serviceName) {
    return (ServiceServer<T, S>) serviceManager.getServer(serviceName);
  }

  @Override
  public <T, S> ServiceServer<T, S> getServiceServer(String serviceName) {
    return getServiceServer(GraphName.of(serviceName));
  }

  @Override
  public InetSocketAddress lookupServiceUri(GraphName serviceName) {
    Response<InetSocketAddress> response =
        masterClient.lookupService(slaveServer.toNodeIdentifier().getName(),
            resolveName(serviceName).toString());
    if (response.getStatusCode() == StatusCode.SUCCESS) {
      return response.getResult();
    } else {
      return null;
    }
  }

  @Override
  public InetSocketAddress lookupServiceUri(String serviceName) {
    return lookupServiceUri(GraphName.of(serviceName));
  }

  @Override
  public <T, S> ServiceClient<T, S> newServiceClient(GraphName serviceName, String serviceType) throws Exception {
    GraphName resolvedServiceName = resolveName(serviceName);
    InetSocketAddress uri = lookupServiceUri(resolvedServiceName);
    if (uri == null) {
      throw new ServiceNotFoundException("No such service " + resolvedServiceName + " of type "
          + serviceType);
    }
    ServiceDescription serviceDescription =
        nodeConfiguration.getServiceDescriptionFactory().newFromType(serviceType);
    ServiceIdentifier serviceIdentifier = new ServiceIdentifier(resolvedServiceName, uri);
    ServiceDeclaration definition = new ServiceDeclaration(serviceIdentifier, serviceDescription);
    return serviceFactory.newClient(definition, nodeConfiguration.getServiceRequestMessageFactory());
  }

  @Override
  public <T, S> ServiceClient<T, S> newServiceClient(String serviceName, String serviceType) throws Exception {
    return newServiceClient(GraphName.of(serviceName), serviceType);
  }

  @Override
  public Time getCurrentTime() {
    return timeProvider.getCurrentTime();
  }

  @Override
  public GraphName getName() {
    return nodeName;
  }

  @Override
  public Log getLog() {
    return rlog;
  }

  @Override
  public GraphName resolveName(GraphName name) {
    return resolver.resolve(name);
  }

  @Override
  public GraphName resolveName(String name) {
    return resolver.resolve(GraphName.of(name));
  }

  @Override
  public void shutdown() {
    signalOnShutdown();
    // NOTE(damonkohler): We don't want to raise potentially spurious
    // exceptions during shutdown that would interrupt the process. This is
    // simply best effort cleanup.
    for (Publisher<?> publisher : topicParticipantManager.getPublishers()) {
      publisher.shutdown();
    }
    for (Subscriber<?> subscriber : topicParticipantManager.getSubscribers()) {
      subscriber.shutdown();
    }
    for (ServiceServer<?, ?> serviceServer : serviceManager.getServers()) {
      try {
        Response<Integer> response =
            masterClient.unregisterService(slaveServer.toNodeIdentifier(), serviceServer);
        if (DEBUG) {
          if (response.getResult().toString().equals("0")) {
            log.error("Failed to unregister service: " + serviceServer.getName());
          }
        }
      } catch (RemoteException e) {
        rlog.error(e);
      }
    }
    for (ServiceClient<?, ?> serviceClient : serviceManager.getClients()) {
      serviceClient.shutdown();
    }
    registrar.shutdown();
    try {
		slaveServer.shutdown();
	} catch (IOException e) {
	}
    signalOnShutdownComplete();
  }

  @Override
  public InetSocketAddress getMasterUri() {
    return masterUri;
  }

  @Override
  public NodeNameResolver getResolver() {
    return resolver;
  }

  @Override
  public ParameterTree getParameterTree() {
    return parameterTree;
  }

  @Override
  public InetSocketAddress getUri() {
    return slaveServer.getUri();
  }

  @Override
  public NodeConfiguration getNodeConfiguration() {
  	return nodeConfiguration;
  }
  
  @Override
  public MessageFactory getTopicMessageFactory() {
    return nodeConfiguration.getTopicMessageFactory();
  }

  @Override
  public MessageFactory getServiceRequestMessageFactory() {
    return nodeConfiguration.getServiceRequestMessageFactory();
  }

  @Override
  public MessageFactory getServiceResponseMessageFactory() {
    return nodeConfiguration.getServiceResponseMessageFactory();
  }

  @Override
  public void addListener(NodeListener listener) {
    nodeListeners.add(listener);
  }

  /**
   * SignalRunnable all {@link NodeListener}s that the {@link Node} has
   * experienced an error.
   * <p>
   * Each listener is called in a separate thread.
   */
  private void signalOnError(final Throwable throwable) {
    final Node node = this;
    nodeListeners.signal(new SignalRunnable<NodeListener>() {
      @Override
      public void run(NodeListener listener) {
        listener.onError(node, throwable);
      }
    });
  }

  /**
   * SignalRunnable all {@link NodeListener}s that the {@link Node} has started.
   * <p>
   * Each listener is called in a separate thread.
   */
  private void signalOnStart() {
    final ConnectedNode connectedNode = this;
    nodeListeners.signal(new SignalRunnable<NodeListener>() {
      @Override
      public void run(NodeListener listener) {
        listener.onStart(connectedNode);
      }
    });
  }

  /**
   * SignalRunnable all {@link NodeListener}s that the {@link Node} has started
   * shutting down.
   * <p>
   * Each listener is called in a separate thread.
   */
  private void signalOnShutdown() {
    final Node node = this;
    try {
      nodeListeners.signal(new SignalRunnable<NodeListener>() {
        @Override
        public void run(NodeListener listener) {
          listener.onShutdown(node);
        }
      }, MAX_SHUTDOWN_DELAY_DURATION, MAX_SHUTDOWN_DELAY_UNITS);
    } catch (InterruptedException e) {
      // Ignored since we do not guarantee that all listeners will finish
      // before
      // shutdown begins.
    }
  }

  /**
   * SignalRunnable all {@link NodeListener}s that the {@link Node} has shut
   * down.
   * <p>
   * Each listener is called in a separate thread.
   */
  private void signalOnShutdownComplete() {
    final Node node = this;
    nodeListeners.signal(new SignalRunnable<NodeListener>() {
      @Override
      public void run(NodeListener listener) {
        try {
          listener.onShutdownComplete(node);
        } catch (Throwable e) {
          log.error(listener);
        }
      }
    });
  }


  InetSocketAddress getAddress() {
    return slaveServer.getAddress();
  }

  @Override
  public ScheduledExecutorService getScheduledExecutorService() {
    return scheduledExecutorService;
  }

  @Override
  public void executeCancellableLoop(final CancellableLoop cancellableLoop) {
    scheduledExecutorService.execute(cancellableLoop);
    addListener(new NodeListener() {
      @Override
      public void onStart(ConnectedNode connectedNode) {
      }

      @Override
      public void onShutdown(Node node) {
        cancellableLoop.cancel();
      }

      @Override
      public void onShutdownComplete(Node node) {
      }

      @Override
      public void onError(Node node, Throwable throwable) {
        cancellableLoop.cancel();
      }
    });
  }


}
