/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.domain.scm;

import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.plugin.access.scm.SCMConfigurations;
import com.thoughtworks.go.plugin.access.scm.SCMMetadataStore;
import com.thoughtworks.go.plugin.access.scm.SCMProperty;
import com.thoughtworks.go.plugin.access.scm.SCMPropertyConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static com.thoughtworks.go.plugin.api.config.Property.PART_OF_IDENTITY;
import static com.thoughtworks.go.plugin.api.config.Property.REQUIRED;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class SCMsTest {
    @BeforeEach
    void setup() {
        SCMMetadataStore.getInstance().clear();
    }

    @AfterEach
    void tearDown() {
        SCMMetadataStore.getInstance().clear();
    }

    @Test
    void shouldCheckEqualityOfSCMs() {
        SCM scm = new SCM();
        SCMs scms = new SCMs(scm);

        assertThat(scms).isEqualTo(new SCMs(scm));
    }

    @Test
    void shouldFindSCMGivenTheSCMId() {
        SCM scm1 = SCMMother.create("id1");
        SCM scm2 = SCMMother.create("id2");

        SCMs scms = new SCMs(scm1, scm2);

        assertThat(scms.find("id2")).isEqualTo(scm2);
    }

    @Test
    void shouldReturnNullIfNoMatchingSCMFound() {
        assertThat(new SCMs().find("not-found")).isNull();
    }

    @Test
    void shouldRemoveSCMById() {
        SCM scm1 = SCMMother.create("id1");
        SCM scm2 = SCMMother.create("id2");
        SCMs scms = new SCMs(scm1, scm2);

        scms.removeSCM("id1");

        assertThat(scms).containsExactly(scm2);
    }

    @Test
    void shouldThrowRuntimeExceptionWhenTryingToRemoveSCMIdWhichIsNotPresent() {
        SCMs scms = new SCMs();
        try {
            scms.removeSCM("id1");
            fail("should not reach here");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo(String.format("Could not find SCM with id '%s'", "id1"));
        }
    }

    @Test
    void shouldValidateForCaseInsensitiveNameAndIdUniqueness() {
        SCM duplicate1 = SCMMother.create("scm-id1");
        SCM duplicate2 = SCMMother.create("SCM-ID1");
        SCM unique = SCMMother.create("unique");
        SCMs scms = new SCMs(duplicate1, duplicate2, unique);

        scms.validate(null);

        assertThat(duplicate1.errors().isEmpty()).isFalse();
        assertThat(duplicate1.errors().getAllOn(SCM.NAME).contains(String.format("Cannot save SCM, found multiple SCMs called '%s'. SCM names are case-insensitive and must be unique.", duplicate1.getName()))).isTrue();
        assertThat(duplicate2.errors().isEmpty()).isFalse();
        assertThat(duplicate2.errors().getAllOn(SCM.NAME).contains(String.format("Cannot save SCM, found multiple SCMs called '%s'. SCM names are case-insensitive and must be unique.", duplicate2.getName()))).isTrue();
        assertThat(unique.errors().getAllOn(SCM.NAME)).isNull();
    }

    @Test
    void shouldFailValidationIfMaterialWithDuplicateFingerprintIsFound() {
        String pluginId = "plugin-id";
        SCMPropertyConfiguration scmConfiguration = new SCMPropertyConfiguration();
        scmConfiguration.add(new SCMProperty("k1"));
        scmConfiguration.add(new SCMProperty("k2").with(REQUIRED, false).with(PART_OF_IDENTITY, false));
        SCMMetadataStore.getInstance().addMetadataFor(pluginId, new SCMConfigurations(scmConfiguration), null);

        SCM scm1 = SCMMother.create("1", "scm1", pluginId, "1.0", new Configuration(create("k1", false, "v1")));
        SCM scm2 = SCMMother.create("2", "scm2", pluginId, "1.0", new Configuration(create("k1", false, "v2")));
        SCM scm3 = SCMMother.create("3", "scm3", pluginId, "1.0", new Configuration(create("k1", false, "v1")));
        SCM scm4 = SCMMother.create("4", "scm4", pluginId, "1.0", new Configuration(create("k1", false, "V1")));
        SCM scm5 = SCMMother.create("5", "scm5", pluginId, "1.0", new Configuration(create("k1", false, "v1"), create("k2", false, "v2")));

        SCMs scms = new SCMs(scm1, scm2, scm3, scm4, scm5);

        scms.validate(null);

        assertThat(scm2.getFingerprint().equals(scm1.getFingerprint())).isFalse();
        assertThat(scm3.getFingerprint().equals(scm1.getFingerprint())).isTrue();
        assertThat(scm4.getFingerprint().equals(scm1.getFingerprint())).isFalse();
        assertThat(scm5.getFingerprint().equals(scm1.getFingerprint())).isTrue();

        String expectedErrorMessage = "Cannot save SCM, found duplicate SCMs. scm1, scm3, scm5";
        assertThat(scm1.errors().getAllOn(SCM.SCM_ID)).isEqualTo(asList(expectedErrorMessage));
        assertThat(scm2.errors().getAllOn(SCM.SCM_ID)).isNull();
        assertThat(scm3.errors().getAllOn(SCM.SCM_ID)).isEqualTo(asList(expectedErrorMessage));
        assertThat(scm4.errors().getAllOn(SCM.SCM_ID)).isNull();
        assertThat(scm5.errors().getAllOn(SCM.SCM_ID)).isEqualTo(asList(expectedErrorMessage));
    }

    @Test
    void shouldFindFunctionallyDuplicateSCMs() {
        Configuration config = new Configuration();
        config.addNewConfigurationWithValue("url", "url", false);
        PluginConfiguration pluginConfig = new PluginConfiguration("plugin_id", "1.0");
        SCM scm1 = new SCM("scmid", pluginConfig, config);
        scm1.setName("noName");
        SCMs scms = new SCMs(scm1);

        SCM scm2 = new SCM("scmid", new PluginConfiguration(), new Configuration());
        SCM scm3 = new SCM("id", pluginConfig, config);

        assertThat(scms.findDuplicate(scm2)).isEqualTo(scm1);
        assertThat(scms.findDuplicate(scm3)).isEqualTo(scm1);
    }

    @Test
    void shouldDetermineIfAddingAnSCMWouldCreateDuplication() {
        Configuration config = new Configuration();
        config.addNewConfigurationWithValue("url", "url", false);
        PluginConfiguration pluginConfig = new PluginConfiguration("plugin_id", "1.0");
        SCM scm1 = new SCM("scmid", pluginConfig, config);
        scm1.setName("noName");
        SCMs scms = new SCMs(scm1);

        SCM scm2 = new SCM("scmid", new PluginConfiguration(), new Configuration());
        SCM scm3 = new SCM("id", pluginConfig, config);
        SCM scm4 = new SCM("something", new PluginConfiguration(), new Configuration());
        scm4.setName("noName");
        SCM scm5 = new SCM("arbitrary", new PluginConfiguration(), new Configuration());
        scm5.setName("aRandomName");

        assertThat(scms.canAdd(scm2)).isFalse();
        assertThat(scms.canAdd(scm3)).isFalse();
        assertThat(scms.canAdd(scm4)).isFalse();
        assertThat(scms.canAdd(scm5)).isTrue();
    }
}
