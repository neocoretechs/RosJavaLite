package org.ros.internal.node;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.Topics;
import org.ros.node.topic.Publisher;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Logger that logs to both an underlying Apache Commons Log as well as /rosout.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
class RosoutLogger implements Log {

  private final DefaultNode defaultNode;
  private final Publisher<rosgraph_msgs.Log> publisher;
  private final Log log;

  public RosoutLogger(DefaultNode defaultNode) throws IOException {
    this.defaultNode = defaultNode;
    publisher = defaultNode.newPublisher(Topics.ROSOUT, rosgraph_msgs.Log._TYPE);
    log = LogFactory.getLog(defaultNode.getName().toString());
  }

  public Publisher<rosgraph_msgs.Log> getPublisher() {
    return publisher;
  }

  private void publish(byte level, Object message, Throwable throwable) {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    throwable.printStackTrace(printWriter);
    publish(level, message.toString() + '\n' + stringWriter.toString());
  }

  private void publish(byte level, Object message) {
    rosgraph_msgs.Log logMessage = publisher.newMessage();
    logMessage.getHeader().setStamp(defaultNode.getCurrentTime());
    logMessage.setLevel(level);
    logMessage.setName(defaultNode.getName().toString());
    logMessage.setMsg(message.toString());
    // TODO(damonkohler): Should update the topics field with a list of all
    // published and subscribed topics for the node that created this logger.
    // This helps filter the rosoutconsole.
    publisher.publish(logMessage);
  }

  @Override
  public boolean isDebugEnabled() {
    return log.isDebugEnabled();
  }

  @Override
  public boolean isErrorEnabled() {
    return log.isErrorEnabled();
  }

  @Override
  public boolean isFatalEnabled() {
    return log.isFatalEnabled();
  }

  @Override
  public boolean isInfoEnabled() {
    return log.isInfoEnabled();
  }

  @Override
  public boolean isTraceEnabled() {
    return log.isTraceEnabled();
  }

  @Override
  public boolean isWarnEnabled() {
    return log.isWarnEnabled();
  }

  @Override
  public void trace(Object message) {
    log.trace(message);
    if (log.isTraceEnabled() && publisher != null) {
      publish(rosgraph_msgs.Log.DEBUG, message);
    }
  }

  @Override
  public void trace(Object message, Throwable t) {
    log.trace(message, t);
    if (log.isTraceEnabled() && publisher != null) {
      publish(rosgraph_msgs.Log.DEBUG, message, t);
    }
  }

  @Override
  public void debug(Object message) {
    log.debug(message);
    if (log.isDebugEnabled() && publisher != null) {
      publish(rosgraph_msgs.Log.DEBUG, message);
    }
  }

  @Override
  public void debug(Object message, Throwable t) {
    log.debug(message, t);
    if (log.isDebugEnabled() && publisher != null) {
      publish(rosgraph_msgs.Log.DEBUG, message, t);
    }
  }

  @Override
  public void info(Object message) {
    log.info(message);
    if (log.isInfoEnabled() && publisher != null) {
      publish(rosgraph_msgs.Log.INFO, message);
    }
  }

  @Override
  public void info(Object message, Throwable t) {
    log.info(message, t);
    if (log.isInfoEnabled() && publisher != null) {
      publish(rosgraph_msgs.Log.INFO, message, t);
    }
  }

  @Override
  public void warn(Object message) {
    log.warn(message);
    if (log.isWarnEnabled() && publisher != null) {
      publish(rosgraph_msgs.Log.WARN, message);
    }
  }

  @Override
  public void warn(Object message, Throwable t) {
    log.warn(message, t);
    if (log.isWarnEnabled() && publisher != null) {
      publish(rosgraph_msgs.Log.WARN, message, t);
    }
  }

  @Override
  public void error(Object message) {
    log.error(message);
    if (log.isErrorEnabled() && publisher != null) {
      publish(rosgraph_msgs.Log.ERROR, message);
    }
  }

  @Override
  public void error(Object message, Throwable t) {
    log.error(message, t);
    if (log.isErrorEnabled() && publisher != null) {
      publish(rosgraph_msgs.Log.ERROR, message, t);
    }
  }

  @Override
  public void fatal(Object message) {
    log.fatal(message);
    if (log.isFatalEnabled() && publisher != null) {
      publish(rosgraph_msgs.Log.FATAL, message);
    }
  }

  @Override
  public void fatal(Object message, Throwable t) {
    log.fatal(message, t);
    if (log.isFatalEnabled() && publisher != null) {
      publish(rosgraph_msgs.Log.FATAL, message, t);
    }
  }
}
