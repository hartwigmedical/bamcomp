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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.hartwig.bamcomp.BamCompare.ComparisonOutcome.fail;
import static java.lang.String.format;

public class BamCompare {
    private static final Logger logger = LoggerFactory.getLogger(BamCompare.class);
    private final String referenceSequence;
    private HeaderComparator headerComparator;

    public BamCompare(String referenceSequence, HeaderComparator headerComparator) {
        this.referenceSequence = referenceSequence;
        this.headerComparator = headerComparator;
    }

    public ComparisonOutcome compare(String bamOne, String bamTwo, boolean compareHeaders) {
        StopWatch stopwatch = new StopWatch();
        stopwatch.start();
        logger.info("Thoroughly comparing \"{}\" to \"{}\"", bamOne, bamTwo);
        try {
            SamReader samOne = open(bamOne);
            SamReader samTwo = open(bamTwo);

            if (compareHeaders && !headerComparator.areHeadersEquivalent(samOne.getFileHeader(), samTwo.getFileHeader())) {
                String notEqual = "Headers are not equal!";
                logger.error(notEqual);
                logger.info("Header for '{}':\n\n{}", bamOne, samOne.getFileHeader().getSAMString());
                logger.info("Header for '{}':\n\n{}", bamTwo, samTwo.getFileHeader().getSAMString());
                return new ComparisonOutcome(false, notEqual);
            }

            AsyncBufferedIterator<SAMRecord> itOne = new AsyncBufferedIterator<>(samOne.iterator(), 100);
            AsyncBufferedIterator<SAMRecord> itTwo = new AsyncBufferedIterator<>(samTwo.iterator(), 100);

            long position = 0;
            while (itOne.hasNext()) {
                position++;
                if (itTwo.hasNext()) {
                    SAMRecord nextOne = itOne.next();
                    SAMRecord nextTwo = itTwo.next();
                    ComparisonOutcome comparison = compare(nextOne, nextTwo);
                    if (!comparison.areEqual) {
                        throw new IllegalStateException(format("%s at %d:\n<<<<<\n%s\n=====\n%s\n>>>>>",
                                comparison.reason, position, nextOne.getSAMString(), nextTwo.getSAMString()));
                    }
                } else {
                    return new ComparisonOutcome(false, "Fewer records in " + bamTwo);
                }
                if (position % 5_000_000 == 0) {
                    logger.info("{} records inspected", position);
                }
            }
            if (itTwo.hasNext()) {
                return new ComparisonOutcome(false,
                        format("There is extra data in '%s' beyond position %d", bamTwo, position));
            }
            return new ComparisonOutcome(true, "All " + position + " records compared equal");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            stopwatch.stop();
            logger.info("Completed comparison in {} seconds", stopwatch.getElapsedTimeSecs());
        }
    }

    private ComparisonOutcome compare(SAMRecord one, SAMRecord two) {
        if (safeUnequals(one.getReadName(), two.getReadName())) {
            return fail("Read Name");
        }
        if (one.getFlags() != two.getFlags()) {
            return fail("Flags");
        }
        if (safeUnequals(one.getReferenceName(), two.getReferenceName())) {
            return fail("Reference Name");
        }
        if (safeUnequals(one.getReferenceIndex(), two.getReferenceIndex())) {
            return fail("Reference Index");
        }
        if (one.getAlignmentStart() != two.getAlignmentStart()) {
            return fail("Alignment start");
        }
        if (one.getMappingQuality() != two.getMappingQuality()) {
            return fail("Mapping quality");
        }
        if (safeUnequals(one.getCigar(), two.getCigar())) {
            return fail("CIGAR");
        }
        if (one.getMateAlignmentStart() != two.getMateAlignmentStart()) {
            return fail("Mate Alignment Start");
        }
        if (safeUnequals(one.getMateReferenceName(), two.getMateReferenceName())) {
            return fail("Mate Reference Name");
        }
        if (safeUnequals(one.getMateReferenceIndex(), two.getMateReferenceIndex())) {
            return fail("Mate Reference Index");
        }
        if (one.getInferredInsertSize() != two.getInferredInsertSize()) {
            return fail("Inferred Size");
        }
        if (!Arrays.equals(one.getReadBases(), two.getReadBases())) {
            return fail("Read Bases");
        }
        if (!Arrays.equals(one.getBaseQualities(), two.getBaseQualities())) {
            return fail("Base Qualities");
        }
        if (safeUnequals(filterTags(one), filterTags(two))) {
            return fail("Attributes (tags)");
        }
        return new BamCompare.ComparisonOutcome(true, "Equal but didn't check MD and NM tags");
    }

    private Map<String, Object> filterTags(SAMRecord record) {
        Map<String, Object> asMap = new HashMap<>();
        record.getAttributes().stream()
                .filter(tag -> !(tag.tag.equalsIgnoreCase("nm") || tag.tag.equalsIgnoreCase("md")))
                .forEach(tag -> asMap.merge(tag.tag, tag.value, (k, v) -> {
                    throw new IllegalArgumentException("Duplicate tag " + tag.tag);
                }));
        return asMap;
    }

    private static <T> boolean safeUnequals(final T attribute1, final T attribute2) {
        return attribute1 != null ? !attribute1.equals(attribute2) : attribute2 != null;
    }

    private SamReader open(String bamPath) throws IOException {
        InputStream streamOne = FileUtils.openInputStream(new File(bamPath));
        SamReaderFactory samReaderFactory = SamReaderFactory.make().referenceSequence(new File(referenceSequence));
        return samReaderFactory.open(SamInputResource.of(streamOne));
    }

    public static class ComparisonOutcome {
        protected final boolean areEqual;
        protected final String reason;

        private ComparisonOutcome(boolean areEqual, String reason) {
            this.areEqual = areEqual;
            this.reason = reason;
        }

        static ComparisonOutcome fail(String reason) {
            return new ComparisonOutcome(false, reason);
        }
    }
}
