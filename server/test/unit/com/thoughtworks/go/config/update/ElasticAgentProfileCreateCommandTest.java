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

package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ElasticAgentProfileCreateCommandTest {

    private ElasticAgentExtension elasticAgentExtension;

    @Before
    public void setUp() throws Exception {
        elasticAgentExtension = mock(ElasticAgentExtension.class);
    }

    @Test
    public void shouldAddElasticProfile() throws Exception {
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        ElasticProfile elasticProfile = new ElasticProfile("foo", "docker");
        ElasticAgentProfileCreateCommand command = new ElasticAgentProfileCreateCommand(elasticProfile, null, elasticAgentExtension, null, null);
        command.update(cruiseConfig);

        assertThat(cruiseConfig.server().getElasticConfig().getProfiles().find("foo"), equalTo(elasticProfile));
    }

    @Test
    public void shouldInvokePluginValidationsBeforeSave() throws Exception {
        ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("key", "error"));
        when(elasticAgentExtension.validate(eq("aws"), Matchers.<Map<String,String>>any())).thenReturn(validationResult);
        ElasticProfile newProfile = new ElasticProfile("foo", "aws", new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue("val")));
        ElasticAgentProfileCreateCommand command = new ElasticAgentProfileCreateCommand(newProfile, mock(GoConfigService.class), elasticAgentExtension, null, new HttpLocalizedOperationResult());
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        command.update(cruiseConfig);
        command.isValid(cruiseConfig);
        assertThat(newProfile.first().errors().size(), is(1));
        assertThat(newProfile.first().errors().asString(), is("error"));
    }

}
