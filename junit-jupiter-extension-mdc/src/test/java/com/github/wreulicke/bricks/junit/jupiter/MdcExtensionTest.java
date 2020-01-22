package com.github.wreulicke.bricks.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class MdcExtensionTest {

	@Test
	public void test() {
		// MDCExtension is enabled by junit jupiter with Automatic Extension Detection.
		// https://junit.org/junit5/docs/current/user-guide/#extensions-registration-automatic-enabling
		// we provides `junit.jupiter.extensions.autodetection.enabled` property via junit-platform.properties
		assertThat(MDC.get("test.displayName")).isNotNull();
	}

}