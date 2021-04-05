package org.ros;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.exception.RosRuntimeException;
import org.ros.internal.loader.CommandLineLoader;
import org.ros.internal.transport.tcp.TcpRosServer;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.namespace.NodeNameResolver;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.NodeMainExecutor;
import org.ros.node.parameter.ParameterTree;
import org.ros.internal.node.DefaultNode;
import org.ros.internal.node.client.MasterClient;
import org.ros.internal.node.parameter.DefaultParameterTree;
import org.ros.internal.node.parameter.ParameterManager;
import org.ros.internal.node.server.NodeIdentifier;
import org.ros.internal.node.server.SlaveServer;
import org.ros.internal.node.service.ServiceManager;
import org.ros.internal.node.topic.TopicParticipantManager;

/**
 * This is a main class entry point for executing {@link NodeMain}s.
 * 
 * @author Jonathan Groff (C) NeoCoreTechs 2021
 */
public class RosRun {
  private static final Log log = LogFactory.getLog(RosRun.class);
  public static void printUsage() {
    System.err.println("Usage: java -jar my_package.jar com.example.MyNodeMain __jarclasses:=/dir/with/jars [args]");
  }

  public static void main(String[] argv) throws Exception {
    if (argv.length == 0) {
      printUsage();
      System.exit(1);
    }
    ArrayList<String> l1 = new ArrayList<String>();
    for(int i = 0; i < argv.length; i++) l1.add(argv[i]);
    CommandLineLoader loader = new CommandLineLoader(l1);
    String nodeClassName = loader.getNodeClassName();
    log.info("Loading node class: " + loader.getNodeClassName());
    NodeConfiguration nodeConfiguration = loader.build();
    nodeConfiguration.setCommandLineLoader(loader);

    NodeMain nodeMain = null;
    try {
    	Map<String,String> remaps = loader.getSpecialRemappings();
    	// determine if directive to load external JARS exists __jarclasses:=/directory/to/jarfiles
    	if( remaps.containsKey("__jarclasses") ) {
    		//String jarClasses = remaps.get("__jarclasses");
    		loader.createJarClassLoader();
    	    TopicParticipantManager topicParticipantManager = new TopicParticipantManager();
    	    ServiceManager serviceManager = new ServiceManager();
    		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    		ParameterManager parameterManager = new ParameterManager(executor);
    		GraphName basename = nodeConfiguration.getNodeName();
    		NameResolver parentResolver = nodeConfiguration.getParentResolver();
    		GraphName nodeName = parentResolver.getNamespace().join(basename);
    		NameResolver resolver = new NodeNameResolver(nodeName, parentResolver);
    		ParameterTree parameterTree = null;
    		SlaveServer slaveServer;
    	    InetSocketAddress masterUri = nodeConfiguration.getMasterUri();
    	    MasterClient masterClient;
    	    try {
    			masterClient = new MasterClient(masterUri, 60000, 60000);
    		} catch (IOException e1) {
    			log.error("Unknown host for master client:"+masterUri+" attempting to access ParameterTree for JAR provisioning",e1);
    			//e1.printStackTrace();
    			throw new RosRuntimeException(e1);
    		}
    	    try {
    			slaveServer =
    			    new SlaveServer(nodeName, nodeConfiguration.getTcpRosBindAddress(),
    			        nodeConfiguration.getTcpRosAdvertiseAddress(),
    			        nodeConfiguration.getRpcBindAddress(),
    			        nodeConfiguration.getRpcAdvertiseAddress(), masterClient, topicParticipantManager,
    			        serviceManager, parameterManager, executor);
    		} catch (IOException e) {
    			log.error("Cannot configure slave server to access ParmeterTree JAR provisioning due to "+e,e);
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
    			log.error("Cannot construct ParameterTree for JAR provisioning due to "+e,e);
    		}
       		Map<String,Object> emptyMap = new HashMap<String,Object>();
       		if(parameterTree != null) {
       			Map<String,Object> jars = (Map<String, Object>) parameterTree.get(GraphName.of("jars/*"), emptyMap );
       			//loader.getJarClassLoader().loadFromTree(..)
       			nodeMain = loader.loadClassWithJars(nodeClassName);
       		} else {
       			nodeMain = loader.loadClass(nodeClassName);
       		}  		
    	} else {
    		nodeMain = loader.loadClass(nodeClassName);
    	}
    } catch (ClassNotFoundException e) {
      throw new RosRuntimeException("Unable to locate node: " + nodeClassName, e);
    } catch (InstantiationException e) {
      throw new RosRuntimeException("Unable to instantiate node: " + nodeClassName, e);
    } catch (IllegalAccessException e) {
      throw new RosRuntimeException("Unable to instantiate node: " + nodeClassName, e);
    }

    assert(nodeMain != null);
    NodeMainExecutor nodeMainExecutor = DefaultNodeMainExecutor.newDefault();
    nodeMainExecutor.execute(nodeMain, nodeConfiguration);
  }
}
