package org.ros.internal.node;

/**
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public interface RegistrantListener<T> {

  /**
   * The registrant has been registered with the master.
   * 
   * @param registrant
   *          the registrant which has been registered
   */
  void onMasterRegistrationSuccess(T registrant);

  /**
   * The registrant has failed to register with the master.
   * 
   * <p>
   * This may be called multiple times per registrant since master registration
   * will be retried until success.
   * 
   * @param registrant
   *          the registrant which has been registered
   */
  void onMasterRegistrationFailure(T registrant);

  /**
   * The registrant has been unregistered with the master.
   * 
   * @param registrant
   *          the registrant which has been unregistered
   */
  void onMasterUnregistrationSuccess(T registrant);

  /**
   * The registrant has failed to unregister with the master.
   * 
   * <p>
   * This may be called multiple times per registrant since master
   * unregistration will be retried until success.
   * 
   * @param registrant
   *          the registrant which has been unregistered
   */
  void onMasterUnregistrationFailure(T registrant);
}