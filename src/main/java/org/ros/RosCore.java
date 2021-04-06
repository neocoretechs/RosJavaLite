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
 * Add the -Djars.provision=/path/to/jars on the command line to enable JAR provisioning through the ParameterTree.<br/>
 * On the RosRun remote node add __jarclasses:=/directory/to/jarfiles as a remapped property to designate
 * the directory to write the localized JARs to.<p/> 
 * TODO: Add /rosout node.
 * @see <a href="http://www.ros.org/wiki/roscore">roscore documentation</a>
 * 
 * @author Jonathan Groff (c) NeoCoreTechs 2021
 */
public class RosCore {
  private static boolean DEBUG = true;
  private static final Log log = LogFactory.getLog(RosCore.class);
  private MasterServer masterServer = null;
  private ParameterServer parameterServer = null;
  private static final String propsEntry = "jars.provision";
  public static final String jarGraph = "/java/";
  public static final String jarParent = "/java";

  public static RosCore newPublic(String host, int port) {
    return new RosCore(BindAddress.newPublic(port), new AdvertiseAddress(host, port));
  }

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

  public void start() {
    masterServer.start();
    parameterServer.start();
    // see if we are going to load JARs for provisioning remote nodes.
	String jarsDir = System.getProperty(propsEntry);
	if(jarsDir != null) {
		log.info("> Processing top level JARs directory:"+jarsDir+" for remote node provisioning via ParameterTree");
		try {
			processFilesForFolder(new File(jarsDir));
		} catch (IOException e) {
			log.error("Was unable to process JARs provisioning directory:"+jarsDir+" due to "+e);
		}
	}
  }

  private void processFilesForFolder(final File folder) throws IOException {
		for (final File fileEntry : folder.listFiles()) {
			if (fileEntry.isDirectory()) {
				log.info(">> Processing JAR subdirectory:"+fileEntry.getName());
				processFilesForFolder(fileEntry);
			} else {
				log.info(">>> Reading JAR "+fileEntry.getName());
				// form valid global graph name
				String graphJar = jarGraph+fileEntry.getName().replace('.', '_').replace('-','x');
				parameterServer.set(GraphName.of(graphJar), readFile(fileEntry));
			}
		}
  }
  
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
  
  public InetSocketAddress getUri() {
    return masterServer.getUri();
  }

  public void awaitStart() throws InterruptedException {
    masterServer.awaitStart();
    parameterServer.awaitStart();
  }

  public boolean awaitStart(long timeout, TimeUnit unit) throws InterruptedException {
    boolean ms = masterServer.awaitStart(timeout, unit);
    boolean ps = parameterServer.awaitStart(timeout,  unit);
    return ms & ps;
  }

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
  
  public static void main(String[] args) throws Exception {
	   RosCore rosCore = RosCore.newPublic(8090);
	   rosCore.start();
	   rosCore.awaitStart(1, TimeUnit.SECONDS);
	   log.info("RosLite Master started @ address "+rosCore.getUri());
  }
}
