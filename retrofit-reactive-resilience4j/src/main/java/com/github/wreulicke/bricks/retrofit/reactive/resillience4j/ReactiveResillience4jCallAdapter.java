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
package com.github.wreulicke.bricks.retrofit.reactive.resillience4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Predicate;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retrofit.RetrofitCircuitBreaker;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

public class ReactiveResillience4jCallAdapter extends CallAdapter.Factory {

  private final CircuitBreaker circuitBreaker;

  private final Predicate<Response> successResponse;

  private final RxJava2CallAdapterFactory delegates;

  public static ReactiveResillience4jCallAdapter of(final CircuitBreaker circuitBreaker, RxJava2CallAdapterFactory delegates) {
    return of(circuitBreaker, Response::isSuccessful, delegates);
  }

  public static ReactiveResillience4jCallAdapter of(final CircuitBreaker circuitBreaker, final Predicate<Response> successResponse,
    RxJava2CallAdapterFactory delegates) {
    return new ReactiveResillience4jCallAdapter(circuitBreaker, successResponse, delegates);
  }

  private ReactiveResillience4jCallAdapter(final CircuitBreaker circuitBreaker, final Predicate<Response> successResponse,
    final RxJava2CallAdapterFactory delegates) {
    this.circuitBreaker = circuitBreaker;
    this.successResponse = successResponse;
    this.delegates = delegates;
  }

  @Override
  public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
    CallAdapter<?, ?> callAdapter = delegates.get(returnType, annotations, retrofit);
    if (callAdapter != null) {
      return new DecorateCallAdaptor<>(callAdapter, circuitBreaker, successResponse);
    }
    return null;
  }

  public static class DecorateCallAdaptor<R, T> implements CallAdapter<R, T> {

    private final CallAdapter<R, T> callAdapter;

    private CircuitBreaker circuitBreaker;

    private Predicate<Response> successResponse;

    public DecorateCallAdaptor(CallAdapter<R, T> callAdapter, CircuitBreaker circuitBreaker, Predicate<Response> successResponse) {
      this.callAdapter = callAdapter;
      this.circuitBreaker = circuitBreaker;
      this.successResponse = successResponse;
    }

    @Override
    public Type responseType() {
      return callAdapter.responseType();
    }

    @Override
    public T adapt(Call<R> call) {
      return callAdapter.adapt(RetrofitCircuitBreaker.decorateCall(circuitBreaker, call, successResponse));
    }
  }
}
