package org.ros.internal.transport.tcp;

import java.util.concurrent.ExecutorService;
/**
 * The ChannelGroup interface specifies an executor service and a shutdown method.<p/>
 * In essence, a way to start a server and stop it, and in doing so, 
 * a group of channels is thereby manifest.
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2021
 *
 */
public interface ChannelGroup {
	public void shutdown();
	public ExecutorService getExecutorService();
}
