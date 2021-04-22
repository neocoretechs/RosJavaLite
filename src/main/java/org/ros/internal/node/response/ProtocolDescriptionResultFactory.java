package org.ros.internal.node.response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ros.address.AdvertiseAddress;
import org.ros.internal.transport.ProtocolDescription;
import org.ros.internal.transport.tcp.TcpRosProtocolDescription;


/**
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class ProtocolDescriptionResultFactory implements ResultFactory<ProtocolDescription> {

  @Override
  public ProtocolDescription newFromValue(Object value) {
    List<Object> protocolParameters = Arrays.asList(value);
    //assert(protocolParameters.size() == 3);
    //assert(protocolParameters.get(0).equals(ProtocolNames.TCPROS));
    ArrayList params = (ArrayList) protocolParameters.get(0);
    AdvertiseAddress address = new AdvertiseAddress((String) params.get(1), (Integer) params.get(2));
    return new TcpRosProtocolDescription(address);
  }
}
