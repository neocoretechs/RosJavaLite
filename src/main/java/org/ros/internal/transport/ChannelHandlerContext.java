package org.ros.internal.transport;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channel;
import java.nio.channels.CompletionHandler;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

public interface ChannelHandlerContext {
	  /**
     * Return the {@link Channel} which is bound to the {@link ChannelHandlerContext}.
     */
    Channel channel();

    /**
     * The {@link Executor} that is used to dispatch the events. This can also be used to directly
     * submit tasks that get executed
     */
    Executor executor();

    /**
     * The unique name of the {@link ChannelHandlerContext}.The name was used when then {@link ChannelHandler}
     * was added to the {@link ChannelPipeline}. This name can also be used to access the registered
     * {@link ChannelHandler} from the {@link ChannelPipeline}.
     */
    String name();

    /**
     * Request to bind to the given {@link SocketAddress} and notify the {@link Future} once the operation
     * completes, either because the operation was successful or because of an error.
     * <p>
     * This will result in having the
     * {@link ChannelOutboundHandler#bind(ChannelHandlerContext, SocketAddress, CompletionHandler)} method
     * called of the next {@link ChannelHandler} contained in the  {@link ChannelPipeline} of the
     * {@link Channel}.
     */
    AsynchronousSocketChannel bind(SocketAddress localAddress) throws IOException;

    /**
     * Request to connect to the given {@link SocketAddress} and notify the {@link ChannelFuture} once the operation
     * completes, either because the operation was successful or because of an error.
     * <p>
     * If the connection fails because of a connection timeout, the {@link ChannelFuture} will get failed with
     * a {@link ConnectTimeoutException}. If it fails because of connection refused a {@link ConnectException}
     * will be used.
     * <p>
     * This will result in having the
     * {@link ChannelOutboundHandler#connect(ChannelHandlerContext, SocketAddress, SocketAddress, CompletionHandler)}
     * method called of the next {@link ChannelHandler} contained in the  {@link ChannelPipeline} of the
     * {@link Channel}.
     */
    void connect(SocketAddress remoteAddress);

    /**
     * Request to connect to the given {@link SocketAddress} while bind to the localAddress and notify the
     * {@link Future} once the operation completes, either because the operation was successful or because of
     * an error.
     * <p>
     * This will result in having the
     * {@link ChannelHandler#connect(ChannelHandlerContext, SocketAddress, SocketAddress, CompletionHandler)}
     * method called of the next {@link ChannelOutboundHandler} contained in the  {@link ChannelPipeline} of the
     * {@link Channel}.
     * @throws IOException 
     */
    void connect(SocketAddress remoteAddress, SocketAddress localAddress) throws IOException;
    


    /**
     * Request to disconnect from the remote peer and notify the {@link Future} once the operation completes,
     * either because the operation was successful or because of an error.
     * <p>
     * This will result in having the
     * {@link ChannelHandler#disconnect(ChannelHandlerContext, CompletionHandler)}
     * method called of the next {@link ChannelHandler} contained in the  {@link ChannelPipeline} of the
     * {@link Channel}.
     * @throws IOException 
     */
    void disconnect() throws IOException;

    /**
     * Request to close the {@link Channel} and notify the {@link Future} once the operation completes,
     * either because the operation was successful or because of
     * an error.
     *
     * After it is closed it is not possible to reuse it again.
     * <p>
     * This will result in having the
     * {@link ChannelHandler#close(ChannelHandlerContext, CompletionHandler)}
     * method called of the next {@link ChannelHandler} contained in the  {@link ChannelPipeline} of the
     * {@link Channel}.
     * @throws IOException 
     */
    void close() throws IOException;

    /**
     * Request to Read data from the {@link Channel} into the first inbound buffer, triggers an
     * {@link ChannelHandler#channelRead(ChannelHandlerContext, Object)} event if data was
     * read, and triggers a
     * {@link ChannelHandler#channelReadComplete(ChannelHandlerContext) channelReadComplete} event so the
     * handler can decide to continue reading.  If there's a pending read operation already, this method does nothing.
     * <p>
     * This will result in having the
     * {@link ChannelHandler#read(ChannelHandlerContext)}
     * method called of the next {@link ChannelHandler} contained in the  {@link ChannelPipeline} of the
     * {@link Channel}.
     */
    Future<Integer> read(ByteBuffer buf);
    /**
     * Request to Read data from the {@link Channel} into the first inbound buffer, triggers an
     * {@link ChannelHandler#channelRead(ChannelHandlerContext, Object)} event if data was
     * read, and triggers a
     * {@link ChannelHandler#channelReadComplete(ChannelHandlerContext) channelReadComplete} event so the
     * handler can decide to continue reading.  If there's a pending read operation already, this method does nothing.
     * <p>
     * This will result in having the
     * {@link ChannelHandler#read(ChannelHandlerContext)}
     * method called of the next {@link ChannelHandler} contained in the  {@link ChannelPipeline} of the
     * {@link Channel}.
     */
	void read(ByteBuffer buf,CompletionHandler<Integer, Void> completionHandler);

    /**
     * Request to write a message via this {@link ChannelHandlerContext} through the {@link ChannelPipeline}.
     * This method will not request to actual flush, so be sure to call {@link #flush()}
     * once you want to request to flush all pending data to the actual transport.
     */
    Future<Integer> write(Object msg);

    /**
     * Request to write a message via this {@link ChannelHandlerContext} through the {@link ChannelPipeline}.
     * This method will not request to actual flush, so be sure to call {@link #flush()}
     * once you want to request to flush all pending data to the actual transport.
     */
    void write(Object msg, CompletionHandler<Integer,Void> handler);


    /**
     * Return the assigned {@link ChannelPipeline}
     */
    ChannelPipeline pipeline();
    
    /**
     * Return the channel group
     * 
     */
    AsynchronousChannelGroup getChannelGroup();

    /**
     * Determine if this channel is ready for processing, it is configured, has a socket
     * and the communication is sound. If the socket breaks this goes false and no writes are
     * performed to this channel
     */
    boolean isReady();
    
    /**
     * Set this channel and its context ready or not for traffic.
     * @param ready
     */
    void setReady(boolean ready);

    /**
     * Get the Object representing a mutex to use for completion of operation if necessary.
     * @return
     */
    Object getChannelCompletionMutex();
    
    /**
     * Each successive handshake completion will add another message type to this synchronized set.
     * This set is used to determine whether a message placed on the outbound queue will be sent to the
     * channel in the context.
     * @return The synchronized hash set of message type strings
     */
    Set<String> getMessageTypes();

}
