package org.ros.internal.loader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.CommandLineVariables;
import org.ros.EnvironmentVariables;

import org.ros.address.InetSocketAddressFactory;
import org.ros.internal.jarclassloader.JarClassLoader;

import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.AbstractNodeMain;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Create {@link NodeConfiguration} instances using a ROS command-line and
 * environment specification. When starting a node through RosRun, this class is used
 * to process the command line remappings.
 * 
 * Remappings ":= on cmdl", get put in 'remappingArguments'. Those that are prefixed with "__" are put
 * into the speacialRemappings collection, those without into 'remappings' after taking Graph.nameOf of the remapped args.
 * Args on the cmdl without a remapping ":=" get put into nodeArguments.
 * 
 * NOTE: If no constructor is detected during loadClass invocation, 
 * A node with a static getInstance() returning a type of NodeMain will be invoked. If neither
 * an InstantiationException is thrown. This is an enhancement to node creation in original RosJava.
 * @author Jonathan Groff (C) NeoCoreTechs 2021
 */
public class CommandLineLoader {
  private static final boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(CommandLineLoader.class);
  private final List<String> argv;
  private final List<String> nodeArguments;
  private final List<String> remappingArguments;
  private final Map<String, String> environment;
  private final Map<String, String> specialRemappings;
  private final Map<GraphName, GraphName> remappings;

  private String nodeClassName;
  
  private JarClassLoader jcl = null; // to pull JARs from ParameterTree

  /**
   * Create new {@link CommandLineLoader} with specified command-line arguments.
   * Environment variables will be pulled from default {@link System}
   * environment variables.
   * 
   * @param argv command-line arguments
   */
  public CommandLineLoader(List<String> argv) {
    this(argv, System.getenv());
  }

  /**
   * Create new {@link CommandLineLoader} with specified command-line arguments
   * and environment variables.
   * 
   * @param argv command-line arguments
   * @param environment environment variables
   */
  public CommandLineLoader(List<String> argv, Map<String, String> environment) {
	if(DEBUG)
		log.info("***invoking command line loader");
    assert(argv.size() > 0);
    this.argv = argv;
    this.environment = environment;
    nodeArguments = new ArrayList<String>();
    remappingArguments = new ArrayList<String>();
    remappings = new HashMap<GraphName, GraphName>();
    specialRemappings = new HashMap<String, String>();
    parseArgv();
  }

  private void parseArgv() {
    for (String argument : argv) {
    	if(DEBUG)
    		log.info("argument="+argument);
      if (argument.contains(":=")) {
        remappingArguments.add(argument);
      } else {
        nodeArguments.add(argument);
      }
    }
    if(nodeArguments.size() > 0)
    	nodeClassName = nodeArguments.get(0);
  }

