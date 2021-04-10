package org.ros;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.concurrent.DefaultScheduledExecutorService;
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
import org.ros.internal.node.server.ParameterServer;
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
    	//Map<String,String> remaps = loader.getSpecialRemappings();
    	// determine if directive to write loaded external JARS exists __jarclasses:=/directory/to/jarfiles
    	//if( remaps.containsKey("__jarclasses") ) {
    		//String jarClasses = remaps.get("__jarclasses");
        // see if we are going to load JARs for provisioning remote nodes.
    	String jarsDir = System.getProperty(RosCore.propsEntry);
    	if(jarsDir != null) {
    		if(!jarsDir.endsWith("/"))
    			jarsDir += "/";
    		loader.createJarClassLoader();
    		ParameterTree parameterTree = null;
    		try {
    			parameterTree = nodeConfiguration.getParameterTree();			
    		} catch (IOException e) {
    			log.error("Cannot construct ParameterTree for JAR provisioning due to "+e,e);
    		}
       		Map<String,Object> emptyMap = new HashMap<String,Object>();
       		String jarFileName;
       		if(parameterTree != null) {
       			Map<String,Object> jars = (Map<String, Object>) parameterTree.get(GraphName.of(RosCore.jarParent), emptyMap );
       			Iterator<Entry<String, Object>> it = jars.entrySet().iterator();
       			while(it.hasNext()) {
       				Entry<String,Object> jarName = it.next();
       				jarFileName = jarsDir+jarName.getKey().replace("_", ".");
       				log.info(jarFileName+" retrieved for provisioning..");
       				FileOutputStream fos = new FileOutputStream(jarFileName);
       				fos.write((byte[]) jarName.getValue());
       				fos.flush();
       				fos.close();
       				log.info("Wrote "+jarName.getKey()+" "+((byte[])jarName.getValue()).length+" bytes.");
       				loader.getJarClassLoader().loadJarFromJarfile("file://"+jarFileName);
       			}
       			log.info("Proceeding to load "+nodeClassName+" using ParameterTree JAR provisioning.");
       			nodeMain = loader.loadClassWithJars(nodeClassName);
       		} else {
       			log.info("Acquisition of JARs from ParameterTree was unsuccessful, using standard classloader");
       			nodeMain = loader.loadClass(nodeClassName);
       		}
    	} else {
    		nodeMain = loader.loadClass(nodeClassName);
    	}
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
      throw new RosRuntimeException("Unable to instantiate node: " + nodeClassName, e);
    }

    assert(nodeMain != null);
    NodeMainExecutor nodeMainExecutor = DefaultNodeMainExecutor.newDefault();
    nodeMainExecutor.execute(nodeMain, nodeConfiguration);
  }
}
