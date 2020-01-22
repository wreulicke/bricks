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
package com.github.wreulicke.bricks.junit.jupiter;

import com.google.auto.service.AutoService;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.MDC;

import java.util.Optional;
import java.util.UUID;

@AutoService(Extension.class)
public class MdcExtension implements BeforeEachCallback, AfterEachCallback {

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    final Optional<String> classNameOpt = context.getTestClass()
      .map(Class::getName);
    classNameOpt.ifPresent(className -> MDC.put("test.class", className));
    if (!classNameOpt.isPresent()) {
      context.getTestInstance()
        .ifPresent(instance -> MDC.put("test.class", instance.getClass()
          .getName()));
    }
    MDC.put("test.displayName", context.getDisplayName());
    MDC.put("test.id", UUID.randomUUID().toString());
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    MDC.remove("test.displayName");
    MDC.remove("test.class");
    MDC.remove("test.id");
  }
}
