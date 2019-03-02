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

import com.netflix.concurrency.limits.Limiter;
import com.netflix.concurrency.limits.limiter.AbstractPartitionedLimiter;
import com.netflix.concurrency.limits.limiter.BlockingLimiter;

public class OkHttpClientLimiterBuilder extends AbstractPartitionedLimiter.Builder<OkHttpClientLimiterBuilder, OkhttpClientRequestContext> {

  private boolean blockOnLimit = false;

  public OkHttpClientLimiterBuilder partitionByHeaderName(String headerName) {
    return partitionResolver(context -> context.request()
      .header(headerName));
  }

  public OkHttpClientLimiterBuilder partitionByHost() {
    return partitionResolver(context -> context.request()
      .url()
      .host());
  }

  public <T> OkHttpClientLimiterBuilder blockOnLimit(boolean blockOnLimit) {
    this.blockOnLimit = blockOnLimit;
    return this;
  }

  @Override
  protected OkHttpClientLimiterBuilder self() {
    return this;
  }

  public Limiter<OkhttpClientRequestContext> build() {
    Limiter<OkhttpClientRequestContext> limiter = super.build();

    if (blockOnLimit) {
      limiter = BlockingLimiter.wrap(limiter);
    }
    return limiter;
  }
}
