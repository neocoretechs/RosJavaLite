package org.ros.node;

import org.ros.internal.loader.CommandLineLoader;
import org.ros.internal.message.definition.MessageDefinitionReflectionProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.address.AdvertiseAddress;
import org.ros.address.AdvertiseAddressFactory;
import org.ros.address.BindAddress;
import org.ros.address.PrivateAdvertiseAddressFactory;
import org.ros.address.PublicAdvertiseAddressFactory;
import org.ros.concurrent.DefaultScheduledExecutorService;
import org.ros.exception.RosRuntimeException;
import org.ros.internal.message.DefaultMessageFactory;
import org.ros.internal.message.service.ServiceDescriptionFactory;
import org.ros.internal.message.service.ServiceRequestMessageFactory;
import org.ros.internal.message.service.ServiceResponseMessageFactory;
import org.ros.internal.message.topic.TopicDescriptionFactory;
import org.ros.internal.node.DefaultNode;
import org.ros.internal.node.client.MasterClient;
import org.ros.internal.node.parameter.DefaultParameterTree;
import org.ros.internal.node.parameter.ParameterManager;
import org.ros.internal.node.server.NodeIdentifier;
import org.ros.internal.node.server.SlaveServer;
import org.ros.internal.node.service.ServiceManager;
import org.ros.internal.node.topic.TopicParticipantManager;
import org.ros.message.MessageDefinitionProvider;
import org.ros.message.MessageFactory;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.namespace.NodeNameResolver;
import org.ros.node.parameter.ParameterTree;
import org.ros.time.TimeProvider;
import org.ros.time.WallTimeProvider;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Stores configuration information (e.g. ROS master URI) for {@link Node}s.
 * 
 * @see <a href="http://www.ros.org/wiki/ROS/Technical%20Overview#Node">Node documentation</a>
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2021
 */
public class NodeConfiguration {
  private static final boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(NodeConfiguration.class);
  /**
   * The default master {@link URI}.
   */
  public static final InetSocketAddress DEFAULT_MASTER_URI;

  static {
    DEFAULT_MASTER_URI = new InetSocketAddress("127.0.0.1", 8090);
  }

  private NameResolver parentResolver;
  private InetSocketAddress masterUri;
  private File rosRoot;
  private List<File> rosPackagePath;
  private GraphName nodeName;
  private TopicDescriptionFactory topicDescriptionFactory;
  private MessageFactory topicMessageFactory;
  private ServiceDescriptionFactory serviceDescriptionFactory;
  private MessageFactory serviceRequestMessageFactory;
  private MessageFactory serviceResponseMessageFactory;
 
  private BindAddress tcpRosBindAddress;
  private AdvertiseAddress tcpRosAdvertiseAddress = null;
  private AdvertiseAddressFactory tcpRosAdvertiseAddressFactory;
  private BindAddress rpcBindAddress;
  private AdvertiseAddress rpcAdvertiseAddress = null;
  private AdvertiseAddressFactory rpcAdvertiseAddressFactory;
  private ScheduledExecutorService scheduledExecutorService;
  private TimeProvider timeProvider;
  private CommandLineLoader commandLineLoader = null;

