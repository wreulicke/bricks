/**
 * MIT License
 *
 * Copyright (c) 2017 Wreulicke
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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.Connection;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class EnhancedLoggingInterceptor implements Interceptor {

  private static final Logger log = LoggerFactory.getLogger(EnhancedLoggingInterceptor.class);

  private final List<String> headerNameForDump;

  public EnhancedLoggingInterceptor(String... headerNameForDump) {
    this.headerNameForDump = Arrays.asList(headerNameForDump);
  }

  public EnhancedLoggingInterceptor(List<String> headerNameForDump) {
    this.headerNameForDump = headerNameForDump;
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();

    RequestBody requestBody = request.body();
    boolean hasRequestBody = requestBody != null;

    Connection connection = chain.connection();
    log.info("--> {} {}{}{}", request.method(), request.url(), connection != null ? " " + connection.protocol() : "", hasRequestBody ? " (" +
      requestBody.contentLength() + "-byte body)" : "");

    long startTime = System.nanoTime();
    Response response;
    try {
      response = chain.proceed(request);
    } catch (Exception e) { // NOPMD
      log.warn("<-- HTTP FAILED", e);
      throw e;
    }

    long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
    ResponseBody responseBody = response.body();
    long contentLength = responseBody.contentLength();
    String bodySize = contentLength != -1 ? contentLength + "-byte" : "unknown-length";

    log.info("<-- {} {} {} ({} ms, {} body" + requestTraceMessage(response) + ")", response.code(), response.message(), response.request()
      .url(), tookMs, bodySize);
    return response;
  }

  private String requestTraceMessage(Response response) {
    return headerNameForDump.stream()
      .flatMap(name -> {
        String value = response.header(name);
        if (value == null) {
          return Stream.empty();
        }
        else {
          return Stream.of(", ", name, ": ", value);
        }
      })
      .collect(Collectors.joining());
  }
}
