package com.hartwig.bamcomp;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.util.AsyncBufferedIterator;
import htsjdk.samtools.util.StopWatch;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static java.lang.String.format;

public class BamCompare {
    private static final Logger logger = LoggerFactory.getLogger(BamCompare.class);
    private final String referenceSequence;

    public BamCompare(String referenceSequence) {
        this.referenceSequence = referenceSequence;
    }

    public static void main(String[] args) {
        String rg = "/home/ned/source/hartwig/data/hg37/Homo_sapiens.GRCh37.GATK.illumina.fasta";
        new BamCompare(rg).compare("/home/ned/source/hartwig/data/colo829_chr20/COLO829v003R_chr20.sorted.bam",
                "/home/ned/source/hartwig/data/colo829_chr20/COLO829v003R_chr20.sorted.bam.cram.bam", true);
    }

    public BamComparisonOutcome compare(String bamOne, String bamTwo, boolean compareHeaders) {
        StopWatch stopwatch = new StopWatch();
        stopwatch.start();
        logger.info("Thoroughly comparing \"{}\" to \"{}\"", bamOne, bamTwo);
        try {
            SamReader samOne = open(bamOne);
            SamReader samTwo = open(bamTwo);

            if (compareHeaders && !new HeaderComparator().areHeadersEqualIgnoringRefGenomeUrl(samOne.getFileHeader(), samTwo.getFileHeader())) {
                logger.error("Headers are not equal!");
                logger.error("<<<<<<<<\n{}\n========{}\n>>>>>>>>", samOne.getFileHeader().getSAMString(),
                        samTwo.getFileHeader().getSAMString());
                return new BamComparisonOutcome(false, "Headers are not equal (ignoring ref genome URLs)");
            }

            AsyncBufferedIterator<SAMRecord> itOne = new AsyncBufferedIterator<>(samOne.iterator(), 100);
            AsyncBufferedIterator<SAMRecord> itTwo = new AsyncBufferedIterator<>(samTwo.iterator(), 100);

            long position = 0;
            while (itOne.hasNext()) {
                position++;
                if (itTwo.hasNext()) {
                    SAMRecord nextOne = itOne.next();
                    SAMRecord nextTwo = itTwo.next();
                    if (!nextOne.getSAMString().equals(nextTwo.getSAMString())) {
                        throw new IllegalStateException("Difference found at location " + position + ":\n<<<<" + nextOne.getSAMString() + "\n====\n" + nextTwo.getSAMString() + "\n>>>>");
                    }
                } else {
                    return new BamComparisonOutcome(false, "Fewer records in " + bamTwo);
                }
                if (position % 5_000_000 == 0) {
                    logger.info("{} records inspected", position);
                }
            }
            if (itTwo.hasNext()) {
                return new BamComparisonOutcome(false,
                        format("There is extra data in '%s' beyond position %d", bamTwo, position));
            }
            return new BamComparisonOutcome(true, "All " + position + " records compared equal");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            stopwatch.stop();
            logger.info("Completed comparison in {} seconds", stopwatch.getElapsedTimeSecs());
        }
    }

    private SamReader open(String bamPath) throws IOException {
        InputStream streamOne = FileUtils.openInputStream(new File(bamPath));
        SamReaderFactory samReaderFactory = SamReaderFactory.make().referenceSequence(new File(referenceSequence));
        return samReaderFactory.open(SamInputResource.of(streamOne));
    }

    public static class BamComparisonOutcome {
        protected final boolean areEqual;
        protected final String reason;

        private BamComparisonOutcome(boolean areEqual, String reason) {
            this.areEqual = areEqual;
            this.reason = reason;
        }
    }
}
