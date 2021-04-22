package org.ros.internal.node.response;
/**
 * Status codes from remote calls.
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 *
 */
public enum StatusCode {
  ERROR(-1), FAILURE(0), SUCCESS(1), NOTFOUND(2);

  private final int intValue;

  private StatusCode(int value) {
    this.intValue = value;
  }

  public int toInt() {
    return intValue;
  }

  public static StatusCode fromInt(int intValue) {
    switch (intValue) {
    case -1:
      return ERROR;
    case 1:
      return SUCCESS;
    case 2:
      return NOTFOUND;
    case 0:
    default:
      return FAILURE;
    }
  }
  
  @Override
  public String toString() {
    switch (this) {
      case ERROR:
        return "Error";
      case SUCCESS:
        return "Success";
      case NOTFOUND:
    	return "Not Found";
      case FAILURE:
      default:
        return "Failure";
    }
  }
}
