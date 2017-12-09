package org.ros.internal.transport;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;

import java.nio.channels.Channel;
import java.nio.channels.CompletionHandler;

import java.util.Set;
import java.util.concurrent.Executor;

import org.ros.internal.transport.tcp.ChannelGroup;

/**
 * This is the ChannelHandlerContext that links the underlying TCP Socket 'channel' to the {@link ChannelPipeline} and the {@link ChannelGroup}
 * and provides access to the {@link Executor} to spin up event dispatcher.
 * @author jg (C) NeoCoreTechs 2017
 *
 */
public interface ChannelHandlerContext {
	 /**
     * Return the {@link Socket} which is bound to the {@link ChannelHandlerContext}.
     */
    Socket channel();

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
     * Request to bind to the given {@link SocketAddress} 
     * <p>
     * This will result in having the socket bound to the local address.
     */
    void bind(SocketAddress localAddress) throws IOException;

    /**
     * Request to connect to the given {@link SocketAddress}.
     * <p>
     * If the connection fails because of a connection timeout, the exception will be thrown
     * This will result in having the socket connected and the input and output streams initialized.
     * @throws IOException 
     */
    void connect(SocketAddress remoteAddress) throws IOException;

    /**
     * Request to connect to the given remote {@link SocketAddress} while bind to the localAddress.
     * This will result in having the socket bound and streams ready.
     * @throws IOException 
     */
    void connect(SocketAddress remoteAddress, SocketAddress localAddress) throws IOException;
    
    /**
     * Request to disconnect from the remote peer.
     * This will result in having the Socket closed.
     * @throws IOException 
     */
    void disconnect() throws IOException;

    /**
     * Request to close the {@link Channel}.
     * After it is closed it is not possible to reuse it again.
     * This will result in having the {@link Socket} closed.
     * @throws IOException 
     */
    void close() throws IOException;

    /**
     * Request to Read data from the {@link InputStream} into the first inbound buffer.
     * It is up to the client, i.e. {@link AsynchTCPWorker}, to trigger a read event if data was
     * read, and it does this through the pipeline {@link ChannelPipeline#fireChannelRead(Object)} and 
     * triggers an event through the pipeline via the {@link ChannelPipeline#fireChannelReadComplete()}
     * if successful.  If there's a pending read operation already, this method does nothing.
     * @throws IOException Generates {@link ChannelPipeline#fireExceptionCaught(Throwable)}
     */
	Object read() throws IOException;

    /**
     * Request to write a message via this {@link ChannelHandlerContext} through the {@link ChannelPipeline}.
     * @throws IOException 
     */
    void write(Object msg) throws IOException;

    /**
     * Return the assigned {@link ChannelPipeline}
     */
    ChannelPipeline pipeline();
    
    /**
     * Return the channel group
     * 
     */
    /*Asynchronous*/ChannelGroup getChannelGroup();

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

    /**
     * Write with the named {@link CompletionHandler}
     * @param msg
     * @param handler
     */
	void write(Object msg, CompletionHandler<Integer, Void> handler);

	  /**
     * Request to Read data from the {@link InputStream} into the first inbound buffer.
     * <p>
     * This will result in having the Socket read and {@link CompletionHandler#completed(Object, Object)}
     * On IOException {@link CompletionHandler#failed(Throwable, Object)}
     */
	Object read(CompletionHandler<Integer, Void> handler);


}
