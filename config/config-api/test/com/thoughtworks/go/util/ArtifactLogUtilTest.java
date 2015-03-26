package com.thoughtworks.go.util;

import org.junit.Test;

import static com.thoughtworks.go.util.ArtifactLogUtil.isConsoleOutput;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ArtifactLogUtilTest {
    @Test
    public void shouldIdentifyConsoleLog() throws Exception {
        assertThat(isConsoleOutput("cruise-output/console.log"), is(true));
    }

    @Test
    public void shouldNotIdentifyAnyOtherArtifactAsConsoleLog() throws Exception {
        assertThat(isConsoleOutput("artifact"), is(false));
    }
}