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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.ServerSocket;
import java.sql.SQLException;

import org.springframework.util.SocketUtils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vladmihalcea.flexypool.FlexyPoolDataSource;
import com.vladmihalcea.flexypool.adaptor.HikariCPPoolAdapter;
import com.vladmihalcea.flexypool.config.Configuration;
import com.zaxxer.hikari.HikariDataSource;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;

class CircuitBreakerConnectionAcquiringStrategyTest {

  private ServerSocket serverSocket;

  @BeforeEach
  public void setUp() throws IOException {
    int port = SocketUtils.findAvailableTcpPort();
    this.serverSocket = new ServerSocket(port);
  }

  @AfterEach
  public void tearDown() {}

  @Test
  public void test() throws SQLException {
    CircuitBreaker test = CircuitBreaker.of("test", CircuitBreakerConfig.custom()
      .failureRateThreshold(3)
      .ringBufferSizeInClosedState(10)
      .ringBufferSizeInHalfOpenState(1)
      .build());
    CircuitBreakerConnectionAcquiringStrategy.Factory<HikariDataSource> dataSourceFactory = new CircuitBreakerConnectionAcquiringStrategy.Factory<>(
      test);
    HikariDataSource dataSource = new HikariDataSource();
    dataSource.setJdbcUrl("jdbc:mysql://localhost:" + serverSocket.getLocalPort() + "/test");
    dataSource.setDriverClassName(com.mysql.cj.jdbc.Driver.class.getName());
    dataSource.setConnectionTimeout(250);
    Configuration<HikariDataSource> configuration = new Configuration.Builder<>("test", dataSource, HikariCPPoolAdapter.FACTORY).build();
    FlexyPoolDataSource<HikariDataSource> flexyPoolDataSource = new FlexyPoolDataSource<>(configuration, dataSourceFactory);

    getConnection(flexyPoolDataSource);
    getConnection(flexyPoolDataSource);
    getConnection(flexyPoolDataSource);
    getConnection(flexyPoolDataSource);
    getConnection(flexyPoolDataSource);
    getConnection(flexyPoolDataSource);
    getConnection(flexyPoolDataSource);
    getConnection(flexyPoolDataSource);
    getConnection(flexyPoolDataSource);
    getConnection(flexyPoolDataSource);
    assertThatThrownBy(() -> getConnection(flexyPoolDataSource)).isInstanceOf(CircuitBreakerOpenException.class);

  }

  private void getConnection(FlexyPoolDataSource<HikariDataSource> flexyPoolDataSource) {
    try {
      flexyPoolDataSource.getConnection();
    } catch (CircuitBreakerOpenException e) {
      throw e;
    } catch (Exception e) {
    }
  }

}
