package org.ros.internal.transport.tcp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.internal.transport.ChannelHandlerContext;
import org.ros.internal.transport.ChannelPipeline;
/**
 * Base class to initialize the pipeline for a given context.
 * During bootup, the main initialization calls the initChannel methods for these
 * factories and they in turn register the handlers into the pipeline
 * @author jg
 *
 */
public abstract class ChannelInitializer {

    private static final Log log = LogFactory.getLog(ChannelInitializer.class);

    /**
     * This method will be called once the {@link Channel} was registered. After the method returns this instance
     * will be removed from the {@link ChannelPipeline} of the {@link Channel}.
     *
     * @param ch            the {@link Channel} which was registered.
     * @throws Exception    is thrown if an error occurs. In that case it will be handled by
     *                      {@link #exceptionCaught(ChannelHandlerContext, Throwable)} which will by default close
     *                      the {@link Channel}.
     */
    protected abstract void initChannel(ChannelHandlerContext ch) throws Exception;

    public final void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        initChannel(ctx);
        ctx.pipeline().fireChannelRegistered();
    }

    /**
     * Handle the {@link Throwable} by logging and closing the {@link Channel}. Sub-classes may override this.
     */
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.warn("Failed to initialize a channel. Closing: " + ctx.channel(), cause);
        ctx.close();
    }
}
