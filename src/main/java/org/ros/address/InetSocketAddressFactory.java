package org.ros.address;

import org.ros.exception.RosRuntimeException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Utility class to generate addresses for various servers
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class InetSocketAddressFactory {
  
  private InetSocketAddressFactory() {
    // Utility class
  }

  private static boolean isIpv4(InetSocketAddress address) {
    return address.getAddress().getAddress().length == 4;
  }
  private static boolean isIpv4(InetAddress address) {
	    return address.getAddress().length == 4;
  }
  
  private static Collection<InetAddress> getAllInetAddresses() {
    List<NetworkInterface> networkInterfaces;
    List<InetAddress> inetAddresses = new ArrayList<InetAddress>();
    try {
      networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());

      // sort to move ether ahead of wlan or lo
      List<String> sortIface = new ArrayList<String>();
      for (NetworkInterface networkInterface : networkInterfaces) {
    	  if(networkInterface.isUp() && !networkInterface.isLoopback())
        sortIface.add(networkInterface.getName());
      }
      Collections.sort(sortIface);
      //for (NetworkInterface networkInterface : networkInterfaces) {
      for( String iface : sortIface) {
    	  inetAddresses.addAll(Collections.list(NetworkInterface.getByName(iface).getInetAddresses()));//networkInterface.getInetAddresses()));
      }
    } catch (SocketException e) {
        throw new RosRuntimeException(e);
    }
    return inetAddresses;
  }

  public static InetAddress newNonLoopback() {
    for (InetAddress address : getAllInetAddresses()) {
      // IPv4 only for now.
      if (!address.isLoopbackAddress() && isIpv4(address)) {
        return address;
      }
    }
    throw new RosRuntimeException("No non-loopback interface found.");
  }

  private static Collection<InetAddress> getAllInetAddressByName(String host) {
    InetAddress[] allAddressesByName;
    try {
      allAddressesByName = InetAddress.getAllByName(host);
    } catch (UnknownHostException unused) {
      try {
        allAddressesByName = InetAddress.getAllByName(host);
      } catch (UnknownHostException e) {
        throw new RosRuntimeException(e);
      }
    }
    return Arrays.asList(allAddressesByName);
  }

  /**
   * Creates an {@link InetAddress} with both an IP and a host set so that no
   * further resolving will take place.
   * 
   * If an IP address string is specified, this method ensures that it will be
   * used in place of a host name.
   * 
   * If a host name other than {@code Address.LOCALHOST} is specified, this
   * method trys to find a non-loopback IP associated with the supplied host
   * name.
   * 
   * If the specified host name is {@code Address.LOCALHOST}, this method
   * returns a loopback address.
   * 
   * 
   * @param host
   * @return an {@link InetAddress} with both an IP and a host set (no further resolving will take place)
   */
  public static InetSocketAddress newFromHostString(String host) {
        try {
        	InetAddress hostaddr = InetAddress.getByName(host);
			return new InetSocketAddress(hostaddr, findPortOnAddress(hostaddr));
		} catch (IOException e) {
			throw new RosRuntimeException(e);
		}
  }

  public static InetSocketAddress newLoopback() {
    return newFromHostString(Address.LOOPBACK);
  }
  
  public static InetSocketAddress newLoopback(int port) {
	    return new InetSocketAddress(Address.LOOPBACK, port);
  }
  
  private static Integer findPortOnAddress(InetAddress host) throws IOException {
	    try (
	        ServerSocket socket = new ServerSocket(0,0,host);
	    ) {
	      return socket.getLocalPort();
	    }
  }
}