  public String getNodeClassName() {
    return nodeClassName;
  }
  /**
   * Return a list of args on the cmdl that are not delimited by special := mapping modifier
   * @return
   */
  public List<String> getNodeArguments() {
    return Collections.unmodifiableList(nodeArguments);
  }
  /**
   * Return cmdl args with := but not __ prefix that have been translated to GraphName.of
   * @return
   */
  public Map<GraphName, GraphName> getRemappings() {
	    return Collections.unmodifiableMap(remappings);
  }
  /**
   * Return cmdl args with __ at prefix
   * @return
   */
  public Map<String, String> getSpecialRemappings() {
	  return Collections.unmodifiableMap(specialRemappings);
  }
  /**
   * Perform a build on the command line args accounting for remappings delimited by __<p>
   * Generate a node name from the getNodeClassName() declared no-arg constructor invoked to
   * perform the declared gerDefaultNodeName method in the class. If this fails, generate an anonymous name
   * from GraphName.newAnonomyous().<p>
   * From there, extract the __name node name from the command line, if its there, and use that. We need a consistent,
   * legitimate node name for proper operation. For the RosCore master node we can use an anonymous name,
   * but otherwise we need proper NodeRegistrationInfo from invocation to invocation. Finally,
   * build up the NodeConfiguration with parent resolver, ROS root, package path, master Uri, and this CommandLineLoader instance.
   * @return The NodeConfiguration
   */
  public NodeConfiguration build() {
	  parseRemappingArguments();
	  NodeConfiguration nodeConfiguration;
	  String nodeName = null;
	  try {
		  Class c = Class.forName(getNodeClassName());
		  Object o = c.getDeclaredConstructor().newInstance();
		  Method m = c.getDeclaredMethod("getDefaultNodeName");
		  GraphName g = (GraphName) m.invoke(o);
		  nodeName = g.toString();
	  } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException| NoSuchMethodException | ClassNotFoundException e) {
		  //e.printStackTrace();
		  nodeName = GraphName.newAnonymous().toString();
	  }
	  if (specialRemappings.containsKey(CommandLineVariables.NODE_NAME)) {
		  nodeName = specialRemappings.get(CommandLineVariables.NODE_NAME);
	  }
	  if (specialRemappings.containsKey(CommandLineVariables.NODE_VISIBILITY)) {
		  if(specialRemappings.get(CommandLineVariables.NODE_VISIBILITY).equals("private")) {
			  nodeConfiguration = NodeConfiguration.newPrivate(nodeName, getMasterUri(), (jcl != null ? jcl : getClass().getClassLoader()));
		  } else {
			  nodeConfiguration = NodeConfiguration.newPublic(nodeName, getHost(), getMasterUri(), (jcl != null ? jcl : getClass().getClassLoader()));
		  }
	  } else {
		  nodeConfiguration = NodeConfiguration.newPublic(nodeName, getHost(), getMasterUri(), (jcl != null ? jcl : getClass().getClassLoader()));
	  }
	  nodeConfiguration.setParentResolver(buildParentResolver());
	  nodeConfiguration.setRosRoot(getRosRoot());
	  nodeConfiguration.setRosPackagePath(getRosPackagePath());
	  nodeConfiguration.setMasterUri(getMasterUri());
	  nodeConfiguration.setCommandLineLoader(this);
	  return nodeConfiguration;
  }

  private void parseRemappingArguments() {
	  for (String remapping : remappingArguments) {
		  assert(remapping.contains(":="));
		  String[] remap = remapping.split(":=");
		  if(remap.length < 2)
			  continue;
		  if (remap.length > 2) {
			  throw new IllegalArgumentException("Invalid remapping argument: " + remapping);
		  }
		  if (remapping.startsWith("__")) {
			  if(DEBUG)
				  log.info("specialRemap="+remap[0]+":="+remap[1]);
			  specialRemappings.put(remap[0], remap[1]);
		  } else {
			  remappings.put(GraphName.of(remap[0]), GraphName.of(remap[1]));
		  }
	  }
  }

  /**
   * Precedence:
   * 
   * <ol>
   * <li>The __ns:= command line argument.</li>
   * <li>The ROS_NAMESPACE environment variable.</li>
   * </ol>
   */
  private NameResolver buildParentResolver() {
	  GraphName namespace = GraphName.root();
	  if (specialRemappings.containsKey(CommandLineVariables.ROS_NAMESPACE)) {
		  namespace =
				  GraphName.of(specialRemappings.get(CommandLineVariables.ROS_NAMESPACE)).toGlobal();
	  } else if (environment.containsKey(EnvironmentVariables.ROS_NAMESPACE)) {
		  namespace = GraphName.of(environment.get(EnvironmentVariables.ROS_NAMESPACE)).toGlobal();
	  }
	  return new NameResolver(namespace, remappings);
  }

  /**
   * Precedence (default: null):
   * 
   * <ol>
   * <li>The __ip:= command line argument.</li>
   * <li>The ROS_IP environment variable.</li>
   * <li>The ROS_HOSTNAME environment variable.</li>
   * <li>The default host as specified in {@link NodeConfiguration}.</li>
   * </ol>
   */
  public String getHost() {
	  String host = InetSocketAddressFactory.newNonLoopback().getCanonicalHostName();
	  if (specialRemappings.containsKey(CommandLineVariables.ROS_IP)) {
		  host = specialRemappings.get(CommandLineVariables.ROS_IP);
	  } else if (environment.containsKey(EnvironmentVariables.ROS_IP)) {
		  host = environment.get(EnvironmentVariables.ROS_IP);
	  } else if (environment.containsKey(EnvironmentVariables.ROS_HOSTNAME)) {
		  host = environment.get(EnvironmentVariables.ROS_HOSTNAME);
	  }
	  return host;
  }

  /**
   * Define the master URI, in the case of a node its the running {@link RosCOre} in the case of RosCore
   * its the interface or InetAddress of the NodeConfiguration.MAIN_PORT. If we specify
   * __iface:=interface, or __master:=address <p>
   * Precedence:
   * 
   * <ol>
   * <li>The __master:= command line argument. This is not required but easy to
   * support.</li>
   * <li>The ROS_MASTER_URI environment variable.</li>
   * <li>The default master URI as defined in {@link NodeConfiguration}.</li>
   * </ol>
   */
  public InetSocketAddress getMasterUri() {
	  InetSocketAddress uri = NodeConfiguration.DEFAULT_MASTER_URI;
	  if(specialRemappings.containsKey(CommandLineVariables.ROS_MASTER_URI)) {
		  uri = new InetSocketAddress(specialRemappings.get(CommandLineVariables.ROS_MASTER_URI), NodeConfiguration.MAIN_PORT);
	  } else 
		  if(environment.containsKey(EnvironmentVariables.ROS_MASTER_URI)) {
			  uri = new InetSocketAddress(environment.get(EnvironmentVariables.ROS_MASTER_URI), NodeConfiguration.MAIN_PORT);
		  } else {
			  if(specialRemappings.containsKey(CommandLineVariables.ROS_MASTER_IFACE))  {
				  try {
					  NetworkInterface ni = NetworkInterface.getByName(specialRemappings.get(CommandLineVariables.ROS_MASTER_IFACE));
					  Iterator<InetAddress> it = ni.getInetAddresses().asIterator();
					  while(it.hasNext()) {
						  InetAddress e = it.next();
						  if (e instanceof Inet4Address && !e.isLoopbackAddress() && e.isReachable(100)) {
							  uri = new InetSocketAddress(e, NodeConfiguration.MAIN_PORT);
							  break;
						  }
					  }
				  } catch (SocketException e) {
					  log.error("Unknown interface:"+specialRemappings.get(CommandLineVariables.ROS_MASTER_IFACE));
				  } catch (IOException e1) {
					  log.error("Can't reach interface:"+specialRemappings.get(CommandLineVariables.ROS_MASTER_IFACE));
				  }
			  }
		  }
	  return uri;
  }

  public File getRosRoot() {
	  if (environment.containsKey(EnvironmentVariables.ROS_ROOT)) {
		  return new File(environment.get(EnvironmentVariables.ROS_ROOT));
	  } else {
		  // For now, this is not required as we are not doing anything (e.g.
		  // ClassLoader) that requires it. In the future, this may become required.
		  return null;
	  }
  }

  private List<File> getRosPackagePath() {
	  if (environment.containsKey(EnvironmentVariables.ROS_PACKAGE_PATH)) {
		  String rosPackagePath = environment.get(EnvironmentVariables.ROS_PACKAGE_PATH);
		  List<File> paths = new ArrayList<File>();
		  for (String path : rosPackagePath.split(File.pathSeparator)) {
			  paths.add(new File(path));
		  }
		  return paths;
	  } else {
		  return new ArrayList<File>();
	  }
  }

  /**
   * Load main node using default classloader
   * @param name the name of the class extending AbstractNodeMain
   * @return an instance of {@link NodeMain}
   * @throws ClassNotFoundException
   * @throws InstantiationException
   * @throws IllegalAccessException
   */
  public NodeMain loadClass(String name) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
	  if(jcl != null)
		  return loadClassWithJars(name);
	  Class<?> clazz = getClass().getClassLoader().loadClass(name);
	  Method meth = null;
	  if( clazz.getConstructors().length == 0 ) { // no public constructors, lets try to get the singleton instance
		  try {
			  meth = clazz.getMethod("getInstance",(Class<?>[])null);
			  return NodeMain.class.cast(meth.invoke(null, (Object[])null));
		  } catch (NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException e) {
			  throw new InstantiationException(e.getMessage());
		  }
	  }
	  return NodeMain.class.cast(clazz.newInstance());
  }

  public void createJarClassLoader() {
	  jcl = new JarClassLoader();
  }

  public JarClassLoader getJarClassLoader() {
	  return jcl;
  }
  /**
   * Load main node using JarClassLoader and ParmeterTree for provisioning remote nodes 
   * @param name the name of the class extending AbstractNodeMain
   * @return an instance of {@link NodeMain}
   * @throws ClassNotFoundException
   * @throws InstantiationException
   * @throws IllegalAccessException
   */
  private NodeMain loadClassWithJars(String name) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
	  Class<?> clazz = jcl.loadClass(name);
	  if( clazz.getConstructors().length == 0 ) { // no public constructors, lets try to get the singleton instance
		  try {	
			  return (NodeMain) jcl.invokeMethodReturn(clazz, "getInstance", null);
		  } catch ( Throwable e) {
			  throw new InstantiationException(e.getMessage());
		  }
	  }
	  try {
		  return (NodeMain) clazz.newInstance();
	  } catch(ClassCastException cce) {
		  log.error(cce);
		  log.error(clazz.getProtectionDomain()+" "+clazz.getProtectionDomain().getClassLoader()+" "+clazz.getTypeName()+" "+clazz.toGenericString()+" "+AbstractNodeMain.class.isInstance(clazz.newInstance()));
		  log.error(NodeMain.class.getProtectionDomain()+" "+NodeMain.class.getProtectionDomain().getClassLoader()+" "+NodeMain.class.getTypeName()+" "+NodeMain.class.toGenericString()+" "+NodeMain.class.isInstance(clazz.newInstance()));
		  throw new InstantiationException(cce.getMessage());
	  }

  }
}
