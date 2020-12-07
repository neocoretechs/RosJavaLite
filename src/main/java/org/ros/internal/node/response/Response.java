package org.ros.internal.node.response;

import org.ros.exception.RemoteException;
import org.ros.exception.RemoteNotFoundException;
import org.ros.exception.RosRuntimeException;

import java.util.ArrayList;
import java.util.List;

/**
 * The response from a remote call.
 * 
 * @author jg Copyright (C) NeoCoreTechs 2018
 */
public class Response<T> {

  private final StatusCode statusCode;
  private final String statusMessage;
  private final T result;

  public static <T> Response<T> newError(String message, T value) {
    return new Response<T>(StatusCode.ERROR, message, value);
  }

  public static <T> Response<T> newFailure(String message, T value) {
    return new Response<T>(StatusCode.FAILURE, message, value);
  }

  public static <T> Response<T> newSuccess(String message, T value) {
    return new Response<T>(StatusCode.SUCCESS, message, value);
  }

  public static <T> Response<T> newNotfound(String message, T value) {
	    return new Response<T>(StatusCode.NOTFOUND, message, value);
  }

  /**
   * Creates a {@link Response} from the {@link List} of {@link Object}s
   * returned from a remote call. Throws {@link RemoteException} if the
   * {@link StatusCode} is StatusCode.FAILURE.
   * 
   * @param <T>
   * @param response
   *          the {@link List} of {@link Object}s returned from the remote call
   * @param resultFactory
   *          a {@link ResultFactory} that creates a result from the third
   *          {@link Object} in the {@link Response}
   * @return a {@link Response} using the specified {@link ResultFactory} to
   *         generate the result
   * @throws RemoteException
   *           if the {@link Response}'s {@link StatusCode} indicates
   *           StatusCode.FAILURE.
   */
  public static <T> Response<T> fromListCheckedFailure(List<Object> response,
      ResultFactory<T> resultFactory) throws RemoteException {
    StatusCode statusCode = StatusCode.SUCCESS;
    String message;
    try {
      statusCode = StatusCode.fromInt((Integer) response.get(0));
      message = (String) response.get(1);
      if (statusCode == StatusCode.FAILURE) {
        throw new RemoteException(statusCode, message);
      }

    } catch (ClassCastException e) {
      throw new RemoteException(StatusCode.FAILURE,
          "Remote side did not return correct type headers in response. Expected StatusCode.class, got "+response.get(0).getClass()+
          " and message of type String but got "+response.get(1).getClass()+" for values "+response.get(0)+" and "+response.get(1));
    }
    try {
      return new Response<T>(statusCode, message, resultFactory.newFromValue(response.get(2)));
    } catch (ClassCastException e) {
      throw new RemoteException(StatusCode.FAILURE,"Remote side did not return correct value type in response. Got "+ response.get(2).getClass()+
      " from value "+ response.get(2)+" expected "+((T)resultFactory).getClass());
    }
  }

  /**
   * Creates a {@link Response} from the {@link List} of {@link Object}s
   * returned from an RPC call. Throws {@link RemoteException} if the
   * {@link StatusCode} is not a success.
   * 
   * @param <T>
   * @param response
   *          the {@link List} of {@link Object}s returned from the remote call
   * @param resultFactory
   *          a {@link ResultFactory} that creates a result from the third
   *          {@link Object} in the {@link Response}
   * @return a {@link Response} using the specified {@link ResultFactory} to
   *         generate the result
   * @throws RemoteException
   *           if the {@link Response}'s {@link StatusCode} does not indicate
   *           success
   */
  public static <T> Response<T> fromListChecked(List<Object> response,
      ResultFactory<T> resultFactory) throws RemoteException {
    StatusCode statusCode;
    String message;
    try {
      statusCode = StatusCode.fromInt((Integer) response.get(0));
      message = (String) response.get(1);
      if (statusCode != StatusCode.SUCCESS) {
        throw new RemoteException(statusCode, message);
      }
    } catch (ClassCastException e) {
        throw new RemoteException(StatusCode.FAILURE,
                "Remote side did not return correct type headers in response. Expected StatusCode.class, got "+response.get(0).getClass()+
                " and message of type String but got "+response.get(1).getClass()+" for values "+response.get(0)+" and "+response.get(1));
    }
    try {
      return new Response<T>(statusCode, message, resultFactory.newFromValue(response.get(2)));
    } catch (ClassCastException e) {
        throw new RemoteException(StatusCode.FAILURE,"Remote side did not return correct value type in response. Got "+ response.get(2).getClass()+
        	      " from value "+ response.get(2)+" expected "+((T)resultFactory).getClass());
    }
  }

  /**
   * Creates a {@link Response} from the {@link List} of {@link Object}s
   * returned from an RPC call. Throws {@link RemoteNotfoundException} if the
   * {@link StatusCode} is not found, and {@link RemoteException) if it is not a success.
   * 
   * @param <T>
   * @param response
   *          the {@link List} of {@link Object}s returned from the remote call
   * @param resultFactory
   *          a {@link ResultFactory} that creates a result from the third
   *          {@link Object} in the {@link Response}
   * @return a {@link Response} using the specified {@link ResultFactory} to
   *         generate the result
   * @throws RemoteException
   *           if the {@link Response}'s {@link StatusCode} does not indicate
   *           success
   * @throws RemoteNotfoundException if the remote call tried to locate a resource not there
   */
  public static <T> Response<T> fromListCheckedNotFound(List<Object> response,
      ResultFactory<T> resultFactory) throws RemoteException, RemoteNotFoundException {
    StatusCode statusCode;
    String message;
    try {
      statusCode = StatusCode.fromInt((Integer) response.get(0));
      message = (String) response.get(1);
      if (statusCode != StatusCode.SUCCESS) {
    	if( statusCode == StatusCode.NOTFOUND )
    		throw new RemoteNotFoundException(statusCode, message);
        throw new RemoteException(statusCode, message);
      }
    } catch (ClassCastException e) {
        throw new RemoteException(StatusCode.FAILURE,
                "Remote side did not return correct type headers in response. Expected StatusCode.class, got "+response.get(0).getClass()+
                " and message of type String but got "+response.get(1).getClass()+" for values "+response.get(0)+" and "+response.get(1));
    }
    try {
      return new Response<T>(statusCode, message, resultFactory.newFromValue(response.get(2)));
    } catch (ClassCastException e) {
        throw new RemoteException(StatusCode.FAILURE,"Remote side did not return correct value type in response. Got "+ response.get(2).getClass()+
        	      " from value "+ response.get(2)+" expected "+((T)resultFactory).getClass());
    }
  }

  public Response(int statusCode, String statusMessage, T value) {
    this(StatusCode.fromInt(statusCode), statusMessage, value);
  }

  public Response(StatusCode statusCode, String statusMessage, T value) {
    this.statusCode = statusCode;
    this.statusMessage = statusMessage;
    this.result = value;
  }

  public List<Object> toList() {
    ArrayList<Object> l1 = new ArrayList<Object>();
    l1.add(statusCode.toInt());
    l1.add(statusMessage);
    l1.add(result == null ? "null" : result);
    return l1;
  }

  public StatusCode getStatusCode() {
    return statusCode;
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  public <T> T getResult() {
    return (T) result;
  }

  @Override
  public String toString() {
    return "Response<" + statusCode + ", " + statusMessage + ", " + result + ">";
  }

  public boolean isSuccess() {
    return statusCode == StatusCode.SUCCESS;
  }
}
