package org.ros.address;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

import org.ros.exception.RosRuntimeException;

/**
 * An {@link AdvertiseAddressFactory} which creates public (non-loopback) addresses.
 * @author jg
 */
public class PublicAdvertiseAddressFactory implements AdvertiseAddressFactory {

  private final InetAddress host;

  public PublicAdvertiseAddressFactory() {
    this(InetSocketAddressFactory.newNonLoopback());
  }

  public PublicAdvertiseAddressFactory(InetAddress host) {
	    this.host = host;
  }
  
  public PublicAdvertiseAddressFactory(String host) {
	    try {
			this.host = InetAddress.getByName(host);
		} catch (UnknownHostException e) {
			throw new RosRuntimeException("Host name "+host+" cannot be resolved");
		}
}

  public AdvertiseAddress newDefault(int port) {
    return new AdvertiseAddress(host, port);
  }

  @Override
  public AdvertiseAddress newDefault() {
	try {
		return new AdvertiseAddress(host, findPortOnAddress(host));
	} catch (IOException e) {
		throw new RosRuntimeException("Cannot generate new default advertise address due to "+e);
	}
  }
  
  private static Integer findPortOnAddress(InetAddress host) throws IOException {
	    try (
	        ServerSocket socket = new ServerSocket(0,0,host);
	    ) {
	      return socket.getLocalPort();
	    }
  }
}
