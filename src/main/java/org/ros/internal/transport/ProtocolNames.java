package org.ros.internal.transport;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author Groff
 */
public final class ProtocolNames {
  
  public static final String TCPROS = "TCPROS";
  public static final String UDPROS = "UDPROS";
  public static final Collection<String> SUPPORTED = new HashSet<String>();
  public static final Collection<String> TCP = new HashSet<String>();
  public static final Collection<String> UDP = new HashSet<String>();
  static {
	  SUPPORTED.add(TCPROS);
	  TCP.add(TCPROS);
	  UDP.add(UDPROS);
  }
  private ProtocolNames(){}
}