  // These follow creation of SlaveServer and access to ParameterTree
  private SlaveServer slaveServer = null;
  private MasterClient masterClient = null;
  private ParameterTree parameterTree = null;
  private TopicParticipantManager topicParticipantManager;
  private ServiceManager serviceManager;
  private ScheduledExecutorService executor;
  private ParameterManager parameterManager;
  private NameResolver resolver;
  private NodeIdentifier nodeIdentifier;
  private ClassLoader classLoader;
  /**
   * @param nodeConfiguration The {@link NodeConfiguration} to copy
   * @return a copy of the supplied {@link NodeConfiguration}
   */
  public static NodeConfiguration copyOf(NodeConfiguration nodeConfiguration) {
    NodeConfiguration copy = new NodeConfiguration(nodeConfiguration.classLoader);
    copy.parentResolver = nodeConfiguration.parentResolver;
    copy.masterUri = nodeConfiguration.masterUri;
    copy.rosRoot = nodeConfiguration.rosRoot;
    copy.rosPackagePath = nodeConfiguration.rosPackagePath;
    copy.nodeName = nodeConfiguration.nodeName;
    copy.topicDescriptionFactory = nodeConfiguration.topicDescriptionFactory;
    copy.topicMessageFactory = nodeConfiguration.topicMessageFactory;
    copy.serviceDescriptionFactory = nodeConfiguration.serviceDescriptionFactory;
    copy.serviceRequestMessageFactory = nodeConfiguration.serviceRequestMessageFactory;
    copy.serviceResponseMessageFactory = nodeConfiguration.serviceResponseMessageFactory;
    copy.tcpRosBindAddress = nodeConfiguration.tcpRosBindAddress;
    copy.tcpRosAdvertiseAddressFactory = nodeConfiguration.tcpRosAdvertiseAddressFactory;
    copy.rpcBindAddress = nodeConfiguration.rpcBindAddress;
    copy.rpcAdvertiseAddressFactory = nodeConfiguration.rpcAdvertiseAddressFactory;
    copy.scheduledExecutorService = nodeConfiguration.scheduledExecutorService;
    copy.timeProvider = nodeConfiguration.timeProvider;
    copy.commandLineLoader = nodeConfiguration.commandLineLoader;
    // SlaveServer and related
    copy.slaveServer = nodeConfiguration.slaveServer;
    copy.masterClient = nodeConfiguration.masterClient;
    copy.parameterTree = nodeConfiguration.parameterTree;
    copy.topicParticipantManager = nodeConfiguration.topicParticipantManager;
    copy.serviceManager = nodeConfiguration.serviceManager;
    copy.executor = nodeConfiguration.executor;
    copy.parameterManager = nodeConfiguration.parameterManager;
    copy.resolver = nodeConfiguration.resolver;
    copy.nodeIdentifier = nodeConfiguration.nodeIdentifier;
    return copy;
  }

  /**
   * Creates a new {@link NodeConfiguration} for a publicly accessible {@link Node}.
   * @param nodeName the name of this node once fully constructed
   * @param host the host that the {@link Node} will run on
   * @param defaultMasterUri the {@link URI} for the master that the {@link Node} will register with
   * @return a new {@link NodeConfiguration} for a publicly accessible {@link Node}
   */
  public static NodeConfiguration newPublic(String nodeName, String host, InetSocketAddress defaultMasterUri, ClassLoader classLoader) {
    NodeConfiguration configuration = new NodeConfiguration(classLoader);
    configuration.setNodeName(nodeName);
    configuration.setRpcBindAddress(BindAddress.newPublic());
    configuration.setRpcAdvertiseAddressFactory(new PublicAdvertiseAddressFactory(host));
    configuration.setTcpRosBindAddress(BindAddress.newPublic());
    configuration.setTcpRosAdvertiseAddressFactory(new PublicAdvertiseAddressFactory(host));
    configuration.setMasterUri(defaultMasterUri);
    try {
    	configuration.getSlaveServer();
 	} catch (IOException e) {
 		throw new RuntimeException("Can not start SlaveServer for new public node configuration due to "+e);
 	}
    return configuration;
  }


  /**
   * Creates a new {@link NodeConfiguration} for a {@link Node} that is only
   * accessible on the local host.
   * 
   * @param masterUri the {@link URI} for the master that the {@link Node} will register with
   * @return a new {@link NodeConfiguration} for a private {@link Node}
   */
  public static NodeConfiguration newPrivate(InetSocketAddress masterUri, String nodeName, ClassLoader classLoader) {
    NodeConfiguration configuration = new NodeConfiguration(classLoader);
    configuration.setRpcBindAddress(BindAddress.newPrivate());
    configuration.setRpcAdvertiseAddressFactory(new PrivateAdvertiseAddressFactory());
    configuration.setTcpRosBindAddress(BindAddress.newPrivate());
    configuration.setTcpRosAdvertiseAddressFactory(new PrivateAdvertiseAddressFactory());
    configuration.setMasterUri(masterUri);
    try {
		configuration.getSlaveServer();
	} catch (IOException e) {
		throw new RuntimeException("Can not start SlaveServer for new private node configuration due to "+e);
	}
    return configuration;
  }


