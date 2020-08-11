package com.hartwig.bamcomp;

import java.util.concurrent.Callable;

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

    @Option(names = {"-1", "--input-one"}, description = "First input file", required = true)
    private String inputOne;

    @Option(names = {"-2", "--input-two"}, description = "Second input file", required = true)
    private String inputTwo;

    @Override
    public Integer call() {
        try {
            new BamToCramValidator(referenceGenome, samtoolsPath, sambambaPath, cores).validate(inputOne, inputTwo);
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
