package org.ros.internal.transport;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author Groff
 */
public final class ProtocolNames implements Serializable {
  private static final long serialVersionUID = -7758680000961964374L;
  public static String TCPROS = "TCPROS";
  public static String UDPROS = "UDPROS";
  public static Collection<String> SUPPORTED = new HashSet<String>();
  public static Collection<String> TCP = new HashSet<String>();
  public static Collection<String> UDP = new HashSet<String>();
  static {
	  SUPPORTED.add(TCPROS);
	  TCP.add(TCPROS);
	  UDP.add(UDPROS);
  }
  public ProtocolNames(){}
}