  /**
   * Creates a new {@link NodeConfiguration} for a {@link Node} that is only accessible on the local host.
   * @param nodeName the name of this node, once fully constructed
   * @param inetSocketAddress 
   * @return a new {@link NodeConfiguration} for a private {@link Node}
   */
  public static NodeConfiguration newPrivate(String nodeName, InetSocketAddress inetSocketAddress, ClassLoader classLoader) {
    return newPrivate(DEFAULT_MASTER_URI, nodeName, classLoader);
  }

  private NodeConfiguration(ClassLoader classLoader) {
	this.classLoader = classLoader;
    MessageDefinitionProvider messageDefinitionProvider = new MessageDefinitionReflectionProvider(classLoader);
    setTopicDescriptionFactory(new TopicDescriptionFactory(messageDefinitionProvider));
    setTopicMessageFactory(new DefaultMessageFactory(messageDefinitionProvider));
    setServiceDescriptionFactory(new ServiceDescriptionFactory(messageDefinitionProvider));
    setServiceRequestMessageFactory(new ServiceRequestMessageFactory(messageDefinitionProvider));
    setServiceResponseMessageFactory(new ServiceResponseMessageFactory(messageDefinitionProvider));
    setParentResolver(NameResolver.newRoot());
    setTimeProvider(new WallTimeProvider());
  }

  /**
   * @return the {@link NameResolver} for the {@link Node}'s parent namespace
   */
  public NameResolver getParentResolver() {
    return parentResolver;
  }

  /**
   * @param resolver the {@link NameResolver} for the {@link Node}'s parent namespace
   * @return this {@link NodeConfiguration}
   */
  public NodeConfiguration setParentResolver(NameResolver resolver) {
    this.parentResolver = resolver;
    return this;
  }

  /**
   * @see <ahref="http://www.ros.org/wiki/ROS/EnvironmentVariables#ROS_MASTER_URI">ROS_MASTER_URI documentation</a>
   * @return the {@link URI} of the master that the {@link Node} will register with
   */
  public InetSocketAddress getMasterUri() {
    return masterUri;
  }
  
  public SlaveServer newSlaveServer() throws IOException {
 	    topicParticipantManager = new TopicParticipantManager();
	    serviceManager = new ServiceManager();
		executor = new DefaultScheduledExecutorService();
		parameterManager = new ParameterManager(executor);
		//GraphName basename = getDefaultNodeName();
		nodeName = parentResolver.getNamespace();//.join(basename);
		resolver = new NodeNameResolver(nodeName, parentResolver);
		masterClient = new MasterClient(masterUri, 60000, 60000);
		slaveServer =
			   new SlaveServer(nodeName, getTcpRosBindAddress(), getTcpRosAdvertiseAddress(),getRpcBindAddress(),getRpcAdvertiseAddress(),
					   masterClient, topicParticipantManager, serviceManager, parameterManager, executor);
	    // start TcpRosServer and SlaveServer
	    slaveServer.start();
	    nodeIdentifier = slaveServer.toNodeIdentifier();
		parameterTree =
			    DefaultParameterTree.newFromNodeIdentifier(nodeIdentifier, masterClient.getRemoteUri(),
			        resolver, parameterManager);
		return slaveServer;
  }
  
  /**
   * 
   * @return CommandLineLoader used to initiate node, if used
   */
  public CommandLineLoader getCommandLineLoader() {
		return commandLineLoader;
  }

