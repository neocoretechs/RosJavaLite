package org.ros.internal.transport.queue;


import org.ros.concurrent.CircularBlockingDeque;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.ros.internal.transport.ChannelHandlerContext;
import org.ros.internal.transport.tcp.AbstractNamedChannelHandler;


/**
 * A type of ChannelHandler that takes channelRead events and queues them.
 * Contains the circular blocking deque shared by MessageDispatcher and managed by IncomingMessageQueue.
 * It is placed in the stack after handshake to be activated on read events.
 * @author jg (C) NeoCoretechs 2017
 * @param <T> the message type
 */
public class MessageReceiver<T> extends AbstractNamedChannelHandler {

  private static final boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(MessageReceiver.class);

  private final CircularBlockingDeque<T> lazyMessages;

  public MessageReceiver(CircularBlockingDeque<T> lazyMessages) {
    this.lazyMessages = lazyMessages;
  }

  @Override
  public String getName() {
    return "IncomingMessageQueueChannelHandler";
  }

  @Override
  public Object channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (DEBUG) {
      log.info(String.format("Received message:"+msg+" ***"+Thread.currentThread().getName()));
    }
    lazyMessages.addLast((T) msg);
    return msg;
  }


@Override
public void exceptionCaught(ChannelHandlerContext arg0, Throwable arg1) throws Exception {
	log.error(arg1);	
}


@Override
public void handlerAdded(ChannelHandlerContext arg0) throws Exception {
    if( DEBUG ) {
		  log.info(this+" handler added:"+arg0);
	}	
}

@Override
public void handlerRemoved(ChannelHandlerContext arg0) throws Exception {
	   if( DEBUG ) {
			  log.info("MessageReceiver handler removed:"+arg0);
		}
}

@Override
public void channelActive(ChannelHandlerContext arg0) throws Exception {
	   if( DEBUG ) {
			  log.info("MessageReceiver channel active:"+arg0);
		}
}

@Override
public void channelInactive(ChannelHandlerContext arg0) throws Exception {
	   if( DEBUG ) {
			  log.info("MessageReceiver channel inactive:"+arg0);
		}
}

@Override
public void channelReadComplete(ChannelHandlerContext arg0) throws Exception {
	   if( DEBUG ) {
			  log.info("MessageReceiver read complete:"+arg0);
		}
}

@Override
public void userEventTriggered(ChannelHandlerContext arg0, Object arg1) throws Exception {
	   if( DEBUG ) {
			  log.debug("MessageReceiver user event triggered:"+arg0);
		}
}


}