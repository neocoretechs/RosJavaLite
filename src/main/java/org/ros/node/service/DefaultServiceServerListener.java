package org.ros.node.service;

/**
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class DefaultServiceServerListener<T, S> implements ServiceServerListener<T, S> {

  @Override
  public void onMasterRegistrationSuccess(ServiceServer<T, S> registrant) {
  }

  @Override
  public void onMasterRegistrationFailure(ServiceServer<T, S> registrant) {
  }

  @Override
  public void onMasterUnregistrationSuccess(ServiceServer<T, S> registrant) {
  }

  @Override
  public void onMasterUnregistrationFailure(ServiceServer<T, S> registrant) {
  }

  @Override
  public void onShutdown(ServiceServer<T, S> serviceServer) {
  }
}
