package org.ros.internal.transport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.exception.RosRuntimeException;

import java.io.Serializable;

import java.util.Collections;
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

 
  public ConnectionHeader() {
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
