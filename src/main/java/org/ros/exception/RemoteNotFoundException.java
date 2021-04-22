package org.ros.exception;

import org.ros.internal.node.response.StatusCode;

/**
 * Remote exception indication a remote resource, such as a publisher, was not found and we want
 * to toss a checked exception to handle that.
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2018,2021
 */
public class RemoteNotFoundException extends RosException {
	private static final long serialVersionUID = 2514173639723076472L;

	public RemoteNotFoundException(final Throwable throwable) {
		super(throwable);
	}

	public RemoteNotFoundException(final String message, final Throwable throwable) {
		super(message, throwable);
	}

	public RemoteNotFoundException(final String message) {
		super(message);
	}

	public RemoteNotFoundException(StatusCode statusCode, String message) {
		super(statusCode.toString()+" "+message);
	}
}
