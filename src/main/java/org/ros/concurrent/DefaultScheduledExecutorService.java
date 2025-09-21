package org.ros.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This wraps a {@link Executors#newVirtualThreadPerTaskExecutor()} to run a series of executing
 * {@link Executors#newSingleThreadScheduledExecutor()} to provide the functionality of
 * both in a single {@link ScheduledExecutorService}. This is necessary since
 * the {@link ScheduledExecutorService} uses a pool executor with an unbounded queue which makes it
 * impossible to create an unlimited number of threads on demand.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017,2021,2025
 */
public class DefaultScheduledExecutorService implements ScheduledExecutorService {

  private final ExecutorService executorService;
  
  public DefaultScheduledExecutorService() {
    this(Executors.newVirtualThreadPerTaskExecutor());
  }

  /**
   * This instance will take over the lifecycle of the services.
   * 
   * @param executorService
   */
  public DefaultScheduledExecutorService(ExecutorService executorService) {
    this.executorService = executorService;
  }

  @Override
  public void shutdown() {
    executorService.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    List<Runnable> combined = new ArrayList<Runnable>();
    combined.addAll(executorService.shutdownNow());
    return combined;
  }

  @Override
  public boolean isShutdown() {
    return executorService.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return executorService.isTerminated();
  }

  /**
   * First calls {@link #awaitTermination(long, TimeUnit)} on the wrapped
   * {@link ExecutorService} and then {@link #awaitTermination(long, TimeUnit)}
   * on the wrapped {@link ScheduledExecutorService}.
   * 
   * @return {@code true} if both {@link Executor}s terminated, {@code false}
   *         otherwise
   */
  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    boolean executorServiceResult = executorService.awaitTermination(timeout, unit);
    return executorServiceResult;
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    return executorService.submit(task);
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    return executorService.submit(task, result);
  }

  @Override
  public Future<?> submit(Runnable task) {
    return executorService.submit(task);
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
      throws InterruptedException {
    return executorService.invokeAll(tasks);
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
      TimeUnit unit) throws InterruptedException {
    return executorService.invokeAll(tasks, timeout, unit);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException,
      ExecutionException {
    return executorService.invokeAny(tasks);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return executorService.invokeAny(tasks, timeout, unit);
  }

  @Override
  public void execute(Runnable command) {
    executorService.execute(command);
  }
  /**
   * Invoke the single threaded executor within a virtual threads context. We do this
   * because the single thread executor creates a series of sequential threads
   * @param command The Runnable to schedule
   * @param delay the delay to start
   * @param unit The TimeUnit
   * @return the Scheduled Future of the task
   */
  private ScheduledFuture<?> invokeSchedulerWithinVirtualThread(Runnable command, long delay, TimeUnit unit) {
	  final ReturnRunnable ret = new ReturnRunnable();
	  executorService.execute(() -> {
		  ScheduledExecutorService singleThreadScheduler = Executors.newSingleThreadScheduledExecutor();
		  try (singleThreadScheduler) {
			  ret.setReturnRunnable(singleThreadScheduler.schedule(command, delay, unit));
		  }
	  });
	  return ret.getReturnRunnable();
  }
  /**
   * Invoke the single threaded executor within a virtual threads context. We do this
   * because the single thread executor creates a series of sequential threads
   * @param command The Runnable to schedule
   * @param initDelay the initial delay
   * @param period the interval
   * @param unit The TimeUnit
   * @return the Scheduled Future of the task
   */
  private ScheduledFuture<?> invokeSchedulerWithinVirtualThread(Runnable command, long initDelay, long period, TimeUnit unit) {
	  final ReturnRunnable ret = new ReturnRunnable();
	  executorService.execute(() -> {
		  ScheduledExecutorService singleThreadScheduler = Executors.newSingleThreadScheduledExecutor();
		  try (singleThreadScheduler) {
			  ret.setReturnRunnable(singleThreadScheduler.scheduleAtFixedRate(command, initDelay, period, unit));
		  }
	  });
	  return ret.getReturnRunnable();
  }
  /**
   * Invoke the single threaded executor within a virtual threads context. We do this
   * because the single thread executor creates a series of sequential threads
   * @param command The Runnable to schedule
   * @param initDelay the initial delay
   * @param delay the delay to start
   * @param unit The TimeUnit
   * @return the Scheduled Future of the task
   */
  private ScheduledFuture<?> invokeSchedulerWithinVirtualThreadFixedDelay(Runnable command, long initDelay, long delay, TimeUnit unit) {
	  final ReturnRunnable ret = new ReturnRunnable();
	  executorService.execute(() -> {
		  ScheduledExecutorService singleThreadScheduler = Executors.newSingleThreadScheduledExecutor();
		  try (singleThreadScheduler) {
			  ret.setReturnRunnable(singleThreadScheduler.scheduleWithFixedDelay(command, initDelay, delay, unit));
		  }
	  });
	  return ret.getReturnRunnable();
  }
  /**
   * Invoke the single threaded executor within a virtual threads context. We do this
   * because the single thread executor creates a series of sequential threads
   * @param command The Callable to schedule
   * @param delay the delay to start
   * @param unit The TimeUnit
   * @return the Scheduled Future of the task
   */
  private <V> ScheduledFuture<V> invokeSchedulerWithinVirtualThread(Callable<V> command, long delay, TimeUnit unit) {
	  final ReturnCallable<V> ret = new ReturnCallable<V>();
	  executorService.execute(() -> {
		  ScheduledExecutorService singleThreadScheduler = Executors.newSingleThreadScheduledExecutor();
		  try (singleThreadScheduler) {
			  ret.setReturnCallable(singleThreadScheduler.schedule(command, delay, unit));
		  }
	  });
	  return ret.getReturnCallable();
  }
  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    //return scheduledExecutorService.schedule(command, delay, unit);
	  return invokeSchedulerWithinVirtualThread(command, delay, unit);
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    //return scheduledExecutorService.schedule(callable, delay, unit);
	  return invokeSchedulerWithinVirtualThread(callable, delay, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
    //return scheduledExecutorService.scheduleAtFixedRate(command, initialDelay, period, unit);
	  return invokeSchedulerWithinVirtualThread(command, initialDelay, period, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
    //return scheduledExecutorService.scheduleWithFixedDelay(command, initialDelay, delay, unit);
	  return invokeSchedulerWithinVirtualThreadFixedDelay(command, initialDelay, delay, unit);
  }
  
  class ReturnCallable<V> {
	  ScheduledFuture<V> returnCallable;
	  public ReturnCallable() { }
	  public void setReturnCallable(ScheduledFuture<V> scheduledFuture) {
		  this.returnCallable = scheduledFuture;
	  }
	  public ScheduledFuture<V> getReturnCallable() {
		  return returnCallable;
	  }
  }
  class ReturnRunnable {
	  ScheduledFuture<?> returnRunnable;
	  public ReturnRunnable() { }
	  public void setReturnRunnable(ScheduledFuture<?> scheduledFuture) {
		  this.returnRunnable = scheduledFuture;
	  }
	  public ScheduledFuture<?> getReturnRunnable() {
		  return returnRunnable;
	  }
  }
}
