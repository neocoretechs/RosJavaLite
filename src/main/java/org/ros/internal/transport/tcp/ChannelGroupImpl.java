package org.ros.internal.transport.tcp;

import java.util.concurrent.ExecutorService;
/**
 * Manipulate groups of channels al la AsynchronousChannelGroup
 * @author jg
 *
 */
public class ChannelGroupImpl implements ChannelGroup {
	private ExecutorService executorService;
	public ChannelGroupImpl(ExecutorService executorService) {
		this.executorService = executorService;
	}
	public void shutdown() { 
		executorService.shutdown();
	}
}
