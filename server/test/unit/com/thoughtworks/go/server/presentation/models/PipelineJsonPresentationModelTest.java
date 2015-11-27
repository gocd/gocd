/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.presentation.models;

import com.thoughtworks.go.domain.PipelinePauseInfo;
import com.thoughtworks.go.util.JsonTester;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Map;

public class PipelineJsonPresentationModelTest {
    private PipelineJsonPresentationModel pipeline;

    @Before
    public void setUp() throws Exception {
        pipeline = new PipelineJsonPresentationModel("group", "connectfour",
                PipelinePauseInfo.paused("upgrading uat", "jez"), true,
                new ArrayList<StageJsonPresentationModel>());
    }

    @Test
    public void shouldReturnJson() {
        Map<String, Object> json = pipeline.toJson();
        new JsonTester(json).shouldContain(
                "{ 'name' : 'connectfour',"
                        + "  'group' : 'group',"
                        + "  'paused' : 'true',"
                        + "  'pauseCause' : 'upgrading uat',"
                        + "  'pauseBy' : 'jez',"
                        + "  'stages' : [],"
                        + "  'forcedBuild' : 'true'"
                        + "}"
        );
    }

    @Test
    public void shouldHaveCanForceStatus() throws Exception {
        pipeline.setCanForce(true);
        Map<String, Object> json = pipeline.toJson();
        new JsonTester(json).shouldContain(
                "{ 'name' : 'connectfour',"
                        + "  'paused' : 'true',"
                        + "  'pauseCause' : 'upgrading uat',"
                        + "  'pauseBy' : 'jez',"
                        + "  'stages' : [],"
                        + "  'forcedBuild' : 'true',"
                        + "  'canForce' : 'true'"
                        + "}"
        );
    }

    @Test
    public void shouldHaveCanPauseStatus() throws Exception {
        pipeline.setCanPause(true);
        Map<String, Object> json = pipeline.toJson();
        new JsonTester(json).shouldContain(
                "{ 'canPause' : 'true'}"
        );

        pipeline.setCanPause(false);
        json = pipeline.toJson();
        new JsonTester(json).shouldContain(
                "{ 'canPause' : 'false'}"
        );
    }
}