  public void setCommandLineLoader(CommandLineLoader commandLineLoader) {
		this.commandLineLoader = commandLineLoader;
  }
  /**
   * @see <a href="http://www.ros.org/wiki/ROS/EnvironmentVariables#ROS_MASTER_URI">ROS_MASTER_URI documentation</a>
   * @param defaultMasterUri the {@link URI} of the master that the {@link Node} will register with
   * @return this {@link NodeConfiguration}
   */
  public NodeConfiguration setMasterUri(InetSocketAddress defaultMasterUri) {
    this.masterUri = defaultMasterUri;
    return this;
  }

  /**
   * Get the previously created slave server, or start one if its not yet;
   * @return The SlaveServer
   */
  public SlaveServer getSlaveServer() throws IOException {
	if(slaveServer == null)
		return newSlaveServer();
	return slaveServer;
  }

/**
   * @see <a href="http://www.ros.org/wiki/ROS/EnvironmentVariables#ROS_ROOT">ROS_ROOT documentation</a>
   * @return the location where the ROS core packages are installed
   */
  public File getRosRoot() {
    return rosRoot;
  }

  /**
   * @see <a href="http://www.ros.org/wiki/ROS/EnvironmentVariables#ROS_ROOT">ROS_ROOT documentation</a>
   * @param rosRoot the location where the ROS core packages are installed
   * @return this {@link NodeConfiguration}
   */
  public NodeConfiguration setRosRoot(File rosRoot) {
    this.rosRoot = rosRoot;
    return this;
  }

  /**
   * These ordered paths tell the ROS system where to search for more ROS
   * packages. If there are multiple packages of the same name, ROS will choose
   * the one that appears in the {@link List} first.
   * 
   * @see <a href="http://www.ros.org/wiki/ROS/EnvironmentVariables#ROS_PACKAGE_PATH">ROS_PACKAGE_PATH documentation</a>
   * @return the {@link List} of paths where the system will look for ROS packages
   */
  public List<File> getRosPackagePath() {
    return rosPackagePath;
  }

  /**
   * These ordered paths tell the ROS system where to search for more ROS
   * packages. If there are multiple packages of the same name, ROS will choose
   * the one that appears in the {@link List} first.
   * 
   * @see <a href="http://www.ros.org/wiki/ROS/EnvironmentVariables#ROS_PACKAGE_PATH">ROS_PACKAGE_PATH documentation</a>
   * @param rosPackagePath the {@link List} of paths where the system will look for ROS  packages
   * @return this {@link NodeConfiguration}
   */
  public NodeConfiguration setRosPackagePath(List<File> rosPackagePath) {
    this.rosPackagePath = rosPackagePath;
    return this;
  }

  /**
   * @return the name of the {@link Node}
   */
  public GraphName getNodeName() {
    return nodeName;
  }

  /**
   * @param nodeName  the name of the {@link Node}
   * @return this {@link NodeConfiguration}
   */
  public NodeConfiguration setNodeName(GraphName nodeName) {
    this.nodeName = nodeName;
    if(slaveServer != null)
    	slaveServer.setNodeName(nodeName);
	log.info("setNodeName(GraphName) Setting Node Name:"+nodeName); 
    return this;
  }

  /**
   * @param nodeName the name of the {@link Node}
   * @return this {@link NodeConfiguration}
   */
  public NodeConfiguration setNodeName(String nodeName) {
	if(slaveServer != null)
	   slaveServer.setNodeName(GraphName.of(nodeName));
	log.info("setNodeName(String) Setting Node Name:"+nodeName); 
    return setNodeName(GraphName.of(nodeName));
  }

  /**
   * Sets the name of the {@link Node} if the name has not already been set.
   * 
   * @param nodeName  the name of the {@link Node}
   * @return this {@link NodeConfiguration}
   */
  public NodeConfiguration setDefaultNodeName(GraphName nodeName) {
    if (this.nodeName == null) {
      setNodeName(nodeName);
    }
    return this;
  }

  /**
   * Sets the name of the {@link Node} if the name has not already been set.
   * 
   * @param nodeName the name of the {@link Node}
   * @return this {@link NodeConfiguration}
   */
  public NodeConfiguration setDefaultNodeName(String nodeName) {
    return setDefaultNodeName(GraphName.of(nodeName));
  }


