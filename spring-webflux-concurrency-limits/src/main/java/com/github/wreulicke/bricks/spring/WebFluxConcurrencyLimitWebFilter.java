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
package com.github.wreulicke.bricks.spring;

import java.util.Optional;
import java.util.function.Predicate;

import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.netflix.concurrency.limits.Limiter;

import reactor.core.publisher.Mono;

public class WebFluxConcurrencyLimitWebFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

  private final Limiter<WebFluxContext> limiter;

  private final Predicate<ServerResponse> successResponse;

  public WebFluxConcurrencyLimitWebFilter(Limiter<WebFluxContext> limiter, Predicate<ServerResponse> successResponse) {
    this.limiter = limiter;
    this.successResponse = successResponse;
  }

  public static WebFluxConcurrencyLimitWebFilter of(Limiter<WebFluxContext> limiter) {
    return new WebFluxConcurrencyLimitWebFilter(limiter, serverResponse -> serverResponse.statusCode()
      .is2xxSuccessful());
  }

  public static WebFluxConcurrencyLimitWebFilter of(Limiter<WebFluxContext> limiter, Predicate<ServerResponse> successResponse) {
    return new WebFluxConcurrencyLimitWebFilter(limiter, successResponse);
  }

  @Override
  public Mono<ServerResponse> filter(ServerRequest request, HandlerFunction<ServerResponse> next) {
    Optional<Limiter.Listener> listenerOptional = limiter.acquire(new WebFluxContext(request));
    if (!listenerOptional.isPresent()) {
      return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
        .build();
    }

    Limiter.Listener listener = listenerOptional.get();

    return next.handle(request)
      .doAfterSuccessOrError(((serverResponse, throwable) -> {
        if (throwable != null) {
          listener.onIgnore();
          return;
        }
        if (successResponse.test(serverResponse)) {
          listener.onSuccess();
        }
      }));
  }
}
