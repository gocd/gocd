/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.config.validation;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.ConfigErrors;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class EnvironmentAgentValidatorTest {
    @Test
    public void shouldThrowExceptionIfValidationFails() throws Exception {
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        BasicEnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("env"));
        environmentConfig.addAgent("does_not_exist_1");
        environmentConfig.addAgent("does_not_exist_2");
        cruiseConfig.addEnvironment(environmentConfig);
        try {
            new EnvironmentAgentValidator().validate(cruiseConfig);
            fail("should have thrown exception");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Environment 'env' has an invalid agent uuid 'does_not_exist_1', Environment 'env' has an invalid agent uuid 'does_not_exist_2'"));
        }
    }

    @Test
    public void shouldNotThrowExceptionIfValidationPasses() throws Exception {
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        BasicEnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("env"));
        environmentConfig.addAgent("exists_1");
        cruiseConfig.addEnvironment(environmentConfig);
        cruiseConfig.agents().add(new AgentConfig("exists_1"));
        new EnvironmentAgentValidator().validate(cruiseConfig);
        assertThat(environmentConfig.getAgents().first().errors().isEmpty(), is(true));
    }

    @Test
    public void shouldReturnValidationErrorMessages(){
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        BasicEnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("env"));
        environmentConfig.addAgent("does_not_exist_1");
        environmentConfig.addAgent("does_not_exist_2");
        environmentConfig.addAgent("exists_3");
        cruiseConfig.addEnvironment(environmentConfig);
        cruiseConfig.agents().add(new AgentConfig("exists_3"));
        List<ConfigErrors> errors = new EnvironmentAgentValidator().validateConfig(cruiseConfig);
        assertThat(errors.size(), is(2));
        assertThat(errors.get(0).on(EnvironmentAgentConfig.UUID), is("Environment 'env' has an invalid agent uuid 'does_not_exist_1'"));
        assertThat(errors.get(1).on(EnvironmentAgentConfig.UUID), is("Environment 'env' has an invalid agent uuid 'does_not_exist_2'"));
    }
}