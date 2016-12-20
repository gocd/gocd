package com.thoughtworks.go.plugin.access.scm;

import org.hamcrest.core.Is;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class SCMConfigurationTest {
    @Test
    public void shouldGetOptionIfAvailable() {
        SCMConfiguration scmConfiguration = new SCMConfiguration("key");
        scmConfiguration.with(SCMConfiguration.REQUIRED, true);

        assertThat(scmConfiguration.hasOption(SCMConfiguration.REQUIRED), is(true));
        assertThat(scmConfiguration.hasOption(SCMConfiguration.SECURE), is(false));
    }

    @Test
    public void shouldGetOptionValue() {
        SCMConfiguration scmConfiguration = new SCMConfiguration("key");
        scmConfiguration.with(SCMConfiguration.DISPLAY_NAME, "some display name");
        scmConfiguration.with(SCMConfiguration.DISPLAY_ORDER, 3);

        assertThat(scmConfiguration.getOption(SCMConfiguration.DISPLAY_NAME), is("some display name"));
        assertThat(scmConfiguration.getOption(SCMConfiguration.DISPLAY_ORDER), is(3));
    }

    @Test
    public void shouldSortByDisplayOrder() throws Exception {
        SCMConfiguration p1 = new SCMConfiguration("k1").with(SCMConfiguration.DISPLAY_ORDER, 1);
        SCMConfiguration p2 = new SCMConfiguration("k2").with(SCMConfiguration.DISPLAY_ORDER, 3);

        assertThat(p2.compareTo(p1), Is.is(2));
    }
}