package org.ros.internal.node.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.concurrent.Holder;
import org.ros.concurrent.RetryingExecutorService;
import org.ros.exception.RosRuntimeException;
import org.ros.internal.node.response.Response;
import org.ros.internal.node.server.NodeIdentifier;
import org.ros.internal.node.server.SlaveServer;
import org.ros.internal.node.server.master.MasterServer;
import org.ros.internal.node.service.DefaultServiceServer;
import org.ros.internal.node.service.ServiceManagerListener;
import org.ros.internal.node.topic.DefaultPublisher;
import org.ros.internal.node.topic.DefaultSubscriber;
import org.ros.internal.node.topic.PublisherIdentifier;
import org.ros.internal.node.topic.TopicParticipantManagerListener;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages topic, and service registrations of a {@link SlaveServer} with the
 * {@link MasterServer}.<br/>
 * The primary activity here is to respond to events that occur when a publisher or subscriber is added
 * or removed. The fact that it is constructed with a MasterClient and an ExecutorService is a clue.
 * For instance, in the onSubscriberAdded event handler method, we contact the remote master to
 * register the subscriber. To do this we call updatePublishers on the subscriber
 * using the collection of publisherUris returned in the response to masterClient.<br/>
 * Passed DefaultSubscriber class creates an UpdatePublisherRunnable thread 
 * which creates a SlaveClient of type SlaveRpcEndpointImpl to
 * contact the publishers. If successful, call signalOnMasterRegistrationSuccess for the subscriber.
 * 
 * @author jg
 */
public class Registrar implements TopicParticipantManagerListener, ServiceManagerListener {

  private static final boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(Registrar.class);

  private static final int SHUTDOWN_TIMEOUT = 5;
  private static final TimeUnit SHUTDOWN_TIMEOUT_UNITS = TimeUnit.SECONDS;

  private final MasterClient masterClient;
  private final ScheduledExecutorService executorService;
  private final RetryingExecutorService retryingExecutorService;

  private NodeIdentifier nodeIdentifier;
  private boolean running;

  /**
   * @param masterClient
   *          a {@link MasterClient} for communicating with the ROS master
   * @param executorService
   *          a {@link ScheduledExecutorService} to be used for all asynchronous
   *          operations
   */
  public Registrar(MasterClient masterClient, ScheduledExecutorService executorService) {
    this.masterClient = masterClient;
    this.executorService = executorService;
    retryingExecutorService = new RetryingExecutorService(executorService);
    nodeIdentifier = null;
    running = false;
    if (DEBUG) {
      log.info("MasterRpcEndpoint Address: " + masterClient.getRemoteUri());
    }
  }

  /**
   * Failed registration actions are retried periodically until they succeed.
   * This method adjusts the delay between successive retry attempts for any
   * particular registration action.
   * 
   * @param delay
   *          the delay in units of {@code unit} between retries
   * @param unit
   *          the unit of {@code delay}
   */
  public void setRetryDelay(long delay, TimeUnit unit) {
    retryingExecutorService.setRetryDelay(delay, unit);
  }

  private boolean submit(Callable<Boolean> callable) {
    if (running) {
      retryingExecutorService.submit(callable);
      return true;
    }
    log.warn("Registrar no longer running, request ignored.");
    return false;
  }

  private <T> boolean callMaster(Callable<Response<T>> callable) {
    assert(nodeIdentifier != null) : "Registrar not started.";  
    boolean success;
    try {
      Response<T> response = callable.call();
      if (DEBUG) {
        log.info(response);
      }
      success = response.isSuccess();
    } catch (Exception e) {
      if (DEBUG) {
        log.error("Exception caught while communicating with master.", e);
      } else {
        log.error("Exception caught while communicating with master.");
      }
      success = false;
    }
    return success;
  }

