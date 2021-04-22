package org.ros.internal.transport.tcp;

import java.io.Serializable;

import org.ros.address.AdvertiseAddress;
import org.ros.internal.transport.ProtocolDescription;
import org.ros.internal.transport.ProtocolNames;

/**
 * Protocol descriptor for TCP socket RosJavaLite
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2021
 */
public class TcpRosProtocolDescription extends ProtocolDescription implements Serializable {
  private static final long serialVersionUID = -6976784864294162007L;
  public TcpRosProtocolDescription() { super(); }

  public TcpRosProtocolDescription(AdvertiseAddress address) {
    super(ProtocolNames.TCPROS, address);
  }

}
