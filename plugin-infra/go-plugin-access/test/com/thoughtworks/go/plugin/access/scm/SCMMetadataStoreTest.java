package com.thoughtworks.go.plugin.access.scm;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class SCMMetadataStoreTest {
    @Before
    public void setUp() throws Exception {
        SCMMetadataStore.getInstance().clear();
    }

    @After
    public void tearDown() throws Exception {
        SCMMetadataStore.getInstance().clear();
    }

    @Test
    public void shouldPopulateDataCorrectly() throws Exception {
        SCMConfigurations scmConfigurations = new SCMConfigurations();
        SCMMetadataStore.getInstance().addMetadataFor("plugin-id", scmConfigurations);

        assertThat(SCMMetadataStore.getInstance().getMetadata("plugin-id"), is(scmConfigurations));
    }

    @Test
    public void shouldBeAbleToCheckIfPluginExists() throws Exception {
        SCMConfigurations scmConfigurations = new SCMConfigurations();
        SCMMetadataStore.getInstance().addMetadataFor("plugin-id", scmConfigurations);

        assertThat(SCMMetadataStore.getInstance().hasPlugin("plugin-id"), is(true));
        assertThat(SCMMetadataStore.getInstance().hasPlugin("some-plugin-which-does-not-exist"), is(false));
    }
}