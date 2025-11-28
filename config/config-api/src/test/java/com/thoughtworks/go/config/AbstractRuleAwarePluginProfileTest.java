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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.rules.RuleAwarePluginProfile;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import org.junit.jupiter.api.Test;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractRuleAwarePluginProfileTest {

    protected abstract RuleAwarePluginProfile newPluginProfile(String id, String pluginId, ConfigurationProperty... configurationProperties);

    protected abstract String getObjectDescription();

    @Test
    public void shouldNotAllowNullPluginIdOrProfileId() {
        RuleAwarePluginProfile profile = newPluginProfile(null, null);

        profile.validate(getValidationContext(profile));
        assertThat(profile.errors().size()).isEqualTo(2);
        assertThat(profile.errors().firstErrorOn("pluginId")).isEqualTo(format("%s cannot have a blank plugin id.", getObjectDescription()));
        assertThat(profile.errors().firstErrorOn("id")).isEqualTo(format("%s cannot have a blank id.", getObjectDescription()));
    }

    @Test
    public void shouldValidatePluginIdPattern() {
        RuleAwarePluginProfile profile = newPluginProfile("!123", "docker");
        profile.validate(getValidationContext(profile));
        assertThat(profile.errors().size()).isEqualTo(1);
        assertThat(profile.errors().firstErrorOn("id")).isEqualTo("Invalid id '!123'. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
    }

    @Test
    public void shouldValidateConfigPropertyNameUniqueness() {
        ConfigurationProperty prop1 = ConfigurationPropertyMother.create("USERNAME");
        ConfigurationProperty prop2 = ConfigurationPropertyMother.create("USERNAME");
        RuleAwarePluginProfile profile = newPluginProfile("docker.unit-test", "cd.go.elastic-agent.docker", prop1, prop2);

        profile.validate(getValidationContext(profile));

        assertThat(profile.errors().size()).isEqualTo(0);

        ConfigurationProperty configProp1 = profile.getConfiguration().get(0);
        ConfigurationProperty configProp2 = profile.getConfiguration().get(1);

        assertThat(configProp1.errors().size()).isEqualTo(1);
        assertThat(configProp2.errors().size()).isEqualTo(1);

        assertThat(configProp1.errors().firstErrorOn("configurationKey")).isEqualTo(format("Duplicate key 'USERNAME' found for %s 'docker.unit-test'", getObjectDescription()));
        assertThat(configProp2.errors().firstErrorOn("configurationKey")).isEqualTo(format("Duplicate key 'USERNAME' found for %s 'docker.unit-test'", getObjectDescription()));
    }

    protected ValidationContext getValidationContext(RuleAwarePluginProfile profile) {
        return null;
    }
}
