package org.ros.internal.system;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
//import java.nio.channels.Channels;
//import java.nio.channels.ReadableByteChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.internal.message.field.DirectByteArrayOutputStream;
/**
 * Static methods to serialize and deserialize ByteBuffer to/from Object.
 * We are using java serialization. Traditional ROS uses XML/RPC so in general
 * we are using ROS generated messages, with all the associated ROS fields and formats
 * in a Java serialization context. If we need a ROS gateway the bindings should remain straightforward.
 * @author jg
 *
 */
public class Utility {
	 private static boolean DEBUG = true;
	 private static final Log log = LogFactory.getLog(Utility.class);
	 
	 public static <T> void serialize(T value, ByteBuffer buffer) {
			DirectByteArrayOutputStream dbaos = new DirectByteArrayOutputStream();
			ObjectOutputStream oos;
			try {
				oos = new ObjectOutputStream(dbaos);
				oos.writeObject(value);
				oos.flush();
				buffer.clear();
				buffer.put(dbaos.getBuf());
				oos.close();
				buffer.flip();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	
	 public static Object deserialize(ByteBuffer buffer) {
			byte[] obuf = buffer.array();
			Object Od = null;
			try {
						ObjectInputStream s;
						ByteArrayInputStream bais = new ByteArrayInputStream(obuf);
						//ReadableByteChannel rbc = Channels.newChannel(bais);
						s = new ObjectInputStream(bais/*Channels.newInputStream(rbc)*/);
						Od = s.readObject();
						s.close();
						bais.close();
						//rbc.close();
			} catch (IOException | ClassNotFoundException cnf) {
					log.error("Class cannot be deserialized, may have been modified beyond version compatibility");
			}
			if( DEBUG )
				log.info("Deserialize return:"+Od);
			return  Od;
				
	}

}
