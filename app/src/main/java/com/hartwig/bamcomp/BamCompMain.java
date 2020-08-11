package com.hartwig.bamcomp;

import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Option;

public class BamCompMain implements Callable<Integer> {
    @Option(names = {"-n", "--cores"}, description = "Number of cores to use for processing", defaultValue = "1")
    private Integer cores;

    @Option(names = {"-g", "--reference-genome"}, description = "Reference genome [hg19|hg38|custom path]", required = true)
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
            String referenceGenomeFile;
            if ("hg19".equals(referenceGenome)) {
                referenceGenomeFile = "/opt/resources/reference_genome/hg37/Homo_sapiens.GRCh37.GATK.illumina.fasta";
            } else if ("hg38".equals(referenceGenome)) {
                referenceGenomeFile = "/opt/resources/reference_genome/hg38/GCA_000001405.15_GRCh38_no_alt_analysis_set.fna";
            } else {
                referenceGenomeFile = referenceGenome;
            }
            new BamToCramValidator(referenceGenomeFile, samtoolsPath, sambambaPath, cores).validate(inputOne, inputTwo);
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
