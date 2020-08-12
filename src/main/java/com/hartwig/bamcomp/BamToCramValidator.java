package com.hartwig.bamcomp;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.hartwig.bamcomp.report.DifferenceReporter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BamToCramValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(BamToCramValidator.class);
    private final String referenceGenome;
    private final ExecutorService executorService;
    private final DifferenceReporter reporter;
    private final String samtoolsPath;
    private final String sambambaPath;
    private final int cores;

    BamToCramValidator(final String referenceGenome, final String samtoolsPath, final String sambambaPath, final int cores,
            final DifferenceReporter reporter) {
        this.referenceGenome = referenceGenome;
        this.samtoolsPath = samtoolsPath;
        this.sambambaPath = sambambaPath;
        this.cores = cores;
        executorService = Executors.newFixedThreadPool(cores);
        this.reporter = reporter;
    }

    void validate(String inputOne, String inputTwo) {
        try {
            String one = ensureBam(inputOne);
            String two = ensureBam(inputTwo);
            Callable<Void> bamComparison = () -> {
                new BamCompare(referenceGenome, new HeaderComparator()).compare(one, two, true, reporter);
                return null;
            };
            String flagstatOne = inputOne + ".flagstat";
            String flagstatTwo = inputTwo + ".flagstat";
            List<Callable<Void>> callables = asList(flagstat(one, flagstatOne), flagstat(two, flagstatTwo), bamComparison);
            List<Future<Void>> futures = execute(callables);
            futures.get(0);
            futures.get(1);
            if (!FileUtils.contentEquals(new File(flagstatOne), new File(flagstatTwo))) {
                reporter.report(format("Contents of \"%s\" != \"%s\"", flagstatOne, flagstatTwo));
            }
            futures.get(2);
            LOGGER.info("Thorough validation succeeded");
        } catch (IOException e) {
            throw new RuntimeException("Validation failed", e);
        }
    }

    private String ensureBam(String inputFile) {
        if (inputFile.toLowerCase().endsWith(".cram")) {
            String newBam = inputFile + ".bam";
            LOGGER.info("Converting '{}' to BAM '{}'", inputFile, newBam);
            Callable<Void> callback = shell(format("%s view -O bam -o %s -@ %s %s",
                    samtoolsPath,  newBam, cores, inputFile), newBam);
            execute(singletonList(callback)).get(0);
            return newBam;
        } else {
            return inputFile;
        }
    }

    private List<Future<Void>> execute(List<Callable<Void>> callables) {
        try {
            return executorService.invokeAll(callables);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while executing", e);
        }
    }

    private Callable<Void> flagstat(String inputFile, String outputFile) {
        return shell(format("%s flagstat -t %s %s | tee %s", sambambaPath, cores, inputFile, outputFile), outputFile);
    }

    private Callable<Void> shell(String command, String outputPath) {
        return () -> {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("/bin/bash", "-exc", command).inheritIO();
            Process process = builder.start();
            int exitCode = process.waitFor();
            LOGGER.info("Process output: {}", IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8));
            if (exitCode != 0) {
                throw new RuntimeException(format("Command returned %d: \"%s\"", exitCode, command));
            }
            return null;
        };
    }
}
