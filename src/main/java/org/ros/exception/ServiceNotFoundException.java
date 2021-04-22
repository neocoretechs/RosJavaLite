package org.ros.exception;

/**
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class ServiceNotFoundException extends RosException {

	private static final long serialVersionUID = -706891733230399864L;

	public ServiceNotFoundException(final Throwable throwable) {
		super(throwable);
	}

	public ServiceNotFoundException(final String message, final Throwable throwable) {
		super(message, throwable);
	}

	public ServiceNotFoundException(final String message) {
		super(message);
	}
}
