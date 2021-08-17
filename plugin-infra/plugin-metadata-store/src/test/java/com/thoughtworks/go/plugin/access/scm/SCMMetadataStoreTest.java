/*
 * Copyright 2021 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.plugin.access.scm;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class SCMMetadataStoreTest {
    @BeforeEach
    public void setUp() throws Exception {
        SCMMetadataStore.getInstance().clear();
    }

    @AfterEach
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
