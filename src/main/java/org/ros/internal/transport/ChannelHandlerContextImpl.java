package org.ros.internal.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
//import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.ros.internal.transport.tcp.ChannelGroup;

/**
 * A handler context contains all the executor, the channel group, the channel, and the pipeline with the handlers.
 * There is one channel per context.
 * There is one pipeline per context.
 * There is one executor per group of contexts.
 * The pipeline is a stateful collection of handlers that represent the current channel state and means
 * of executing functions in the process of connecting, disconnecting, reading, failing etc.
 * The pipeline is configured by means of factories that create ChannelInitializers, inserting
 * them in order in the pipeline deque.
 * The functions of the system move data through the pipeline, triggering the handlers in the sequence they were
 * added.
 * Traffic is filtered to subscriber channels via the hash set of requested message types
 * @author jg
 *
 */
public class ChannelHandlerContextImpl implements ChannelHandlerContext {
	/*Asynchronous*/ChannelGroup channelGroup;
	Executor executor;
	/*Asynchronous*/SocketChannel channel;
	ChannelPipeline pipeline;
	boolean ready = false;
	Object mutex = new Object();
	Set<String> outboundMessageTypes;
	
	
	public ChannelHandlerContextImpl(/*Asynchronous*/ChannelGroup channelGroup2, /*Asynchronous*/SocketChannel channel2, Executor exc) {
		channelGroup = channelGroup2;
		channel = channel2;
		executor = exc;
		pipeline = new ChannelPipelineImpl(this);
		outboundMessageTypes = (Set<String>) new HashSet<String>();
	}
	
	public void setChannel(/*Asynchronous*/SocketChannel sock) {
		this.channel = sock;
	}
	
	
	@Override
	public Executor executor() {
		return executor;
	}

	@Override
	public String name() {
		return "ChannelHandlerContext";
	}


	@Override
	public /*Asynchronous*/SocketChannel bind(SocketAddress localAddress) throws IOException {
		return channel.bind(localAddress);
	}

	@Override
	public void connect(SocketAddress remoteAddress) throws IOException {
		channel.connect(remoteAddress);
	}

	@Override
	public void connect(SocketAddress remoteAddress, SocketAddress localAddress) throws IOException {
		channel.bind(localAddress);
		channel.connect(remoteAddress);
	}


	@Override
	public void disconnect() throws IOException {
		channel.close();
	}

	@Override
	public void close() throws IOException {
		channel.close();
	}


	@Override
	public /*Future<Integer>*/ int read(ByteBuffer buf) throws IOException {
		return channel.read(buf);
	}

	@Override
	public /*Future<Integer>*/int write(ByteBuffer msg) throws IOException {
			return channel.write(msg);
	}

	@Override
	public void write(ByteBuffer msg, CompletionHandler<Integer, Void> handler) {
		try {
			int res = channel.write(msg/*, null, handler*/);
			handler.completed(res, null);
		} catch (IOException e) {
			handler.failed(e, null);
		}
	}

	@Override
	public void read(ByteBuffer buf, CompletionHandler<Integer, Void> handler) {
		try {
			int res = channel.read(buf/*, null, handler*/);
			handler.completed(res, null);
		} catch (IOException e) {
			handler.failed(e, null);
		}
	}


	@Override
	public ChannelPipeline pipeline() {
		return pipeline;
	}

	@Override
	public /*Asynchronous*/ChannelGroup getChannelGroup() {
		return channelGroup;
	}

	@Override
	public Channel channel() {
		return channel;
	}

	@Override
	public String toString() {
		return new String("ChannelHandlerContext:"+channel+" "+channelGroup+" "+executor+" "+pipeline+" ready:"+ready);
	}

	@Override
	public boolean isReady() {
		return ready;
	}
	
	/**
	 * Sets this context ready or not to receive traffic
	 */
	@Override
	public void setReady(boolean ready) { this.ready = ready;}

	/**
	 * Object to synchronize read and write completion for the channel in this context, since we will have
	 * multiple outbound writers accessing the same channel
	 */
	public Object getChannelCompletionMutex() { return mutex; }
	
	/**
	 * Get the type of messages we want to send to the attached subscriber, based on the handshakes
	 * received.
	 * @return The HashSet of message type as String
	 */
	public Set<String> getMessageTypes() { return outboundMessageTypes; }
	
	
}
