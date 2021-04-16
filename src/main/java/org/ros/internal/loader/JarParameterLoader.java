package org.ros.internal.loader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.RosCore;
import org.ros.concurrent.CancellableLoop;
import org.ros.message.MessageFactory;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeMain;
import org.ros.node.parameter.ParameterListener;
import org.ros.node.parameter.ParameterTree;
import org.ros.node.topic.Publisher;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Load an updated JAR file to the ParameterServer for provisioning to remote nodes on startup.
 * Make sure to use RosRun and not the RosLauncher, since we want to be the provisioner, not the povisionee.
 * 
 * @author Jonathan Groff (C) NeoCoreTechs 2021
 */
public class JarParameterLoader extends AbstractNodeMain {
  private static final Log log = LogFactory.getLog(JarParameterLoader.class);
  private static volatile JarParameterLoader instance = null;
  public static NodeMain getInstance() {
		synchronized(JarParameterLoader.class) {
			if(instance == null) {
				instance = new JarParameterLoader();
			}
		}
		return instance;
	}
  private CountDownLatch awaitStart = new CountDownLatch(1);
  private boolean isSet = false;
  String jarName;
  
  @Override
  public GraphName getDefaultNodeName() {
    return GraphName.of("rosjava/parameter_server_jar_loader");
  }
  
  private JarParameterLoader() {}
  
  @Override
  public void onStart(ConnectedNode connectedNode) {
	Map<String, String> remaps = connectedNode.getNodeConfiguration().getCommandLineLoader().getSpecialRemappings();
	if( remaps.containsKey("__jar") ) {
		jarName = remaps.get("__jar");
	} else {
		log.fatal("Must specify remapped property __jar:=<jarfile> for remote node provisioning via ParameterTree");
		System.exit(1);
	}	
		
    ParameterTree param = connectedNode.getParameterTree();
  
    // tell the waiting constructors that we have registered publishers
 	awaitStart.countDown();
    connectedNode.executeCancellableLoop(new CancellableLoop() {
      @Override
      protected void loop() throws InterruptedException {
		try {
			awaitStart.await();
		} catch (InterruptedException e) {}

		File fileEntry = new File(jarName);
		log.info(">>> Reading JAR "+fileEntry.getName());
		// form valid global graph name
		String graphJar = RosCore.jarGraph+fileEntry.getName().replace('.', '_').replace('-','x');
		param.addParameterListener(graphJar, new ParameterListener() {
			@Override
			public void onNewValue(Object value) {
				isSet = true;			
			}});
		try {
			param.set(GraphName.of(graphJar), readFile(fileEntry));
		} catch (IOException e) {
			log.fatal("Can not read JAR file "+fileEntry);
			e.printStackTrace();
			System.exit(1);
		}
        while(!isSet) {
        	Thread.sleep(100);
        }
        System.exit(1);
      }
    });
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
  
}
