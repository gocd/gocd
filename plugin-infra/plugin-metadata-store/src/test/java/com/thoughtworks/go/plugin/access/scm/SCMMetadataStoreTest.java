/*
 * Copyright Thoughtworks, Inc.
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

import static org.assertj.core.api.Assertions.assertThat;

public class SCMMetadataStoreTest {
    @BeforeEach
    public void setUp() {
        SCMMetadataStore.getInstance().clear();
    }

    @AfterEach
    public void tearDown() {
        SCMMetadataStore.getInstance().clear();
    }

    @Test
    public void shouldPopulateDataCorrectly() {
        SCMConfigurations scmConfigurations = new SCMConfigurations();
        SCMView scmView = createSCMView("display-value", "template");
        SCMMetadataStore.getInstance().addMetadataFor("plugin-id", scmConfigurations, scmView);

        assertThat(SCMMetadataStore.getInstance().getConfigurationMetadata("plugin-id")).isEqualTo(scmConfigurations);
        assertThat(SCMMetadataStore.getInstance().getViewMetadata("plugin-id")).isEqualTo(scmView);
        assertThat(SCMMetadataStore.getInstance().displayValue("plugin-id")).isEqualTo("display-value");
        assertThat(SCMMetadataStore.getInstance().template("plugin-id")).isEqualTo("template");

        assertThat(SCMMetadataStore.getInstance().getConfigurationMetadata("some-plugin-which-does-not-exist")).isNull();
        assertThat(SCMMetadataStore.getInstance().getViewMetadata("some-plugin-which-does-not-exist")).isNull();
        assertThat(SCMMetadataStore.getInstance().displayValue("some-plugin-which-does-not-exist")).isNull();
        assertThat(SCMMetadataStore.getInstance().template("some-plugin-which-does-not-exist")).isNull();
    }

    @Test
    public void shouldBeAbleToCheckIfPluginExists() {
        SCMConfigurations scmConfigurations = new SCMConfigurations();
        SCMView scmView = createSCMView(null, null);
        SCMMetadataStore.getInstance().addMetadataFor("plugin-id", scmConfigurations, scmView);

        assertThat(SCMMetadataStore.getInstance().hasPlugin("plugin-id")).isEqualTo(true);
        assertThat(SCMMetadataStore.getInstance().hasPlugin("some-plugin-which-does-not-exist")).isEqualTo(false);
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
