package org.ros.exception;

/**
 * Thrown when a requested parameter is not found.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class ParameterNotFoundException extends RosRuntimeException {

  public ParameterNotFoundException(String message) {
    super(message);
  }

  public ParameterNotFoundException(String message, Throwable throwable) {
    super(message, throwable);
  }

  public ParameterNotFoundException(Throwable throwable) {
    super(throwable);
  }
}
