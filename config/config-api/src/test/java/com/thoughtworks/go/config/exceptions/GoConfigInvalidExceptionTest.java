/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.config.exceptions;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class GoConfigInvalidExceptionTest {
    @Test
    public void shouldReturnAllErrorMessagesOnCruiseConfig(){
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.addError("BasicCruiseConfig_key", "BasicCruiseConfig_error");
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("");
        pipelineConfig.addError("PipelineConfig_key", "PipelineConfig_error");
        cruiseConfig.addPipeline("default", pipelineConfig);

        GoConfigInvalidException exception = new GoConfigInvalidException(cruiseConfig, "");
        assertThat(exception.getAllErrorMessages(), is("BasicCruiseConfig_error, PipelineConfig_error"));
    }

}