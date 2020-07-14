package com.thoughtworks.go.addon.businesscontinuity;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
class FileDetailsTest {

    @Test
    void shouldImplementToString() {
        assertThat(new FileDetails("some-md5").toString(), is("md5='some-md5'"));
    }
}
