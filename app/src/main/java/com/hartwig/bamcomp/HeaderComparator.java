package com.hartwig.bamcomp;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;

import java.util.Map;
import java.util.Set;

public class HeaderComparator {
    boolean areHeadersEqualIgnoringRefGenomeUrl(SAMFileHeader one, SAMFileHeader two) {
        if (one.getReadGroups().size() != two.getReadGroups().size()) {
            return false;
        }

        for (SAMReadGroupRecord readGroup : one.getReadGroups()) {
            if (!two.getReadGroups().contains(readGroup)) {
                return false;
            }
        }

        Set<Map.Entry<String, String>> attributes = one.getAttributes();
        for (Map.Entry<String, String> attribute: attributes) {
            if (! two.getAttributes().contains(attribute)) {
                return false;
            }
        }

        SAMSequenceDictionary sequenceDictionary = one.getSequenceDictionary();
        if (sequenceDictionary.size() != two.getSequenceDictionary().size()) {
            return false;
        }

        for (int i = 0; i < sequenceDictionary.getSequences().size(); i++) {
            SAMSequenceRecord recordOne = one.getSequenceDictionary().getSequences().get(i);
            SAMSequenceRecord recordTwo = two.getSequenceDictionary().getSequences().get(i);
            // In the headers I looked at there were only the "M5" and "UR" attributes, the second of which contains
            // the URL, which varies depending on the local path on the machine it was done on. It's possible we
            // could miss some attributes of course if other BAMs have other attributes in addition to these two.
            if (!(recordOne.getSequenceName().equals(recordTwo.getSequenceName()))
                    || recordOne.getSequenceIndex() != recordTwo.getSequenceIndex()
                    || recordOne.getSequenceLength() != recordTwo.getSequenceLength()) {
                return false;
            }
            String m5 = "M5";
            String m51 = recordOne.getAttribute(m5);
            String m52 = recordTwo.getAttribute(m5);
            if ((m51 == null && m52 != null)
                    || (m51 != null && m52 == null)
                    || !(recordTwo.getAttribute(m5).equals(recordTwo.getAttribute(m5)))) {
                return false;
            }
        }
        return true;
    }
}
