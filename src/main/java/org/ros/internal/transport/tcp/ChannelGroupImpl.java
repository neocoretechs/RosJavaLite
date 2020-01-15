package org.ros.internal.transport.tcp;

import java.util.concurrent.ExecutorService;
/**
 * Manipulate groups of channels al la AsynchronousChannelGroup.
 * Primarily contains the ExecutorService by which threads are put to work.
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
	public ExecutorService getExecutorService() { return executorService; }
	
	@Override
	public boolean equals(Object o) {
		return (executorService == ((ChannelGroup)o).getExecutorService());
	}
	@Override
	public int hashCode() {
		return executorService.hashCode();
	}
	@Override
	public String toString() {
		return "ChannelGroup executor "+executorService;
	}
}
