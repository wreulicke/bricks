package com.github.wreulicke.bricks.junit.jupiter;

import com.google.auto.service.AutoService;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.MDC;

import java.util.Optional;

@AutoService(Extension.class)
public class MdcExtension implements BeforeEachCallback, AfterEachCallback {

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		final Optional<String> classNameOpt = context.getTestClass()
				.map(Class::getName);
		classNameOpt.ifPresent(className -> MDC.put("test.class", className));
		if (!classNameOpt.isPresent()) {
			context.getTestInstance().ifPresent(instance -> MDC.put("test.class", instance.getClass().getName()));
		}
		MDC.put("test.displayName", context.getDisplayName());
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		MDC.remove("test.displayName");
		MDC.remove("test.class");
	}
}