  /**
   * @param topicMessageFactory  the {@link MessageFactory} for the {@link Node}
   * @return this {@link NodeConfiguration}
   */
  public NodeConfiguration setTopicMessageFactory(MessageFactory topicMessageFactory) {
    this.topicMessageFactory = topicMessageFactory;
    return this;
  }

  public MessageFactory getTopicMessageFactory() {
    return topicMessageFactory;
  }

  /**
   * @param serviceRequestMessageFactory the {@link ServiceRequestMessageFactory} for the {@link Node}
   * @return this {@link NodeConfiguration}
   */
  public NodeConfiguration setServiceRequestMessageFactory(
      ServiceRequestMessageFactory serviceRequestMessageFactory) {
    this.serviceRequestMessageFactory = serviceRequestMessageFactory;
    return this;
  }

  public MessageFactory getServiceRequestMessageFactory() {
    return serviceRequestMessageFactory;
  }

  /**
   * @param serviceResponseMessageFactory  the {@link ServiceResponseMessageFactory} for the {@link Node}
   * @return this {@link NodeConfiguration}
   */
  public NodeConfiguration setServiceResponseMessageFactory(
      ServiceResponseMessageFactory serviceResponseMessageFactory) {
    this.serviceResponseMessageFactory = serviceResponseMessageFactory;
    return this;
  }

  public MessageFactory getServiceResponseMessageFactory() {
    return serviceResponseMessageFactory;
  }

  /**
   * @param topicDescriptionFactory the {@link TopicDescriptionFactory} for the {@link Node}
   * @return this {@link NodeConfiguration}
   */
  public NodeConfiguration setTopicDescriptionFactory(
      TopicDescriptionFactory topicDescriptionFactory) {
    this.topicDescriptionFactory = topicDescriptionFactory;
    return this;
  }

  public TopicDescriptionFactory getTopicDescriptionFactory() {
    return topicDescriptionFactory;
  }

  /**
   * @param serviceDescriptionFactory the {@link ServiceDescriptionFactory} for the {@link Node}
   * @return this {@link NodeConfiguration}
   */
  public NodeConfiguration setServiceDescriptionFactory(
      ServiceDescriptionFactory serviceDescriptionFactory) {
    this.serviceDescriptionFactory = serviceDescriptionFactory;
    return this;
  }

  public ServiceDescriptionFactory getServiceDescriptionFactory() {
    return serviceDescriptionFactory;
  }

  /**
   * @see <a href="http://www.ros.org/wiki/ROS/TCPROS">TCPROS documentation</a>
   * 
   * @return the {@link BindAddress} for the {@link Node}'s TCPROS server
   */
  public BindAddress getTcpRosBindAddress() {
    return tcpRosBindAddress;
  }

  /**
   * @see <a href="http://www.ros.org/wiki/ROS/TCPROS">TCPROS documentation</a>
   * 
   * @param tcpRosBindAddress the {@link BindAddress} for the {@link Node}'s TCPROS server
   */
  public NodeConfiguration setTcpRosBindAddress(BindAddress tcpRosBindAddress) {
    this.tcpRosBindAddress = tcpRosBindAddress;
    return this;
  }

  /**
   * @see <a href="http://www.ros.org/wiki/ROS/TCPROS">TCPROS documentation</a>
   * 
   * @return the {@link AdvertiseAddressFactory} for the {@link Node}'s TCPROS server
   */
  public AdvertiseAddressFactory getTcpRosAdvertiseAddressFactory() {
    return tcpRosAdvertiseAddressFactory;
  }

  /**
   * @see <a href="http://www.ros.org/wiki/ROS/TCPROS">TCPROS documentation</a>
   * 
   * @param tcpRosAdvertiseAddressFactory the {@link AdvertiseAddressFactory} for the {@link Node}'s TCPROS server
   * @return this {@link NodeConfiguration}
   */
  public NodeConfiguration setTcpRosAdvertiseAddressFactory(AdvertiseAddressFactory tcpRosAdvertiseAddressFactory) {
    this.tcpRosAdvertiseAddressFactory = tcpRosAdvertiseAddressFactory;
    return this;
  }

