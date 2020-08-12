package com.hartwig.bamcomp.report;

public interface DifferenceReporter {
    void report(String message);
    boolean hadErrors();
}
