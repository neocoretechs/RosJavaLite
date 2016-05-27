package org.ros.internal.transport.tcp;

import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.ReadPendingException;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.internal.message.MessageBufferPool;
import org.ros.internal.message.MessageBuffers;
import org.ros.internal.system.Utility;
import org.ros.internal.transport.ChannelHandlerContext;

/**
 * This AsynchTCPWorker is spawned for servicing traffic for publisher during handshake.
 * After handshake we dont need to get trqffic FROM subscribers, only send it to them.
 * Due to that we need a worker that does not fiddle with sockets and try to close them if this thread
 * is terminated. The thread is designed to be terminated after handshake and replaced with
 * the outbound message processor.
 * It functions as a temporary worker for the {@link TcpRosServer} 
 * as well as the {@link TcpClient} to handle read traffic, in which case a permanent version is used
 * @author jg
 * Copyright (C) NeoCoreTechs 2016
 *
 */
public class AsynchTempTCPWorker implements Runnable {
	private static final boolean DEBUG = true;
	private static final Log log = LogFactory.getLog(AsynchTempTCPWorker.class);
	public boolean shouldRun = true;
	private ChannelHandlerContext ctx;
	private Object waitHalt = new Object(); 
	//private MessageBufferPool pool = new MessageBufferPool();
	
    public AsynchTempTCPWorker(ChannelHandlerContext ctx) throws IOException {
    	this.ctx = ctx;
    	if( DEBUG )
    		log.info("AsynchTempTCPWorker constructed with context:"+ctx);
	}
	
	/**
	 * Client (Slave port) sends data to our master in the following loop
	 */
	@Override
	public void run() {
		//final Object waitFinish = ctx.getChannelCompletionMutex();
			try {
				//while(shouldRun) {
					//final ByteBuffer buf = MessageBuffers.dynamicBuffer();//pool.acquire();
					// initiate asynch read
					// If we get a read pending exception, try again
				   	final ByteBuffer buf = MessageBuffers.dynamicBuffer();//pool.acquire();
					while(true) {
						try {
							buf.clear();
							ctx.read(buf, new CompletionHandler<Integer, Void>() {
								@Override
								public void completed(Integer arg0, Void arg1) {
									buf.flip();
									if( DEBUG )
										log.info("ROS AsynchTempTCPWorker COMPLETED READ for "+ctx+" command received:"+buf);
									Object res = Utility.deserialize(buf);
									try {
										ctx.pipeline().fireChannelRead(res);
									} catch (Exception e) {
										if( DEBUG) {
											log.info("Exception out of fireChannelRead",e);
											e.printStackTrace();
										}
									}
							
								}
								@Override
								public void failed(Throwable arg0, Void arg1) {
									if( DEBUG ){
										log.info("AsynchTempTcpWorker read op failed:",arg0);
										arg0.printStackTrace();
									}
									try {
										ctx.pipeline().fireExceptionCaught(arg0);
									} catch (Exception e) {
										e.printStackTrace();
									}
									if( arg0 instanceof ClosedChannelException ) {
										shouldRun = false;
									}
								} 	
							});
						} catch(ReadPendingException rpe) { Thread.sleep(1); continue; }
						break;
					}// while
				//} // shouldRun
				
			} catch(Exception se) {
				if( se instanceof SocketException ) {
					log.error("Received SocketException, connection reset..");
				} else {
					log.error("Remote invocation failure ",se);
				}
			}
			synchronized(waitHalt) {
				waitHalt.notify();
			}

	}
	
	public void close() {
		synchronized(waitHalt) {
			try {
				shouldRun = false;
				waitHalt.wait();
			} catch (InterruptedException ie) {}
		}
	}

}
