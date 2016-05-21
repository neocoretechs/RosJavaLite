package org.ros.internal.transport;

public interface ChannelHandler {
	 /**
     * Gets called after the {@link ChannelHandler} was added to the actual context and it's ready to handle events.
     */
    void handlerAdded(ChannelHandlerContext ctx) throws Exception;

    /**
     * Gets called after the {@link ChannelHandler} was removed from the actual context and it doesn't handle events
     * anymore.
     */
    void handlerRemoved(ChannelHandlerContext ctx) throws Exception;
    
	 /**
     * Gets called after the {@link Channel} gets initialized
     */
    void channelActive(ChannelHandlerContext ctx) throws Exception;

    /**
     * Gets called after the {@link Channel} gets shut down
     */
    void channelInactive(ChannelHandlerContext ctx) throws Exception;
    
    /**
     * Activated when read of channel buffer occurs
     * @param ctx
     * @param msg
     * @return
     */
	Object channelRead(ChannelHandlerContext ctx, Object msg) throws Exception;
	
	/**
	 * Activated when read is completed
	 */
	void channelReadComplete(ChannelHandlerContext ctx) throws Exception;
	
	 /**
     * Activated when exception occurs and each pipeline handler is notified with the exception
     * @param ctx
     * @param msg
     * @return
     */
	void exceptionCaught(ChannelHandlerContext ctx, Throwable msg) throws Exception;
	
	/**
	 * 
	 */
	void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception;

}
