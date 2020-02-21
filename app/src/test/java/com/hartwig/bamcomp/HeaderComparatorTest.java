package com.hartwig.bamcomp;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class HeaderComparatorTest {
    private static File resource(String name) {
        return new File(System.getProperty("user.dir") + "/src/test/resources/HeaderComparatorTest/" + name);
    }

    private static boolean invoke(String resourceOne, String resourceTwo) {
        HeaderComparator victim = new HeaderComparator();
        SamReader one = SamReaderFactory.make().open(resource(resourceOne));
        SamReader two = SamReaderFactory.make().open(resource(resourceTwo));
        return victim.areHeadersEquivalent(one.getFileHeader(), two.getFileHeader());
    }

    @Test
    public void completelyIdenticalHeadersShouldBeEqual() {
        assertThat(invoke("ok.header", "copy_of_ok.header")).isTrue();
    }

    @Test
    public void shouldReturnFalseWhenSequenceCountsAreNotEqual() {
        assertThat(invoke("ok.header", "missing_sequence.header")).isFalse();
        assertThat(invoke("missing_sequence.header", "ok.header")).isFalse();
    }

    @Test
    public void headersWithDifferentUrAttributesShouldStillBeEqual() {
        assertThat(invoke("ok.header", "changed_ur.header")).isTrue();
        assertThat(invoke("changed_ur.header", "ok.header")).isTrue();
    }

    @Test
    public void headersWithDifferentMdAttributesShouldNotBeEqual() {
        assertThat(invoke("ok.header", "changed_md5s.header")).isFalse();
        assertThat(invoke("changed_md5s.header", "ok.header")).isFalse();
    }

    @Test
    public void shouldBeEqualIfSequencesAreTheSameExceptMdIsMissingInOne() {
        assertThat(invoke("no_md5s.header","ok.header")).isTrue();
        assertThat(invoke("ok.header","no_md5s.header")).isTrue();
    }

    @Test
    public void shouldNotBeEqualIfSequenceLnsVary() {
        assertThat(invoke("ok.header","changed_sequence_lns.header")).isFalse();
    }

    @Test
    public void shouldNotBeEqualIfReadGroupsAreDifferent() {
        assertThat(invoke("changed_read_group.header", "ok.header")).isFalse();
    }

    @Test
    public void shouldNotBeEqualIfNumberOfReadGroupsAreDifferent() {
        assertThat(invoke("changed_read_group.header", "ok.header")).isFalse();
    }

    @Test
    public void shouldNotBeEqualIfHdAttributeIsDifferent() {
        assertThat(invoke("hd_attribute_changed.header", "ok.header")).isFalse();
    }


}