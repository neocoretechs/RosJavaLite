package org.ros.internal.transport.tcp;

import java.util.concurrent.ExecutorService;

public interface ChannelGroup {
	public void shutdown();
	public ExecutorService getExecutorService();
}