  /**
   * @see <a href="http://www.ros.org/wiki/ROS/TCPROS">TCPROS documentation</a> 
   * @return the {@link AdvertiseAddress} for the {@link Node}'s TCPROS server
   */
  public AdvertiseAddress getTcpRosAdvertiseAddress() {
	  if( tcpRosAdvertiseAddress == null)
		  tcpRosAdvertiseAddress = tcpRosAdvertiseAddressFactory.newDefault();
	  return tcpRosAdvertiseAddress;
  }

  /**
   * @see <a href="http://www.ros.org/wiki/ROS/Technical%20Overview#Node">Node documentation</a>
   * 
   * @return the {@link BindAddress} for the {@link Node}'s XML-RPC server
   */
  public BindAddress getRpcBindAddress() {
    return rpcBindAddress;
  }

  /**
   * @see <a href="http://www.ros.org/wiki/ROS/Technical%20Overview#Node">Node documentation</a>
   * 
   * @param RpcBindAddress  the {@link BindAddress} for the {@link Node}'s RPC server
   */
  public NodeConfiguration setRpcBindAddress(BindAddress rpcBindAddress) {
    this.rpcBindAddress = rpcBindAddress;
    return this;
  }

  /**
   * @see <a href="http://www.ros.org/wiki/ROS/Technical%20Overview#Node">Node documentation</a>
   * 
   * @return the {@link AdvertiseAddress} for the {@link Node}'s RPC server
   */
  public AdvertiseAddress getRpcAdvertiseAddress() {
	  if( rpcAdvertiseAddress == null)
		  rpcAdvertiseAddress = rpcAdvertiseAddressFactory.newDefault();
	  return rpcAdvertiseAddress;
  }

  /**
   * @see <a href="http://www.ros.org/wiki/ROS/Technical%20Overview#Node">Node  documentation</a>
   * 
   * @return the {@link AdvertiseAddressFactory} for the {@link Node}'s RPC
   *         server
   */
  public AdvertiseAddressFactory getRpcAdvertiseAddressFactory() {
    return rpcAdvertiseAddressFactory;
  }

  /**
   * @see <a href="http://www.ros.org/wiki/ROS/Technical%20Overview#Node">Node  documentation</a>
   * 
   * @param rpcAdvertiseAddressFactory the {@link AdvertiseAddressFactory} for the {@link Node}'s XML-RPC server
   */
  public NodeConfiguration setRpcAdvertiseAddressFactory(
      AdvertiseAddressFactory rpcAdvertiseAddressFactory) {
    this.rpcAdvertiseAddressFactory = rpcAdvertiseAddressFactory;
    return this;
  }

  /**
   * @return the configured {@link TimeProvider}
   */
  public TimeProvider getTimeProvider() {
    return timeProvider;
  }

  /**
   * Sets the {@link TimeProvider} that {@link Node}s will use. By default, the
   * {@link WallTimeProvider} is used.
   * 
   * @param timeProvider the {@link TimeProvider} that {@link Node}s will use
   */
  public NodeConfiguration setTimeProvider(TimeProvider timeProvider) {
    this.timeProvider = timeProvider;
    return this;
  }

  /**
   * @return the parameterTree
   * @throws IOException If slave server to parameter tree has not been started and it failed to start.
   */
  public ParameterTree getParameterTree() throws IOException {
	  if(slaveServer == null)
		  newSlaveServer();
	return parameterTree;
  }

  public MasterClient getMasterClient() {
	return masterClient;
  }

  public TopicParticipantManager getTopicParticipantManager() {
	return topicParticipantManager;
  }

  public ServiceManager getServiceManager() {
	return serviceManager;
  }

  public ParameterManager getParameterManager() {
	return parameterManager;
  }
}
