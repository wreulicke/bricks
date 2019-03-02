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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Predicate;

import com.netflix.concurrency.limits.Limiter;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

public class ReactiveResillience4jCallAdapter extends CallAdapter.Factory {

  private final Limiter<RetrofitContext> limiter;

  private final Predicate<Response> successResponse;

  private final RxJava2CallAdapterFactory delegates;

  public static ReactiveResillience4jCallAdapter of(final Limiter<RetrofitContext> limiter, final RxJava2CallAdapterFactory delegates) {
    return of(limiter, Response::isSuccessful, delegates);
  }

  public static ReactiveResillience4jCallAdapter of(final Limiter<RetrofitContext> limiter, final Predicate<Response> successResponse,
    RxJava2CallAdapterFactory delegates) {
    return new ReactiveResillience4jCallAdapter(limiter, successResponse, delegates);
  }

  private ReactiveResillience4jCallAdapter(final Limiter<RetrofitContext> limiter, final Predicate<Response> successResponse,
    final RxJava2CallAdapterFactory delegates) {
    this.limiter = limiter;
    this.successResponse = successResponse;
    this.delegates = delegates;
  }

  @Override
  public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
    CallAdapter<?, ?> callAdapter = delegates.get(returnType, annotations, retrofit);
    if (callAdapter != null) {
      return new DecorateCallAdaptor<>(callAdapter, limiter, successResponse);
    }
    return null;
  }

  public static class DecorateCallAdaptor<R, T> implements CallAdapter<R, T> {

    private final CallAdapter<R, T> callAdapter;

    private final Limiter<RetrofitContext> limiter;

    private final Predicate<Response> successResponse;

    public DecorateCallAdaptor(CallAdapter<R, T> callAdapter, Limiter<RetrofitContext> limiter, Predicate<Response> successResponse) {
      this.callAdapter = callAdapter;
      this.limiter = limiter;
      this.successResponse = successResponse;
    }

    @Override
    public Type responseType() {
      return callAdapter.responseType();
    }

    @Override
    public T adapt(Call<R> call) {
      return callAdapter.adapt(new DecoratedCall<R>(call) {

        @Override
        public void enqueue(Callback<R> callback) {
          Optional<Limiter.Listener> listenerOptional = limiter.acquire(new RetrofitContext(call.request()));
          if (!listenerOptional.isPresent()) {
            callback.onFailure(call, new LimitExceededException("Concurrent limit exceeded"));
            return;
          }

          Limiter.Listener listener = listenerOptional.get();
          call.enqueue(new Callback<R>() {

            @Override
            public void onResponse(Call<R> call, Response<R> response) {
              if (successResponse.test(response)) {
                listener.onSuccess();
              }
              else if (response.code() == 503) {
                listener.onDropped();
              }
              else {
                listener.onIgnore();
              }
              callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<R> call, Throwable t) {
              listener.onIgnore();
              callback.onFailure(call, t);
            }

          });
        }

        @Override
        public Response<R> execute() throws IOException {
          Optional<Limiter.Listener> listenerOptional = limiter.acquire(new RetrofitContext(call.request()));
          if (!listenerOptional.isPresent()) {
            throw new LimitExceededException("Concurrent limit exceeded");
          }

          Limiter.Listener listener = listenerOptional.get();
          try {
            final Response<R> response = call.execute();

            if (successResponse.test(response)) {
              listener.onSuccess();
            }
            else if (response.code() == 503) {
              listener.onDropped();
            }
            else {
              listener.onIgnore();
            }

            return response;
          } catch (Throwable throwable) {
            listener.onIgnore();
            throw throwable;
          }
        }
      });
    }
  }
}
