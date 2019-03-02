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

import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DecoratedCall<T> implements Call<T> {

  private final Call<T> call;

  public DecoratedCall(Call<T> call) {
    this.call = call;
  }

  @Override
  public Response<T> execute() throws IOException {
    return this.call.execute();
  }

  @Override
  public void enqueue(Callback<T> callback) {
    call.enqueue(callback);
  }

  @Override
  public boolean isExecuted() {
    return call.isExecuted();
  }

  @Override
  public void cancel() {
    call.cancel();
  }

  @Override
  public boolean isCanceled() {
    return call.isCanceled();
  }

  @Override
  public Call<T> clone() {
    return call.clone();
  }

  @Override
  public Request request() {
    return call.request();
  }
}
