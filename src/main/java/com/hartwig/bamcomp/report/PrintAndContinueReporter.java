package com.hartwig.bamcomp.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrintAndContinueReporter implements DifferenceReporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrintAndContinueReporter.class);
    private boolean wasInvoked = false;

    @Override
    public synchronized void report(final String message) {
        wasInvoked = true;
        LOGGER.error(message);
    }

    @Override
    public synchronized boolean hadErrors() {
        return wasInvoked;
    }
}
