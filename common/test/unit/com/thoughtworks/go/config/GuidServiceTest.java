package com.thoughtworks.go.config;

import org.junit.After;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class GuidServiceTest {

    @After
    public void tearDown() {
        GuidService.deleteGuid();
    }

    @Test
    public void shouldStripExtraWhitespaceFromGuidStringOnLoad() throws Exception {
        GuidService.storeGuid(" \tuuid\n ");
        assertThat(GuidService.loadGuid(), is("uuid"));
    }
}