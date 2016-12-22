package com.thoughtworks.go.plugin.access.scm;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
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
        SCMView scmView = createSCMView("display-value", "template");
        SCMMetadataStore.getInstance().addMetadataFor("plugin-id", scmConfigurations, scmView);

        assertThat(SCMMetadataStore.getInstance().getConfigurationMetadata("plugin-id"), is(scmConfigurations));
        assertThat(SCMMetadataStore.getInstance().getViewMetadata("plugin-id"), is(scmView));
        assertThat(SCMMetadataStore.getInstance().displayValue("plugin-id"), is("display-value"));
        assertThat(SCMMetadataStore.getInstance().template("plugin-id"), is("template"));

        assertThat(SCMMetadataStore.getInstance().getConfigurationMetadata("some-plugin-which-does-not-exist"), is(nullValue()));
        assertThat(SCMMetadataStore.getInstance().getViewMetadata("some-plugin-which-does-not-exist"), is(nullValue()));
        assertThat(SCMMetadataStore.getInstance().displayValue("some-plugin-which-does-not-exist"), is(nullValue()));
        assertThat(SCMMetadataStore.getInstance().template("some-plugin-which-does-not-exist"), is(nullValue()));
    }

    @Test
    public void shouldBeAbleToCheckIfPluginExists() throws Exception {
        SCMConfigurations scmConfigurations = new SCMConfigurations();
        SCMView scmView = createSCMView(null, null);
        SCMMetadataStore.getInstance().addMetadataFor("plugin-id", scmConfigurations, scmView);

        assertThat(SCMMetadataStore.getInstance().hasPlugin("plugin-id"), is(true));
        assertThat(SCMMetadataStore.getInstance().hasPlugin("some-plugin-which-does-not-exist"), is(false));
    }

    private SCMView createSCMView(final String displayValue, final String template) {
        return new SCMView() {
            @Override
            public String displayValue() {
                return displayValue;
            }

            @Override
            public String template() {
                return template;
            }
        };
    }
}