  @Override
  public void onPublisherAdded(final DefaultPublisher<?> publisher) {
    if (DEBUG) {
      log.info("Registering publisher: " + publisher);
    }
    boolean submitted = submit(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        boolean success = callMaster(new Callable<Response<List<InetSocketAddress>>>() {
          @Override
          public Response<List<InetSocketAddress>> call() throws Exception {
            return masterClient.registerPublisher(publisher.toDeclaration());
          }
        });
        if (success) {
          publisher.signalOnMasterRegistrationSuccess();
        } else {
          publisher.signalOnMasterRegistrationFailure();
        }
        return !success;
      }
    });
    if (!submitted) {
      executorService.execute(new Runnable() {
        @Override
        public void run() {
          publisher.signalOnMasterRegistrationFailure();
        }
      });
    }
  }

  @Override
  public void onPublisherRemoved(final DefaultPublisher<?> publisher) {
    if (DEBUG) {
      log.info("Unregistering publisher: " + publisher);
    }
    boolean submitted = submit(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        boolean success = callMaster(new Callable<Response<Integer>>() {
          @Override
          public Response<Integer> call() throws Exception {
            return masterClient.unregisterPublisher(publisher.getIdentifier());
          }
        });
        if (success) {
          publisher.signalOnMasterUnregistrationSuccess();
        } else {
          publisher.signalOnMasterUnregistrationFailure();
        }
        return !success;
      }
    });
    if (!submitted) {
      executorService.execute(new Runnable() {
        @Override
        public void run() {
          publisher.signalOnMasterUnregistrationFailure();
        }
      });
    }
  }
  /**
   * Contact the remote master to register the subscriber. Call updatePublishers on the subscriber
   * using the collection of publisherUris returned in the response. Passed DefaultSubscriber class creates an 
   * UpdatePublisherRunnable thread which creates a SlaveClient of type SlaveRpcEndpointImpl to
   * contact the publishers. If successful, signalOnMasterRegistrationSuccess for the subscriber.
   */
  @Override
  public void onSubscriberAdded(final DefaultSubscriber<?> subscriber) {
    if (DEBUG) {
      log.info("Registering subscriber: " + subscriber);
    }
    boolean submitted = submit(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        final Holder<Response<List<InetAddress>>> holder = Holder.newEmpty();
        boolean success = callMaster(new Callable<Response<List<InetAddress>>>() {
          @Override
          public Response<List<InetAddress>> call() throws Exception {
            return holder.set(masterClient.registerSubscriber(nodeIdentifier, subscriber));
          }
        });
        if (success) {
          Collection<PublisherIdentifier> publisherIdentifiers =
              PublisherIdentifier.newCollectionFromUris((Collection<InetSocketAddress>) holder.get().getResult(),
                  subscriber.getTopicDeclaration());
          subscriber.updatePublishers(publisherIdentifiers);
          subscriber.signalOnMasterRegistrationSuccess();
        } else {
          subscriber.signalOnMasterRegistrationFailure();
        }
        return !success;
      }
    });
    if (!submitted) {
      executorService.execute(new Runnable() {
        @Override
        public void run() {
          subscriber.signalOnMasterRegistrationFailure();
        }
      });
    }
  }

  @Override
  public void onSubscriberRemoved(final DefaultSubscriber<?> subscriber) {
    if (DEBUG) {
      log.info("Unregistering subscriber: " + subscriber);
    }
    boolean submitted = submit(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        boolean success = callMaster(new Callable<Response<Integer>>() {
          @Override
          public Response<Integer> call() throws Exception {
            return masterClient.unregisterSubscriber(nodeIdentifier, subscriber);
          }
        });
        if (success) {
          subscriber.signalOnMasterUnregistrationSuccess();
        } else {
          subscriber.signalOnMasterUnregistrationFailure();
        }
        return !success;
      }
    });
    if (!submitted) {
      executorService.execute(new Runnable() {
        @Override
        public void run() {
          subscriber.signalOnMasterUnregistrationFailure();
        }
      });
    }
  }

  @Override
  public void onServiceServerAdded(final DefaultServiceServer<?, ?> serviceServer) {
    if (DEBUG) {
      log.info("Registering service: " + serviceServer);
    }
    boolean submitted = submit(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        boolean success = callMaster(new Callable<Response<Void>>() {
          @Override
          public Response<Void> call() throws Exception {
            return masterClient.registerService(nodeIdentifier, serviceServer);
          }
        });
        if (success) {
          serviceServer.signalOnMasterRegistrationSuccess();
        } else {
          serviceServer.signalOnMasterRegistrationFailure();
        }
        return !success;
      }
    });
    if (!submitted) {
      executorService.execute(new Runnable() {
        @Override
        public void run() {
          serviceServer.signalOnMasterRegistrationFailure();
        }
      });
    }
  }

  @Override
  public void onServiceServerRemoved(final DefaultServiceServer<?, ?> serviceServer) {
    if (DEBUG) {
      log.info("Unregistering service: " + serviceServer);
    }
    boolean submitted = submit(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        boolean success = callMaster(new Callable<Response<Integer>>() {
          @Override
          public Response<Integer> call() throws Exception {
            return masterClient.unregisterService(nodeIdentifier, serviceServer);
          }
        });
        if (success) {
          serviceServer.signalOnMasterUnregistrationSuccess();
        } else {
          serviceServer.signalOnMasterUnregistrationFailure();
        }
        return !success;
      }
    });
    if (!submitted) {
      executorService.execute(new Runnable() {
        @Override
        public void run() {
          serviceServer.signalOnMasterUnregistrationFailure();
        }
      });
    }
  }

  /**
   * Starts the {@link Registrar} for the {@link SlaveServer} identified by the
   * given {@link NodeIdentifier}.
   * 
   * @param nodeIdentifier
   *          the {@link NodeIdentifier} for the {@link SlaveServer} this
   *          {@link Registrar} is responsible for
   */
  public void start(NodeIdentifier nodeIdentifier) {
    assert(nodeIdentifier != null);
    assert(this.nodeIdentifier == null) : "Registrar already started.";
    this.nodeIdentifier = nodeIdentifier;
    running = true;
  }

  /**
   * Shuts down the {@link Registrar}.
   * 
   * <p>
   * No further registration requests will be accepted. All queued registration
   * jobs have up to {@link #SHUTDOWN_TIMEOUT} {@link #SHUTDOWN_TIMEOUT_UNITS}
   * to complete before being canceled.
   * 
   * <p>
   * Calling {@link #shutdown()} more than once has no effect.
   */
  public void shutdown() {
    if (!running) {
      return;
    }
    running = false;
    try {
      retryingExecutorService.shutdown(SHUTDOWN_TIMEOUT, SHUTDOWN_TIMEOUT_UNITS);
    } catch (InterruptedException e) {
      throw new RosRuntimeException(e);
    }
  }
}
