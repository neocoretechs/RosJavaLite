package org.ros.internal.transport.tcp;

import org.ros.internal.transport.ChannelHandler;

/**
 * Adds a name property to a channelhandler contract
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2021
 */
public interface NamedChannelHandler extends ChannelHandler {

  /**
   * @return the name of this {@link ChannelHandler}
   */
  String getName();
}
