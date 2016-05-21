package org.ros.internal.system;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.internal.message.field.DirectByteArrayOutputStream;

public class Utility {
	  private static final Log log = LogFactory.getLog(Utility.class);
	 public static <T> void serialize(T value, ByteBuffer buffer) {
		    //serializer.serialize((Message) value, buffer);
			DirectByteArrayOutputStream dbaos = new DirectByteArrayOutputStream();
			ObjectOutputStream oos;
			try {
				oos = new ObjectOutputStream(dbaos);
				oos.writeObject(value);
				oos.flush();
				buffer.put(dbaos.getBuf());
				oos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	
	 public static Object deserialize(ByteBuffer buffer) {
		    //return deserializer.deserialize(buffer);
			byte[] obuf = buffer.array();
			Object Od = null;
			try {
						ObjectInputStream s;
						ByteArrayInputStream bais = new ByteArrayInputStream(obuf);
						ReadableByteChannel rbc = Channels.newChannel(bais);
						s = new ObjectInputStream(Channels.newInputStream(rbc));
						Od = s.readObject();
						s.close();
						bais.close();
						rbc.close();
			} catch (IOException ioe) {
			} catch (ClassNotFoundException cnf) {
					log.error("Class cannot be deserialized, may have been modified beyond version compatibility");
			}
			return  Od;
				
	}

}
