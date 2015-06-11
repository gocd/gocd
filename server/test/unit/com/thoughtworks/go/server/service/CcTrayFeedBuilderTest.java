/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.service;

import java.util.ArrayList;

import com.thoughtworks.go.config.BasicPipelineConfigs;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.activity.CcTrayStatus;
import com.thoughtworks.go.domain.activity.ProjectStatus;
import com.thoughtworks.go.util.ClassMockery;
import com.thoughtworks.go.helper.PipelineConfigMother;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class CcTrayFeedBuilderTest {
    private CcTrayStatus ccTrayStatus;
    private Mockery context = new ClassMockery();
    private CcTrayFeedBuilder ccTrayFeedBuilder;
    private ArrayList<ProjectStatus> result;

    @Before
    public void setUp() {
        ccTrayStatus = context.mock(CcTrayStatus.class);
        ccTrayFeedBuilder = new CcTrayFeedBuilder(ccTrayStatus);
        result = new ArrayList<ProjectStatus>();
    }

    @Test
    public void shouldBuildCcTrayFeedForStageAndJob() {
        context.checking(new Expectations() {
            {
                one(ccTrayStatus).dumpProject("cruise :: dev", result);
                one(ccTrayStatus).dumpProject("cruise :: dev :: linux-firefox", result);
            }
        });
        PipelineConfig withStages = PipelineConfigMother.createPipelineConfig("cruise", "dev", "linux-firefox");
        PipelineConfigs group = new BasicPipelineConfigs(withStages);
        ccTrayFeedBuilder.visit(group);
    }
}
