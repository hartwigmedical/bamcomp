package com.hartwig.bamcomp.report;

import org.slf4j.LoggerFactory;

public class PrintAndExitReporter implements DifferenceReporter {
    @Override
    public synchronized void report(final String message) {
        LoggerFactory.getLogger(PrintAndExitReporter.class).error(message);
        System.exit(1);
    }

    @Override
    public boolean hadErrors() {
        return false;
    }
}
