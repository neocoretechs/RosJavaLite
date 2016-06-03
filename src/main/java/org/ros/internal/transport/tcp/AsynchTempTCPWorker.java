package org.ros.internal.transport.tcp;

import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.ReadPendingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.ros.internal.message.MessageBuffers;
import org.ros.internal.system.Utility;
import org.ros.internal.transport.ChannelHandlerContext;

/**
 * This AsynchTCPWorker is spawned for servicing traffic for publisher during handshake.
 * After handshake we dont need to get traffic FROM subscribers, only send it to them.
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
	private static final boolean DEBUG = false;
	private static final Log log = LogFactory.getLog(AsynchTempTCPWorker.class);
	public boolean shouldRun = true;
	private ChannelHandlerContext ctx;

	
    public AsynchTempTCPWorker(ChannelHandlerContext ctx) throws IOException {
    	this.ctx = ctx;
    	if( DEBUG )
    		log.info("AsynchTempTCPWorker constructed with context:"+ctx);
	}
	
	/**
	 * Wait for completion of the handshake via polling the context read, repeatedly if necessary.
	 * Once we have success, terminate the thread after setting the context active.
	 */
	@Override
	public void run() {
			try {
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
					// set as ready for channel write loop in OutogingMessageQueue
					ctx.setReady(true);
				
			} catch(Exception se) {
				if( se instanceof SocketException ) {
					log.error("Received SocketException, connection reset..");
				} else {
					log.error("Remote invocation failure ",se);
				}
			}


	}
	

}
