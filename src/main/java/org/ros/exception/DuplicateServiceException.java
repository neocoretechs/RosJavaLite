
package org.ros.exception;

/**
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class DuplicateServiceException extends RosRuntimeException {

  public DuplicateServiceException(final String message) {
    super(message);
  }
}
