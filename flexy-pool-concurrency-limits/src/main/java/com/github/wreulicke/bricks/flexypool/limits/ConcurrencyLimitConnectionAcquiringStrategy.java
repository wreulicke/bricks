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
package com.github.wreulicke.bricks.flexypool.limits;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import javax.sql.DataSource;

import com.netflix.concurrency.limits.Limiter;
import com.vladmihalcea.flexypool.adaptor.PoolAdapter;
import com.vladmihalcea.flexypool.common.ConfigurationProperties;
import com.vladmihalcea.flexypool.connection.ConnectionRequestContext;
import com.vladmihalcea.flexypool.exception.CantAcquireConnectionException;
import com.vladmihalcea.flexypool.metric.Metrics;
import com.vladmihalcea.flexypool.strategy.AbstractConnectionAcquiringStrategy;
import com.vladmihalcea.flexypool.strategy.ConnectionAcquiringStrategyFactory;

public class ConcurrencyLimitConnectionAcquiringStrategy<T extends DataSource> extends AbstractConnectionAcquiringStrategy {

  private final Limiter<FlexyPoolRequestContext> contextLimiter;

  /**
   * Creates a strategy using the given {@link ConfigurationProperties}
   *
   * @param configurationProperties configurationProperties
   * @param contextLimiter
   */
  protected ConcurrencyLimitConnectionAcquiringStrategy(ConfigurationProperties<? extends DataSource, Metrics, PoolAdapter> configurationProperties,
    Limiter<FlexyPoolRequestContext> contextLimiter) {
    super(configurationProperties);
    this.contextLimiter = contextLimiter;
  }



  public static class Factory<T extends DataSource> implements ConnectionAcquiringStrategyFactory<ConcurrencyLimitConnectionAcquiringStrategy, T> {

    private final Limiter<FlexyPoolRequestContext> contextLimiter;

    public Factory(Limiter<FlexyPoolRequestContext> contextLimiter) {
      this.contextLimiter = contextLimiter;
    }

    @Override
    public ConcurrencyLimitConnectionAcquiringStrategy newInstance(ConfigurationProperties<T, Metrics, PoolAdapter<T>> configurationProperties) {
      return new ConcurrencyLimitConnectionAcquiringStrategy(configurationProperties, contextLimiter);
    }
  }

  @Override
  public Connection getConnection(ConnectionRequestContext requestContext) throws SQLException {
    Optional<Limiter.Listener> listenerOpt = contextLimiter.acquire(new FlexyPoolRequestContextImpl(requestContext.getCredentials()
      .getUsername()));
    if (listenerOpt.isPresent()) {
      Limiter.Listener listener = listenerOpt.get();
      try {
        Connection connection = getConnection(requestContext);
        listener.onSuccess();
        return connection;
      } catch (SQLException e) {
        listener.onIgnore();
        throw e;
      }
    }
    throw new CantAcquireConnectionException("Concurrency limit exceeded");
  }
}
