/**
 * MIT License
 *
 * Copyright (c) 2019 Wreulicke
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.wreulicke.bricks.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.netflix.concurrency.limits.Limiter;

public class ConcurrencyLimitsExecutorService implements ExecutorService {

  private final Limiter<ConcurrentContext> limiter;

  private final ExecutorService delegate;

  public ConcurrencyLimitsExecutorService(Limiter<ConcurrentContext> limiter, ExecutorService executorService) {
    this.limiter = limiter;
    this.delegate = executorService;
  }

  private Limiter.Listener acquireListener() {
    Optional<Limiter.Listener> listenerOpt = limiter.acquire(new ConcurrentContext());

    if (!listenerOpt.isPresent()) {
      throw new RejectedExecutionException("Concurrency limit context exceeded");
    }

    Limiter.Listener listener = listenerOpt.get();
    return listener;
  }

  private Runnable decorate(Limiter.Listener listener, Runnable runnable) {
    return () -> {
      try {
        runnable.run();
        listener.onSuccess();
      } catch (Exception e) {
        listener.onIgnore();
        throw e;
      }
    };
  }

  private <T> Callable<T> decorate(Limiter.Listener listener, Callable<T> task) {
    return () -> {
      try {
        T result = task.call();
        listener.onSuccess();
        return result;
      } catch (Exception e) {
        listener.onIgnore();
        throw e;
      }
    };
  }

  @Override
  public void execute(Runnable command) {
    Limiter.Listener listener = acquireListener();

    try {
      delegate.execute(decorate(listener, command));
    } catch (RejectedExecutionException e) {
      listener.onDropped();
    }

  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    Limiter.Listener listener = acquireListener();

    try {
      return delegate.submit(decorate(listener, task));
    } catch (RejectedExecutionException e) {
      listener.onDropped();
      throw e;
    }
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    Limiter.Listener listener = acquireListener();

    try {
      return delegate.submit(decorate(listener, task), result);
    } catch (RejectedExecutionException e) {
      listener.onDropped();
      throw e;
    }
  }

  @Override
  public Future<?> submit(Runnable task) {
    Limiter.Listener listener = acquireListener();

    try {
      return delegate.submit(decorate(listener, task));
    } catch (RejectedExecutionException e) {
      listener.onDropped();
      throw e;
    }
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
    TaskWithListener<Callable<T>> taskWithListeners = tasks.stream()
      .reduce(new TaskWithListener<>(), (taskWithListener, o) -> {
        Limiter.Listener listener = acquireListener();
        return taskWithListener.add(listener, decorate(listener, o));
      }, TaskWithListener::merge);

    try {
      return delegate.invokeAll(taskWithListeners.tasks);
    } catch (InterruptedException e) {
      taskWithListeners.listeners.forEach(Limiter.Listener::onIgnore);
      throw e;
    } catch (RejectedExecutionException e) {
      taskWithListeners.listeners.forEach(Limiter.Listener::onDropped);
      throw e;
    }
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
    TaskWithListener<Callable<T>> taskWithListeners = tasks.stream()
      .reduce(new TaskWithListener<>(), (taskWithListener, o) -> {
        Limiter.Listener listener = acquireListener();
        return taskWithListener.add(listener, decorate(listener, o));
      }, TaskWithListener::merge);

    try {
      return delegate.invokeAll(taskWithListeners.tasks, timeout, unit);
    } catch (InterruptedException e) {
      taskWithListeners.listeners.forEach(Limiter.Listener::onIgnore);
      throw e;
    } catch (RejectedExecutionException e) {
      taskWithListeners.listeners.forEach(Limiter.Listener::onDropped);
      throw e;
    }
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
    TimeoutException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void shutdown() {
    delegate.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    return delegate.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return delegate.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return delegate.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return delegate.awaitTermination(timeout, unit);
  }

  private static class TaskWithListener<T> {

    final List<Limiter.Listener> listeners;

    final List<T> tasks;

    TaskWithListener() {
      listeners = new ArrayList<>();
      tasks = new ArrayList<>();
    }

    TaskWithListener(List<Limiter.Listener> listeners, List<T> tasks) {
      this.listeners = listeners;
      this.tasks = tasks;
    }

    public TaskWithListener<T> add(Limiter.Listener listener, T task) {
      List<Limiter.Listener> listeners = new ArrayList<>(this.listeners);
      listeners.add(listener);
      List<T> tasks = new ArrayList<>(this.tasks);
      tasks.add(task);
      return new TaskWithListener<>(listeners, tasks);
    }

    public TaskWithListener<T> merge(TaskWithListener<T> other) {
      List<Limiter.Listener> listeners = new ArrayList<>(this.listeners);
      List<T> tasks = new ArrayList<>(this.tasks);
      listeners.addAll(other.listeners);
      tasks.addAll(other.tasks);
      return new TaskWithListener<>(listeners, tasks);
    }
  }
}
