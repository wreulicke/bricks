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
package com.github.wreulicke.bricks.okhttp3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@ExtendWith(MockitoExtension.class)
public class EnhancedLoggingInterceptorTest {

  EnhancedLoggingInterceptor sut = new EnhancedLoggingInterceptor(Arrays.asList("X-Trace-Id"));

  @Mock
  Interceptor.Chain chain;

  @Captor
  ArgumentCaptor<ILoggingEvent> captorLoggingEvent;

  @Mock
  Appender<ILoggingEvent> mockAppender;


  @BeforeEach
  public void setUp() {
    final Logger logger = (Logger) LoggerFactory.getLogger(EnhancedLoggingInterceptor.class);
    logger.addAppender(mockAppender);
  }

  @AfterEach
  public void tearDown() {
    final Logger logger = (Logger) LoggerFactory.getLogger(EnhancedLoggingInterceptor.class);
    logger.detachAppender(mockAppender);
  }

  @Test
  public void test_success() throws IOException {
    // setup
    Request request = new Request.Builder().url("http://example.com")
      .method("GET", null)
      .build();
    when(chain.request()).thenReturn(request);
    Response response = new Response.Builder().code(200)
      .protocol(Protocol.HTTP_1_1)
      .request(request)
      .message("")
      .body(ResponseBody.create(MediaType.parse("application/json"), new byte[0]))
      .build();
    when(chain.proceed(any())).thenReturn(response);

    // exercise
    sut.intercept(chain);

    // verify
    verify(mockAppender, times(2)).doAppend(captorLoggingEvent.capture());

    assertThat(captorLoggingEvent.getAllValues()
      .get(0)
      .getFormattedMessage()).startsWith("--> GET http://example.com/");
    assertThat(captorLoggingEvent.getAllValues()
      .get(1)
      .getFormattedMessage()).startsWith("<-- 200  http://example.com/");
  }

  @Test
  public void test2() throws IOException {
    // setup
    Request request = new Request.Builder().url("http://example.com")
      .method("GET", null)
      .build();
    when(chain.request()).thenReturn(request);
    when(chain.proceed(any())).thenThrow(new IllegalStateException());

    // exercise
    assertThatThrownBy(() -> sut.intercept(chain)).isInstanceOf(IllegalStateException.class);

    // verify
    verify(mockAppender, times(2)).doAppend(captorLoggingEvent.capture());

    assertThat(captorLoggingEvent.getAllValues()
      .get(0)
      .getFormattedMessage()).startsWith("--> GET http://example.com/");
    assertThat(captorLoggingEvent.getAllValues()
      .get(1)
      .getFormattedMessage()).startsWith("<-- HTTP FAILED");
  }

  @Test
  public void test_haveTraceId() throws IOException {
    // setup
    Request request = new Request.Builder().url("http://example.com")
      .method("GET", null)
      .build();
    when(chain.request()).thenReturn(request);
    Response response = new Response.Builder().code(200)
      .protocol(Protocol.HTTP_1_1)
      .request(request)
      .message("")
      .body(ResponseBody.create(MediaType.parse("application/json"), new byte[0]))
      .header("X-Trace-Id", "TraceId")
      .build();
    when(chain.proceed(any())).thenReturn(response);

    // exercise
    sut.intercept(chain);

    // verify
    verify(mockAppender, times(2)).doAppend(captorLoggingEvent.capture());

    assertThat(captorLoggingEvent.getAllValues()
      .get(0)
      .getFormattedMessage()).startsWith("--> GET http://example.com/");
    assertThat(captorLoggingEvent.getAllValues()
      .get(1)
      .getFormattedMessage()).startsWith("<-- 200  http://example.com/")
        .contains("X-Trace-Id: TraceId");
  }


}
