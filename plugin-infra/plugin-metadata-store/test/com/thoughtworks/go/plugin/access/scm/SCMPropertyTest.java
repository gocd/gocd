package com.thoughtworks.go.plugin.access.scm;

import com.thoughtworks.go.plugin.api.config.Property;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class SCMPropertyTest {
    @Test
    public void validateSCMPropertyDefaults() throws Exception {
        SCMProperty scmProperty = new SCMProperty("Test-Property");

        assertThat(scmProperty.getOptions().size(), is(5));
        assertThat(scmProperty.getOption(Property.REQUIRED), is(true));
        assertThat(scmProperty.getOption(Property.PART_OF_IDENTITY), is(true));
        assertThat(scmProperty.getOption(Property.SECURE), is(false));
        assertThat(scmProperty.getOption(Property.DISPLAY_NAME), is(""));
        assertThat(scmProperty.getOption(Property.DISPLAY_ORDER), is(0));

        scmProperty = new SCMProperty("Test-Property", "Dummy Value");

        assertThat(scmProperty.getOptions().size(), is(5));
        assertThat(scmProperty.getOption(Property.REQUIRED), is(true));
        assertThat(scmProperty.getOption(Property.PART_OF_IDENTITY), is(true));
        assertThat(scmProperty.getOption(Property.SECURE), is(false));
        assertThat(scmProperty.getOption(Property.DISPLAY_NAME), is(""));
        assertThat(scmProperty.getOption(Property.DISPLAY_ORDER), is(0));
    }
}