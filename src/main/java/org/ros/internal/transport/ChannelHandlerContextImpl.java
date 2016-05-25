package org.ros.internal.transport;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * A handler context contains all the executor, the channel group, the channel, and the pipeline with the handlers.
 * There is one channel per context.
 * There is one pipeline context.
 * There is one executor per group of contexts.
 * The pipeline is a stateful collection of handlers that represent the current channel state and means
 * of executing functions in the process of connecting, disconnecting, reading, failing etc.
 * The pipeline is configured by means of factories that create ChannelInitializers, inserting
 * them in order in the pipeline deque.
 * The functions of the system move data through the pipeline, triggering the handlers in the sequence they were
 * added.
 * @author jg
 *
 */
public class ChannelHandlerContextImpl implements ChannelHandlerContext {
	AsynchronousChannelGroup channelGroup;
	Executor executor;
	AsynchronousSocketChannel channel;
	ChannelPipeline pipeline;
	boolean ready = false;
	Object mutex = new Object();
	
	
	public ChannelHandlerContextImpl(AsynchronousChannelGroup grp, AsynchronousSocketChannel ch, Executor exc) {
		channelGroup = grp;
		channel = ch;
		executor = exc;
		pipeline = new ChannelPipelineImpl(this);
	}
	
	public void setChannel(AsynchronousSocketChannel sock) {
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
	public AsynchronousSocketChannel bind(SocketAddress localAddress) throws IOException {
		return channel.bind(localAddress);
	}

	@Override
	public void connect(SocketAddress remoteAddress) {
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
	public Future<Integer> read(ByteBuffer buf) {
		return channel.read(buf);	
	}

	@Override
	public Future<Integer> write(Object msg) {
		return channel.write((ByteBuffer) msg);
	}

	@Override
	public void write(Object msg, CompletionHandler<Integer, Void> handler) {
		channel.write((ByteBuffer)msg, null, handler);
	}

	@Override
	public void read(ByteBuffer buf, CompletionHandler<Integer, Void> handler) {
		channel.read(buf, null, handler);
	}


	@Override
	public ChannelPipeline pipeline() {
		return pipeline;
	}

	@Override
	public AsynchronousChannelGroup getChannelGroup() {
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
	
	public void setReady(boolean ready) { this.ready = ready;}

	/**
	 * Object to synchronize read and write completion for the channel in this context, since we will have
	 * multiple outbound writers accessing the same channel
	 */
	public Object getChannelCompletionMutex() { return mutex; }
}
