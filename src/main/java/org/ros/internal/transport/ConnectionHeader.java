package org.ros.internal.transport;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.exception.RosRuntimeException;
import org.ros.internal.message.MessageBuffers;
import org.ros.internal.system.Utility;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jg
 */
public class ConnectionHeader implements Serializable {
  private static final long serialVersionUID = -7508346673596180951L;
  private static final boolean DEBUG = true;
  private static final Log log = LogFactory.getLog(ConnectionHeader.class);

  private Map<String, String> fields = new ConcurrentHashMap<String, String>();

  /**
   * Decodes a header that came over the wire into a {@link Map} of fields and
   * values.
   * 
   * @param buffer
   *          the incoming {@link ChannelBuffer} containing the header
   * @return a {@link Map} of header fields and values
   */
  public static ConnectionHeader decode(ByteBuffer buffer) {
	  /*
    Map<String, String> fields = new HashMap<String, String>();
    int position = 0;
    int readableBytes = buffer.limit();
    while (position < readableBytes) {
      int fieldSize = buffer.getInt();
      position += 4;
      if (fieldSize == 0) {
        throw new IllegalStateException("Invalid 0 length handshake header field.");
      }
      if (position + fieldSize > readableBytes) {
        throw new IllegalStateException("Invalid line length handshake header field.");
      }
      String field = decodeAsciiString(buffer, fieldSize);
      position += field.length();
      assert(field.indexOf("=") > 0 ) : String.format("Invalid field in handshake header: \"%s\"", field);
      String[] keyAndValue = field.split("=");
      if (keyAndValue.length == 1) {
        fields.put(keyAndValue[0], "");
      } else {
        fields.put(keyAndValue[0], keyAndValue[1]);
      }
    }
    if (DEBUG) {
      log.info("Decoded header: " + fields);
    }
    ConnectionHeader connectionHeader = new ConnectionHeader();
    connectionHeader.mergeFields(fields);
    */
	  for(int i =0; i < buffer.position(); i++) { if( buffer.get(i) != 0 ){ log.info("decode Ok not 0"); break;}}
	  buffer.position(0);
    ConnectionHeader connectionHeader = (ConnectionHeader) Utility.deserialize(buffer);
	if( DEBUG )
		 log.info("ConnectionHeader decode:"+connectionHeader+" from buffer:"+buffer);
    return connectionHeader;
  }

  private static String decodeAsciiString(ByteBuffer buffer, int length) {
	byte[] b = new byte[length];
    return buffer.get(b).toString();//Charset.forName("US-ASCII"));
  }

  public ConnectionHeader() {
  }

  /**
   * Encodes this {@link ConnectionHeader} for transmission over the wire.
   * 
   * @return a {@link ChannelBuffer} containing the encoded header for wire
   *         transmission
   */
  public ByteBuffer encode() {
	  /*
    ByteBuffer buffer = MessageBuffers.dynamicBuffer();
    buffer.clear();
    for (Entry<String, String> entry : fields.entrySet()) {
      String field = entry.getKey() + "=" + entry.getValue();
      buffer.putInt(field.length());
      buffer.put(field.getBytes(Charset.forName("US-ASCII")));
    }
    return (ByteBuffer) buffer.flip();
    */
	  ByteBuffer buffer = MessageBuffers.dynamicBuffer();
	  buffer.clear();
	  Utility.serialize(this, buffer);
	  if( DEBUG )
		  log.info("Encode:"+buffer);
	  return buffer;
  }

  public void merge(ConnectionHeader other) {
    Map<String, String> otherFields = other.getFields();
    mergeFields(otherFields);
  }

  public void mergeFields(Map<String, String> other) {
    for (Entry<String, String> field : other.entrySet()) {
      String name = field.getKey();
      String value = field.getValue();
      addField(name, value);
    }
  }

  public void addField(String name, String value) {
    if (!fields.containsKey(name) || fields.get(name).equals(value)) {
      fields.put(name, value);
    } else {
      throw new RosRuntimeException(String.format("Unable to merge field %s: %s != %s", name,
          value, fields.get(name)));
    }
  }

  public Map<String, String> getFields() {
    return Collections.unmodifiableMap(fields);
  }

  public boolean hasField(String name) {
    return fields.containsKey(name);
  }

  public String getField(String name) {
    return fields.get(name);
  }

  @Override
  public String toString() {
    return String.format("ConnectionHeader <%s>", fields.toString());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((fields == null) ? 0 : fields.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ConnectionHeader other = (ConnectionHeader) obj;
    if (fields == null) {
      if (other.fields != null)
        return false;
    } else if (!fields.equals(other.fields))
      return false;
    return true;
  }
}
