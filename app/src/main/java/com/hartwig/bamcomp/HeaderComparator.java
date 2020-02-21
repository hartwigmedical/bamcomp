package com.hartwig.bamcomp;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HeaderComparator {
    boolean areHeadersEquivalent(SAMFileHeader one, SAMFileHeader two) {
        if (one.getReadGroups().size() != two.getReadGroups().size()) {
            return false;
        }

        for (SAMReadGroupRecord readGroup : one.getReadGroups()) {
            if (!two.getReadGroups().contains(readGroup)) {
                return false;
            }
        }

        if (!one.getAttributes().equals(two.getAttributes())) {
            return false;
        }

        SAMSequenceDictionary sequenceDictionary = one.getSequenceDictionary();
        if (sequenceDictionary.size() != two.getSequenceDictionary().size()) {
            return false;
        }

        for (int i = 0; i < sequenceDictionary.getSequences().size(); i++) {
            SAMSequenceRecord recordOne = one.getSequenceDictionary().getSequences().get(i);
            SAMSequenceRecord recordTwo = two.getSequenceDictionary().getSequences().get(i);
            if (!recordOne.isSameSequence(recordTwo)) {
                return false;
            }
        }
        return true;
    }
}
