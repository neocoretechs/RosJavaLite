package org.ros.internal.transport;

/**
 * Fields found inside the header for node to node communication.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public interface ConnectionHeaderFields {

  public static final String CALLER_ID = "callerid";
  public static final String TOPIC = "topic";
  public static final String MD5_CHECKSUM = "md5sum";
  public static final String TYPE = "type";
  public static final String SERVICE = "service";
  public static final String TCP_NODELAY = "tcp_nodelay";
  public static final String LATCHING = "latching";
  public static final String PERSISTENT = "persistent";
  public static final String MESSAGE_DEFINITION = "message_definition";
  public static final String ERROR = "error";
  public static final String PROBE = "probe";
}
