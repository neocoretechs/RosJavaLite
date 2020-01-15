package org.ros.internal.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.CompletionHandler;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
	private static final boolean DEBUG = false;
	private static final Log log = LogFactory.getLog(ChannelHandlerContextImpl.class);
	/*Asynchronous*/ChannelGroup channelGroup;
	/*Asynchronous*/Socket/*Channel*/ channel;
	ChannelPipeline pipeline;
	boolean ready = false;
	Object mutex = new Object();
	Set<String> outboundMessageTypes;
	InputStream is = null;
	OutputStream os = null;

	public ChannelHandlerContextImpl(/*Asynchronous*/ChannelGroup channelGroup2, /*Asynchronous*/Socket channel2) {
		channelGroup = channelGroup2;
		channel = channel2;
		pipeline = new ChannelPipelineImpl(this);
		outboundMessageTypes = (Set<String>) new HashSet<String>();
	}
	
	public void setChannel(/*Asynchronous*/Socket/*Channel*/ sock) {
		this.channel = sock;
	}
		
	@Override
	public Executor executor() {
		return channelGroup.getExecutorService();
	}

	@Override
	public String name() {
		return "ChannelHandlerContext";
	}


	@Override
	public /*AsynchronousChannel*/void  bind(SocketAddress localAddress) throws IOException {
		/*return*/ channel.bind(localAddress);
	}

	@Override
	public void connect(SocketAddress remoteAddress) throws IOException {
		int try_num = 1;
		int secondsMax = 30;
		int secondsToWait = 1;
		while(secondsToWait < secondsMax) {
			try {
				channel.connect(remoteAddress);
				//is = channel.getInputStream();
				//os = channel.getOutputStream();
				return;
			} catch(Exception e) { }
			secondsToWait = (int) Math.min(secondsMax, Math.pow(2, try_num++));
			log.info("Failed to connect to remote address "+remoteAddress+", waiting "+secondsToWait+" seconds for retry...");
			try {
				Thread.sleep(secondsToWait*1000);
			} catch (InterruptedException e) {}
		}
		// Exceeded maximum wait, bomb
		throw new IOException("failed to connect to remote address "+remoteAddress+" after maximum retries");
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
	public Object read() throws IOException {
		is = channel.getInputStream();
		ObjectInputStream ois = new ObjectInputStream(is);
		try {
			return ois.readObject();
		} catch (ClassNotFoundException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void write(Object msg) throws IOException {
		os = channel.getOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(os);
		oos.writeObject(msg);
		oos.flush();
	}

	@Override
	public void write(Object msg, CompletionHandler<Integer, Void> handler) {
		try {
			write(msg);
			handler.completed(0, null);
		} catch (IOException e) {
			handler.failed(e, null);
		}
	}

	@Override
	public Object read(CompletionHandler<Integer, Void> handler) {
		try {
			Object o = read();
			handler.completed(0, null);
			return o;
		} catch (IOException e) {
			handler.failed(e, null);
		}
		return null;
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
	public Socket channel() {
		return channel;
	}

	@Override
	public String toString() {
		return new String("ChannelHandlerContext:"+channel+" ChannelGroup:"+channelGroup+" ChannelPipeline:"+pipeline+" ready:"+ready);
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
