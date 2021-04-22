package org.ros.address;

/**
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class PrivateAdvertiseAddressFactory implements AdvertiseAddressFactory {

  @Override
  public AdvertiseAddress newDefault() {
    return new AdvertiseAddress(Address.LOOPBACK);
  }
}
