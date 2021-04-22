package org.ros.address;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import org.ros.exception.RosRuntimeException;

/**
 * A wrapper for {@link InetSocketAddress} that emphasizes the difference
 * between an address that should be used for binding a server port and one that
 * should be advertised to external entities.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class BindAddress {

  private final InetSocketAddress address;

  private BindAddress(InetSocketAddress address) {
    this.address = address;
  }

  /**
   * @param port the port to bind to
   * @return a {@link BindAddress} instance with specified port that will bind to all network interfaces on the host
   */
  public static BindAddress newPublic(int port) {
    return new BindAddress(new InetSocketAddress(InetSocketAddressFactory.newNonLoopback(), port));
  }

  public static BindAddress newPublic() {
	InetAddress host = InetSocketAddressFactory.newNonLoopback();
    try {
		return new BindAddress(new InetSocketAddress(host, findPortOnAddress(host)));
	} catch (IOException e) {
		throw new RosRuntimeException(e);
	}
  }

  /**
   * @param port the port to bind to
   * @return a {@link BindAddress} instance with specified port that will bind to the loopback interface on the host
   */
  public static BindAddress newPrivate(int port) {
    return new BindAddress(InetSocketAddressFactory.newLoopback(port));
  }

  public static BindAddress newPrivate() {
	return new BindAddress(InetSocketAddressFactory.newLoopback());
  }

  @Override
  public String toString() {
    return "BindAddress<" + address + ">";
  }

  public InetSocketAddress toInetSocketAddress() {
    return address;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((address == null) ? 0 : address.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    BindAddress other = (BindAddress) obj;
    if (address == null) {
      if (other.address != null) return false;
    } else if (!address.equals(other.address)) return false;
    return true;
  }
  
  private static Integer findPortOnAddress(InetAddress host) throws IOException {
	    try (
	        ServerSocket socket = new ServerSocket(0,0,host);
	    ) {
	      return socket.getLocalPort();
	    }
}
}
