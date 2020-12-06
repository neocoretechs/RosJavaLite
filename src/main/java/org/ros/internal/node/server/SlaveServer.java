package org.ros.internal.node.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.address.AdvertiseAddress;
import org.ros.address.BindAddress;
import org.ros.internal.node.client.MasterClient;
import org.ros.internal.node.parameter.ParameterManager;
import org.ros.internal.node.service.ServiceManager;
import org.ros.internal.node.topic.DefaultPublisher;
import org.ros.internal.node.topic.DefaultSubscriber;
import org.ros.internal.node.topic.PublisherIdentifier;
import org.ros.internal.node.topic.SubscriberIdentifier;
import org.ros.internal.node.topic.TopicDeclaration;
import org.ros.internal.node.topic.TopicParticipantManager;
import org.ros.internal.system.Process;
import org.ros.internal.transport.ChannelHandlerContext;
import org.ros.internal.transport.ProtocolDescription;
import org.ros.internal.transport.ProtocolNames;
import org.ros.internal.transport.tcp.TcpRosProtocolDescription;
import org.ros.internal.transport.tcp.TcpRosServer;
import org.ros.namespace.GraphName;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;

/**
 * SlaveServer is the remote execution endpoint.<br/> This class is reflected for its invokable
 * methods and that is made available to remote clients as the callable remote procedures.<br/>
 * A remote client can request connections, get collections of publishers and subscriber and information
 * about the state of the bus including connections between publishers and subscribers. TcpRosServer is
 * the class that does most of the work and is wrapped by this class, which is here to provide the subset of
 * remotely invokable methods via reflection using the ServerInvokeMethod class.
 * @see ServerInvokeMethod
 * @see TcpRosServer
 * @author jg
 */
public class SlaveServer extends RpcServer {
  private static boolean DEBUG = true;
  private static final Log log = LogFactory.getLog(SlaveServer.class);
  private final GraphName nodeName;
  private final MasterClient masterClient;
  private final TopicParticipantManager topicParticipantManager;
  private final ServiceManager serviceManager;
  private final ParameterManager parameterManager;
  private final TcpRosServer tcpRosServer;
  private ServerInvokeMethod invokableMethods;

  public SlaveServer(GraphName nodeName, BindAddress tcpRosBindAddress,
      AdvertiseAddress tcpRosAdvertiseAddress, BindAddress rpcBindAddress,
      AdvertiseAddress rpcAdvertiseAddress, MasterClient master,
      TopicParticipantManager topicParticipantManager, ServiceManager serviceManager,
      ParameterManager parameterManager, ScheduledExecutorService executorService) throws IOException {
    super(rpcBindAddress, rpcAdvertiseAddress);
    this.nodeName = nodeName;
    this.masterClient = master;
    this.topicParticipantManager = topicParticipantManager;
    this.serviceManager = serviceManager;
    this.parameterManager = parameterManager;
    try {
		invokableMethods = new ServerInvokeMethod(this.getClass().getName(), 0);
	} catch (ClassNotFoundException e) {
		throw new IOException(e);
	}  
    this.tcpRosServer =
        new TcpRosServer(tcpRosBindAddress, tcpRosAdvertiseAddress, topicParticipantManager, serviceManager, executorService);
    if(DEBUG) {
    	log.info("ADDRESSES SlaveServer ctor:"+nodeName+" TCPBind:"+tcpRosBindAddress+", TCPRosAdv:"+tcpRosAdvertiseAddress+", RPCBind:"+rpcBindAddress+", RPCAdv:"+rpcAdvertiseAddress);
    	log.info("MANAGERS SlaveServer ctor:"+nodeName+" MasterClient:"+master+" TopicParticipantManager:"+topicParticipantManager+
    			" ServiceManager:"+serviceManager+" ParameterManager:"+parameterManager+" SchedulaedExecutorService:"+executorService);
    }
  }

  public AdvertiseAddress getTcpRosAdvertiseAddress() {
    return tcpRosServer.getAdvertiseAddress();
  }
  /**
   * Return the ChannelHandlerContext array of subscribers
   * @return
   */
  public ArrayBlockingQueue<ChannelHandlerContext> getSubscribers() {
	  return tcpRosServer.getSubscribers();
  }
  /**
   * Start the RPC server. This start() routine requires that the
   * {@link TcpRosServer} is initialized first so that the slave server returns
   * correct information when topics are requested.
   */
  public void start() {
    super.start();
    tcpRosServer.start();
  }

  // TODO(damonkohler): This should also shut down the Node.
  @Override
  public void shutdown() throws IOException {
    super.shutdown();
    tcpRosServer.shutdown();
  }

