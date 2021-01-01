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
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static java.lang.String.format;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public abstract class AbstractPluginProfileTest {

    protected abstract PluginProfile pluginProfile(String id, String pluginId, ConfigurationProperty... configurationProperties);
    protected abstract String getObjectDescription();

    @Nested
    class validate {
        @Test
        void shouldNotAllowNullPluginIdOrProfileId() {
            PluginProfile profile = pluginProfile(null, null);

            profile.validate(null);

            assertThat(profile.errors().size(), is(2));
            assertThat(profile.errors().on("pluginId"), is(format("%s cannot have a blank plugin id.", getObjectDescription())));
            assertThat(profile.errors().on("id"), is(format("%s cannot have a blank id.", getObjectDescription())));
        }

        @Test
        void shouldValidatePluginIdPattern() throws Exception {
            PluginProfile profile = pluginProfile("!123", "docker");

            profile.validate(null);

            assertThat(profile.errors().size(), is(1));
            assertThat(profile.errors().on("id"), is("Invalid id '!123'. This must be alphanumeric and " +
                    "can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
        }

        @Test
        void shouldValidateConfigPropertyNameUniqueness() throws Exception {
            ConfigurationProperty prop1 = ConfigurationPropertyMother.create("USERNAME");
            ConfigurationProperty prop2 = ConfigurationPropertyMother.create("USERNAME");
            PluginProfile profile = pluginProfile("docker.unit-test", "cd.go.elastic-agent.docker", prop1, prop2);

            profile.validate(null);

            assertThat(profile.errors().size(), is(0));

            assertThat(prop1.errors().size(), is(1));
            assertThat(prop2.errors().size(), is(1));

            assertThat(prop1.errors().on("configurationKey"), is(format("Duplicate key 'USERNAME' found for %s 'docker.unit-test'", getObjectDescription())));
            assertThat(prop2.errors().on("configurationKey"), is(format("Duplicate key 'USERNAME' found for %s 'docker.unit-test'", getObjectDescription())));
        }
    }
}
