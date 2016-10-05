/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.elastic;

import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ElasticProfileTest {

    @Test
    public void shouldNotAllowNullPluginIdOrProfileId() throws Exception {
        ElasticProfile profile = new ElasticProfile();

        profile.validate(null);
        assertThat(profile.errors().size(), is(2));
        assertThat(profile.errors().on(ElasticProfile.PLUGIN_ID), is("Elastic profile cannot have a blank plugin id."));
        assertThat(profile.errors().on(ElasticProfile.ID), is("Elastic profile cannot have a blank id."));
    }

    @Test
    public void shouldValidateElasticPluginIdPattern() throws Exception {
        ElasticProfile profile = new ElasticProfile("!123", "docker");
        profile.validate(null);
        assertThat(profile.errors().size(), is(1));
        assertThat(profile.errors().on(ElasticProfile.ID), is("Invalid id '!123'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    @Test
    public void shouldValidateConfigPropertyNameUniqueness() throws Exception {
        ConfigurationProperty prop1 = ConfigurationPropertyMother.create("USERNAME");
        ConfigurationProperty prop2 = ConfigurationPropertyMother.create("USERNAME");
        ElasticProfile profile = new ElasticProfile("docker.unit-test", "cd.go.elastic-agent.docker", prop1, prop2);

        profile.validate(null);

        assertThat(profile.errors().size(), is(0));

        assertThat(prop1.errors().size(), is(1));
        assertThat(prop2.errors().size(), is(1));

        assertThat(prop1.errors().on(ConfigurationProperty.CONFIGURATION_KEY), is("Duplicate key 'USERNAME' found for elastic profile 'docker.unit-test'"));
        assertThat(prop2.errors().on(ConfigurationProperty.CONFIGURATION_KEY), is("Duplicate key 'USERNAME' found for elastic profile 'docker.unit-test'"));
    }

}