  public List<Object> getBusStats(String callerId) {
    throw new UnsupportedOperationException();
  }
  /**
   * We are returning a list of ArrayList of strings. Each ArrayList of string will
   * contain 5 elements representing subscriber or publisher bus information.<br/>
   * The first element of each list is a monotonically increasing integer.<br/>
   * The second element is the node identifier of the opposite party, sub for pub and pub for sub.<br/>
   * The third element is o for subscriber, i for publisher<br/>
   * The fourth element is the protocol<br/>
   * The fifth element is the topic name.<br/>
   * @param callerId
   * @return A List of Objects representing the above, packaged thusly to enable remote serialization delivery.
   */
  public List<Object> getBusInfo(String callerId) {
    List<Object> busInfo = new ArrayList<Object>();
    // The connection ID field is opaque to the user. A monotonically increasing
    // integer for now is sufficient.
    int id = 0;
    for (DefaultPublisher<?> publisher : getPublications()) {
      for (SubscriberIdentifier subscriberIdentifier : topicParticipantManager
          .getPublisherConnections(publisher)) {
        List<String> publisherBusInfo = new ArrayList<String>();
        publisherBusInfo.add(Integer.toString(id));
        publisherBusInfo.add(subscriberIdentifier.getNodeIdentifier().getName().toString());
        // TODO(damonkohler): Pull out BusInfo constants.
        publisherBusInfo.add("o");
        // TODO(damonkohler): Add getter for protocol to topic participants.
        publisherBusInfo.add(ProtocolNames.TCPROS);
        publisherBusInfo.add(publisher.getTopicName().toString());
        busInfo.add(publisherBusInfo);
        id++;
      }
    }
    for (DefaultSubscriber<?> subscriber : getSubscriptions()) {
      for (PublisherIdentifier publisherIdentifer : topicParticipantManager.getSubscriberConnections(subscriber)) {
        List<String> subscriberBusInfo = new ArrayList<String>();
        subscriberBusInfo.add(Integer.toString(id));
        // Subscriber connection PublisherIdentifiers are populated with node
        // URIs instead of names. As a result, the only identifier information
        // available is the URI.
        subscriberBusInfo.add(publisherIdentifer.getNodeIdentifier().getUri().toString());
        // TODO(damonkohler): Pull out BusInfo constants.
        subscriberBusInfo.add("i");
        // TODO(damonkohler): Add getter for protocol to topic participants.
        subscriberBusInfo.add(ProtocolNames.TCPROS);
        subscriberBusInfo.add(publisherIdentifer.getTopicName().toString());
        busInfo.add(subscriberBusInfo);
        id++;
      }
    }
    return busInfo;
  }

  public InetSocketAddress getMasterUri() {
    return masterClient.getRemoteUri();
  }

  /**
   * @return PID of this process if available, throws
   *         {@link UnsupportedOperationException} otherwise.
   */
  @Override
  public int getPid() {
    return Process.getPid();
  }

  public Collection<DefaultSubscriber<?>> getSubscriptions() {
    return topicParticipantManager.getSubscribers();
  }

  public Collection<DefaultPublisher<?>> getPublications() {
    return topicParticipantManager.getPublishers();
  }

  /**
   * @param parameterName
   * @param parameterValue
   * @return the number of parameter subscribers that received the update
   */
  public int paramUpdate(GraphName parameterName, Object parameterValue) {
    return parameterManager.updateParameter(parameterName, parameterValue);
  }
  /**
   * If there is a subscriber subscribed to this topicName, call updatePublishers on the subscriber
   * using the collection of publisherUris. DefaultSubscriber class creates an 
   * UpdatePublisherRunnable thread which creates a SlaveClient of type SlaveRpcEndpointImpl to
   * contact the publisher.
   * @param callerId
   * @param topicName
   * @param publisherUris collection of InetSocketAddress of remote publishers to be updated. 
   */
  public void publisherUpdate(String callerId, String topicName, Collection<InetSocketAddress> publisherUris) {
    GraphName graphName = GraphName.of(topicName);
    if (topicParticipantManager.hasSubscriber(graphName)) {
      DefaultSubscriber<?> subscriber = topicParticipantManager.getSubscriber(graphName);
      TopicDeclaration topicDeclaration = subscriber.getTopicDeclaration();
      Collection<PublisherIdentifier> identifiers =
          PublisherIdentifier.newCollectionFromUris(publisherUris, topicDeclaration);
      subscriber.updatePublishers(identifiers);
      if(DEBUG) {
    	  log.info("Updating subscriber:"+subscriber);
    	  for(InetSocketAddress i: publisherUris)
    		  log.info("PUBLISHER:"+i+" for:"+this);
      }
    }
  }
  /**
   * Request a topic conforming to the specified set of protocols, for instance
   * if a ProtocolNames.TCPROS is included, a new TcpRosProtocolDescription from tcpRosServer.getAdvertiseAddress
   * using tcpRosServer class field will be returned
   * @param topicName
   * @param protocols
   * @return
   * @throws ServerException
   */
  public ProtocolDescription requestTopic(String topicName, Collection<String> protocols) throws ServerException {
    // TODO(damonkohler): Use NameResolver.
    // Canonicalize topic name.
    GraphName graphName = GraphName.of(topicName).toGlobal();
    if( DEBUG )
    	log.info("Requesting topic:"+topicName+" for GraphName:"+graphName);
    if (!topicParticipantManager.hasPublisher(graphName)) {
      //throw new ServerException("No publishers for topic: " + graphName);
    	return null;
    }
    for (String protocol : protocols) {
      if (protocol.equals(ProtocolNames.TCPROS)) {
        try {
        	if( DEBUG )
        	  log.info("Requested topic:"+topicName+" for GraphName:"+graphName+" returning:"+tcpRosServer.getAdvertiseAddress());
          return new TcpRosProtocolDescription(tcpRosServer.getAdvertiseAddress());
        } catch (Exception e) {
          throw new ServerException(e);
        }
      }
    }
    throw new ServerException("No supported protocols specified.");
  }

  /**
   * @return a {@link NodeIdentifier} for this {@link SlaveServer}
   */
  public NodeIdentifier toNodeIdentifier() {
    return new NodeIdentifier(nodeName, getUri());
  }

  @Override
  public Object invokeMethod(RemoteRequestInterface rri) throws Exception {
	  synchronized(invokableMethods) {
		  return invokableMethods.invokeMethod(rri, this);
	  }
  }
}
