package org.ros;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.address.AdvertiseAddress;
import org.ros.address.BindAddress;
import org.ros.internal.node.server.ParameterServer;
import org.ros.internal.node.server.master.MasterServer;
import org.ros.internal.transport.tcp.TcpRosServer;
import org.ros.namespace.GraphName;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * {@link RosCore} is a collection of nodes and programs that are pre-requisites
 * of a ROS-based system. You must have a {@link RosCore}
 * running in order for ROS nodes to communicate.
 * The light weight implementation will not interop with standard ROS
 * nodes, the XML RPC has been eliminated in favor of lightweight java serialization protocol.
 * An added capability is the provisioning of remote nodes with JAR files to simplify deployment of
 * numerous remote nodes via the parameter server and the ParameterTree.<p/>
 * Add the -Djars.provision=/path/to/jars on the command line to enable JAR, script, xml and other asset
 * provisioning through the ParameterTree.<br/>
 * On the RosRun remote node add the same command line parameter to write the files to that directory. Due to the
 * way the classloader and the protection domain works its necessary to load the classes and write them to the same directory
 * as the bootstrap JARs for running ROS that are defined on the classpath.<p/>
 * To exclude files and directories from provisioning, add them to a file called _exclusion, otherwise all files and subdirectories
 * will be loaded, but the directory structure will NOT be preserved, just flattened. In addition, due to the way the ParameterTree
 * operates, special characters such as - will be converted to x, and other special characters may cause failure and so
 * resources such as these that are explicitly loaded by applications must be manually provisioned 
 * and added to the _exclusion file.<p/>
 * The real use case is to provide a means to add update of nodes and assets beyond basic bootstrapping to minimize constant
 * reconfiguration and provide a means of "live update".
 * TODO: Add /rosout node.
 * @see <a href="http://www.ros.org/wiki/roscore">roscore documentation</a>
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2021
 */
public class RosCore {
  private static boolean DEBUG = true;
  private static final Log log = LogFactory.getLog(RosCore.class);
  private MasterServer masterServer = null;
  private ParameterServer parameterServer = null;
  public static final String propsEntry = "jars.provision";
  public static final String jarGraph = "/java/";
  public static final String jarParent = "/java";

  /**
   * Start new public Core ROS services servers.
   * @param host The host upon which the master runs
   * @param port The port of the master running
   * @return the RosCore object to further control the servers.
   */
  public static RosCore newPublic(String host, int port) {
    return new RosCore(BindAddress.newPublic(port), new AdvertiseAddress(host, port));
  }
  
  /**
   * Start new public Core ROS services servers.
   * @param port The port of the master running, host assumed default local node name with first acquired interface.
   * @return the RosCore object to further control the servers.
   */
  public static RosCore newPublic(int port) {
    return new RosCore(BindAddress.newPublic(port), AdvertiseAddress.newPublic(port));
  }


  public static RosCore newPrivate() {
	BindAddress ba = BindAddress.newPrivate();
    return new RosCore(ba, AdvertiseAddress.newPrivate());
  }

