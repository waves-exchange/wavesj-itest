package com.tradisys.commons.waves.itest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssertionsExt {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssertionsExt.class);

    public static <T extends Throwable> void assertThrows(Class<T> expectedType, Executable executable, String expectedErrorMsg) {
        T e = Assertions.assertThrows(expectedType, executable);
        if (expectedErrorMsg != null) {
            String actualErrorMsg = e.getMessage();
            boolean contains = actualErrorMsg.contains(expectedErrorMsg);
            if (!contains) {
                LOGGER.error("Actual exception message differs from expected:\n\tactualMsg={}\n\texpectedMsg={}",
                        actualErrorMsg, expectedErrorMsg);
                Assertions.fail();
            }
        }
    }
}
