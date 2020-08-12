package com.hartwig.bamcomp;

import java.util.concurrent.Callable;

import com.hartwig.bamcomp.report.DifferenceReporter;
import com.hartwig.bamcomp.report.PrintAndContinueReporter;
import com.hartwig.bamcomp.report.PrintAndExitReporter;

import picocli.CommandLine;
import picocli.CommandLine.Option;

public class BamCompMain implements Callable<Integer> {
    @Option(names = {"-n", "--cores"}, description = "Number of cores to use for processing", defaultValue = "1")
    private Integer cores;

    @Option(names = {"-r", "--reference-genome"}, description = "Absolute path to reference genome", required = true)
    private String referenceGenome;

    @Option(names = {"--samtools-binary"}, description = "Absolute path to samtools executable", required = true)
    private String samtoolsPath;

    @Option(names = {"--sambamba-binary"}, description = "Absolute path to sambamba executable", required = true)
    private String sambambaPath;

    @Option(names = {"-i", "--inputs"}, arity = "2", description = "Exactly two input files", required = true)
    private String[] input;

    @Option(names = {"-a", "--all-differences"}, description = "Don't exit on first difference, print all")
    private boolean printAllDifferences;

    @Override
    public Integer call() {
        try {
            DifferenceReporter reporter = printAllDifferences ? new PrintAndContinueReporter() : new PrintAndExitReporter();
            new BamToCramValidator(referenceGenome, samtoolsPath, sambambaPath, cores, reporter).validate(input[0], input[1]);
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
    }

    public static void main(String... args) {
        System.exit(new CommandLine(new BamCompMain()).execute(args));
    }
}
