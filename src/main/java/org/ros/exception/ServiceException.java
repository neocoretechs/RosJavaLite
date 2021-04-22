package org.ros.exception;

/**
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class ServiceException extends Exception {

  public ServiceException(final Throwable throwable) {
    super(throwable);
  }

  public ServiceException(final String message, final Throwable throwable) {
    super(message, throwable);
  }

  public ServiceException(final String message) {
    super(message);
  }
}
