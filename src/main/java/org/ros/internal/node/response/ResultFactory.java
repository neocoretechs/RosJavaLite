package org.ros.internal.node.response;


/**
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 * 
 * @param <T>
 *          the result type
 */
public interface ResultFactory<T> {

  /**
   * @param value
   * @return a value to be returned as the result part of a {@link Response}
   */
  public T newFromValue(Object value);
}
