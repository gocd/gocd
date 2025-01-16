/*
 * Copyright 2022 ThoughtWorks, Inc.
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

package com.thoughtworks.go.spark.spa

import com.google.gson.Gson
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.PipelineAccessSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.spark.mocks.StubTemplateEngine
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import spark.ModelAndView

import static org.mockito.Mockito.when

@MockitoSettings(strictness = Strictness.LENIENT)
class BuildDetailControllerTest implements ControllerTrait<BuildDetailController>, SecurityServiceTrait {

    private static final Gson GSON = new Gson()

    @Override
    BuildDetailController createControllerInstance() {
        return new BuildDetailController(new SPAAuthenticationHelper(securityService, goConfigService), pipelineService, restfulService, jobInstanceDao, templateEngine)
    }

    String pipelineName = "up42"
    String stageName = "run-tests"
    String jobName = "java"
    String pipelineCounter = '42'
    String stageCounter = '41'

    @Nested
    class Index {

        @BeforeEach
        void setUp() {
            when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true)
        }

        @Nested
        class Security implements SecurityTestTrait, PipelineAccessSecurity {

            @Override
            String getControllerMethodUnderTest() {
                return "index"
            }

            @Override
            void makeHttpCall() {
                get(controller.controllerPath(pipelineName, pipelineCounter, stageName, stageCounter, jobName))
            }

            @Override
            String getPipelineName() {
                return BuildDetailControllerTest.this.pipelineName
            }
        }


    }

    @Test
    void 'should set appropriate meta information on view model'() {
        get(controller.controllerPath(pipelineName, pipelineCounter, stageName, stageCounter, jobName))

        String expectedBody = new StubTemplateEngine().render(
                new ModelAndView([
                        viewTitle: "${jobName} Job Details".toString(),
                        meta     : [
                                pipelineName   : pipelineName,
                                pipelineCounter: pipelineCounter,
                                stageName      : stageName,
                                stageCounter   : stageCounter,
                                jobName        : jobName
                        ]
                ], null)
        )

        assertThatResponse()
                .isOk()
                .hasBody(expectedBody)

    }

}

