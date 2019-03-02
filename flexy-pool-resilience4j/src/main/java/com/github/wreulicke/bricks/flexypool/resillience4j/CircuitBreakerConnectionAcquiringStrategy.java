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
package com.github.wreulicke.bricks.flexypool.resillience4j;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.vladmihalcea.flexypool.adaptor.PoolAdapter;
import com.vladmihalcea.flexypool.common.ConfigurationProperties;
import com.vladmihalcea.flexypool.connection.ConnectionRequestContext;
import com.vladmihalcea.flexypool.metric.Metrics;
import com.vladmihalcea.flexypool.strategy.AbstractConnectionAcquiringStrategy;
import com.vladmihalcea.flexypool.strategy.ConnectionAcquiringStrategyFactory;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.utils.CircuitBreakerUtils;

public class CircuitBreakerConnectionAcquiringStrategy<T extends DataSource> extends AbstractConnectionAcquiringStrategy {

  private final CircuitBreaker circuitBreaker;

  /**
   * Creates a strategy using the given {@link ConfigurationProperties}
   *
   * @param configurationProperties configurationProperties
   * @param circuitBreaker
   */
  protected CircuitBreakerConnectionAcquiringStrategy(ConfigurationProperties<? extends DataSource, Metrics, PoolAdapter> configurationProperties,
    CircuitBreaker circuitBreaker) {
    super(configurationProperties);
    this.circuitBreaker = circuitBreaker;
  }

  public static class Factory<T extends DataSource> implements ConnectionAcquiringStrategyFactory<CircuitBreakerConnectionAcquiringStrategy, T> {

    private CircuitBreaker circuitBreaker;

    public Factory(CircuitBreaker circuitBreaker) {
      this.circuitBreaker = circuitBreaker;
    }

    /**
     * Creates a {@link com.vladmihalcea.flexypool.strategy.RetryConnectionAcquiringStrategy} for a
     * given
     * {@link ConfigurationProperties}
     *
     * @param configurationProperties configurationProperties
     * @return strategy
     */
    public CircuitBreakerConnectionAcquiringStrategy newInstance(ConfigurationProperties<T, Metrics, PoolAdapter<T>> configurationProperties) {
      return new CircuitBreakerConnectionAcquiringStrategy(configurationProperties, circuitBreaker);
    }
  }

  @Override
  public Connection getConnection(ConnectionRequestContext requestContext) throws SQLException {
    CircuitBreakerUtils.isCallPermitted(circuitBreaker);
    long start = System.nanoTime();
    try {
      Connection returnValue = getConnection(requestContext);
      long durationInNanos = System.nanoTime() - start;
      circuitBreaker.onSuccess(durationInNanos);
      return returnValue;
    } catch (SQLException throwable) {
      long durationInNanos = System.nanoTime() - start;
      circuitBreaker.onError(durationInNanos, throwable);
      throw throwable;
    }
  }
}
