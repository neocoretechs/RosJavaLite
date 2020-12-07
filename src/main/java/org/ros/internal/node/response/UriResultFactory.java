package org.ros.internal.node.response;

import java.net.InetSocketAddress;
//import java.net.URI;    
/**
 * @author Jonathan Groff (C) NeocoreTechs 2020
 */
public class UriResultFactory implements ResultFactory<InetSocketAddress> {

  @Override
  public InetSocketAddress newFromValue(Object value) {
    	int strt = ((String)value).indexOf("/");
    	int end = ((String)value).indexOf(":");
    	if(strt == -1 || end == -1) {
    		System.out.println("Format of address of descritor incorrect, got "+value+", expected <hostname>/<n.n.n.n:port>");
    		return null;
    	}
    	String host = ((String)value).substring(strt+1, end);
    	Integer port = Integer.parseInt(((String)value).substring(end+1));
      return new InetSocketAddress(host, port);
  }
}
