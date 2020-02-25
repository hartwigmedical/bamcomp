package com.hartwig.bamcomp;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class BamToCramValidator {
    private static final Logger logger = LoggerFactory.getLogger(BamToCramValidator.class);
    private ExecutorService executorService;

    private static final String ALL_CORES = "$(nproc)";
    private static final String SAMTOOLS = "/opt/tools/samtools/1.9/samtools";
    private static final String SAMBAMBA = "/opt/tools/sambamba/0.6.8/sambamba";
    private static final String REF_GENOME = "/opt/resources/reference_genome/Homo_sapiens.GRCh37.GATK.illumina.fasta";

    private BamToCramValidator(int cores) {
        executorService = Executors.newFixedThreadPool(cores);
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            throw new IllegalArgumentException("USAGE: [in1.bam|in1.cram] [in2.bam|in2.cram] [# of cores]");
        }
        try {
            new BamToCramValidator(Integer.parseInt(args[2])).validate(args[0], args[1]);
            System.exit(0);
        } catch (Exception e) {
            logger.error("Unexpected exception!", e);
            System.exit(1);
        }
    }

    private void validate(String inputOne, String inputTwo) {
        try {
            String one = ensureBam(inputOne);
            String two = ensureBam(inputTwo);
            Callable<String> bamComparison = () -> {
                BamCompare.ComparisonOutcome result = new BamCompare(REF_GENOME, new HeaderComparator())
                        .compare(one, two, true);
                if (result.areEqual) {
                    return result.reason;
                } else {
                    throw new RuntimeException(result.reason);
                }
            };
            List<Callable<String>> callables = asList(flagstat(one), flagstat(two), bamComparison);
            List<Future<String>> futures = execute(callables);
            String pathA = futures.get(0).get();
            String pathB = futures.get(1).get();
            if (!FileUtils.contentEquals(new File(pathA), new File(pathB))) {
                throw new RuntimeException(format("Contents of \"%s\" != \"%s\"", pathA, pathB));
            }
            logger.info("Thorough validation succeeded: " + futures.get(2).get());
        } catch (ExecutionException | InterruptedException | IOException e) {
            throw new RuntimeException("Validation failed", e);
        }
    }

    private String ensureBam(String inputFile) throws InterruptedException, ExecutionException {
        if (inputFile.toLowerCase().endsWith(".cram")) {
            String newBam = inputFile + ".bam";
            logger.info("Converting '{}' to BAM '{}'", inputFile, newBam);
            Callable<String> callback = shell(format("%s view -O bam,decode_md=0 -o %s -@ %s -T %s %s",
                    SAMTOOLS,  newBam, ALL_CORES, REF_GENOME, inputFile), newBam);
            return execute(singletonList(callback)).get(0).get();
        } else {
            return inputFile;
        }
    }

    private List<Future<String>> execute(List<Callable<String>> callables) {
        try {
            return executorService.invokeAll(callables);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while executing", e);
        }
    }

    private Callable<String> flagstat(String inputFile) {
        String outputFile = inputFile + ".flagstat";
        return shell(format("%s flagstat -t %s %s | tee %s", SAMBAMBA, ALL_CORES, inputFile, outputFile), outputFile);
    }

    private Callable<String> shell(String command, String outputPath) {
        return () -> {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("/bin/bash", "-exc", command).inheritIO();
            Process process = builder.start();
            int exitCode = process.waitFor();
            logger.info("Process output: {}", IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8));
            if (exitCode != 0) {
                throw new RuntimeException(format("Command returned %d: \"%s\"", exitCode, command));
            }
            return outputPath;
        };
    }
}
