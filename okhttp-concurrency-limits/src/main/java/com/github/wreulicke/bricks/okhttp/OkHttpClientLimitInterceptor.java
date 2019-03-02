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
package com.github.wreulicke.bricks.okhttp;

import java.io.IOException;
import java.util.Optional;

import com.netflix.concurrency.limits.Limiter;

import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OkHttpClientLimitInterceptor implements Interceptor {
  private final Limiter<OkhttpClientRequestContext> contextLimiter;

  public OkHttpClientLimitInterceptor(Limiter<OkhttpClientRequestContext> contextLimiter) {
    this.contextLimiter = contextLimiter;
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    OkhttpClientRequestContext context = new OkhttpClientRequestContext(chain.request());
    Optional<Limiter.Listener> listerOpt = contextLimiter.acquire(context);
    if (listerOpt.isPresent()) {
      Limiter.Listener listener = listerOpt.get();
      try {
        Response response = chain.proceed(chain.request());
        if (response.isSuccessful()) {
          listener.onSuccess();
        }
        else if (response.code() == 503) {
          listener.onDropped();
        }
        else {
          listener.onIgnore();
        }

        return response;
      } catch (IOException e) {
        listener.onIgnore();
        throw e;
      }
    }
    else {
      return new Response.Builder().code(503)
        .protocol(Protocol.HTTP_1_1) // dummy
        .request(chain.request())
        .message("Client concurrency limit reached")
        .body(ResponseBody.create(null, new byte[0]))
        .build();
    }
  }
}