  private RosCore(BindAddress bindAddress, AdvertiseAddress advertiseAddress) {
	  if(DEBUG)
		  log.info("RosCore initialization with bind:"+bindAddress+" advertise:"+advertiseAddress);
    try {
		masterServer = new MasterServer(bindAddress, advertiseAddress);
		parameterServer = new ParameterServer(bindAddress, new AdvertiseAddress(advertiseAddress.getHost(),advertiseAddress.getPort()+1));
	} catch (IOException e) {
		log.error("RosCore fault, master server can not be constructed due to "+e,e);
		//e.printStackTrace();
	}
  }
  /**
   * Start the master and parameter servers. Read the exclusion file and provision the parameter server
   * with assets for the remote nodes to acquire.
   */
  public void start() {
    masterServer.start();
    parameterServer.start();
    // see if we are going to load JARs for provisioning remote nodes.
	String jarsDir = System.getProperty(propsEntry);
	if(jarsDir != null) {
		log.info("> Processing top level resource directory:"+jarsDir+" for remote node provisioning via ParameterTree");
		try {
			// read exclusion file
			File exclusion = new File((new File(jarsDir).getAbsolutePath())+("/_exclusion"));
			String exString = new String(readFile(exclusion));
			String[] exclusions = exString.split("\\r?\\n");
			for(String s: exclusions)
				log.info(s+" will be excluded from resource provisioning.");
			processFilesForFolder(new File(jarsDir), exclusions);
		} catch (IOException e) {
			log.error("Was unable to process resource provisioning directory:"+jarsDir+" due to "+e);
		}
	}
  }
  /**
   * Recursively process the files from the provided top level folder with potential exclusions.
   * @param folder Top level folder, recursively processed, or file, processed terminally.
   * @param exclusions Array of exclusion asset names, file and directory, without path, assumed relative.
   * @throws IOException
   */
  private void processFilesForFolder(final File folder, String[] exclusions) throws IOException {
		for (final File fileEntry : folder.listFiles()) {
			boolean include = true;
			if(fileEntry.isDirectory()) {
				log.info(">> Processing resource subdirectory:"+fileEntry.getName());
				// see if this directory is excluded
				for(String checkDir : exclusions)
					if(checkDir.equals(fileEntry.getName())) {
						log.info("Excluding directory "+checkDir+" from resource provisioning.");
						include = false;
						break;
					}
				if(include)
					processFilesForFolder(fileEntry, exclusions);
			} else {
				// check against file exclusion
				for(String checkFile : exclusions)
					if(checkFile.equals(fileEntry.getName())) {
						log.info("Excluding file "+checkFile+" from resource provisioning.");
						include = false;
						break;
					}
				if(include) {
					log.info(">>> Reading resource: "+fileEntry.getName());
					// form valid global graph name
					String graphJar = jarGraph+fileEntry.getName().replace('.', '_').replace('-','x');
					parameterServer.set(GraphName.of(graphJar), readFile(fileEntry));
				}
			}
		}
  }
  /**
   * Read a file into a byte array payload.
   * @param file
   * @return
   * @throws IOException
   */
  private static byte[] readFile(File file) throws IOException {
		// Open file
		RandomAccessFile f = new RandomAccessFile(file, "r");
		try {
			// Get and check length
			long longlength = f.length();
			int length = (int) longlength;
			if (length != longlength)
				throw new IOException("File size >= 2 GB");
			// Read file and return data
			byte[] data = new byte[length];
			f.readFully(data);
			return data;
		} finally {
			f.close();
		}
  }
  
  /**
   * Get the "Uri" of the master server, which in RosJavaLite is the InetSocketAddress.
   * @return
   */
  public InetSocketAddress getUri() {
    return masterServer.getUri();
  }

  /**
   * Await the start of the master and parameter servers.
   * @throws InterruptedException
   */
  public void awaitStart() throws InterruptedException {
    masterServer.awaitStart();
    parameterServer.awaitStart();
  }

  /**
   * Await the start of the mater and parameter servers using the specified timeouts.
   * @param timeout
   * @param unit
   * @return true if both servers started in the specified timeframes.
   * @throws InterruptedException
   */
  public boolean awaitStart(long timeout, TimeUnit unit) throws InterruptedException {
    boolean ms = masterServer.awaitStart(timeout, unit);
    boolean ps = parameterServer.awaitStart(timeout,  unit);
    return ms & ps;
  }

  /**
   * Shut down the parameter server, then the master server.
   */
  public void shutdown() {
    try {
    	parameterServer.shutdown();
		masterServer.shutdown();
	} catch (IOException e) {
		log.error("Can not shut down master server due to "+e,e);
		//e.printStackTrace();
	}
  }

  public ParameterServer getParameterServer() {
	  return parameterServer;
  }
  
  public MasterServer getMasterServer() {
    return masterServer;
  }
  /**
   * Start a new public RosCore on default port 8090 using first acquired interface.
   * Wait 1 second for each server to start before throwing an exception.
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
	   RosCore rosCore = RosCore.newPublic(8090);
	   rosCore.start();
	   rosCore.awaitStart(1, TimeUnit.SECONDS);
	   log.info("RosLite Master started @ address "+rosCore.getUri());
  }
}
