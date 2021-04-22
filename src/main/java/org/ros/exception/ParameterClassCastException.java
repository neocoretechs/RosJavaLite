package org.ros.exception;

/**
 * Thrown when a requested parameter does not match the requested parameter
 * type.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class ParameterClassCastException extends RosRuntimeException {

  public ParameterClassCastException(String message) {
    super(message);
  }

  public ParameterClassCastException(String message, Throwable throwable) {
    super(message, throwable);
  }

  public ParameterClassCastException(Throwable throwable) {
    super(throwable);
  }
}
