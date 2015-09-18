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

import java.io.IOException;

import com.thoughtworks.go.config.MergedGoConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class PipelinePauseServiceIntegrationTest {

    @Autowired PipelinePauseService pipelinePauseService;
    @Autowired
    GoConfigService goConfigService;
    @Autowired
    MergedGoConfig mergedGoConfig;
    private GoConfigFileHelper configHelper = new GoConfigFileHelper();

    @Before
    public void setUp() throws IOException {
        configHelper.onSetUp();
    }

    @After
    public void tearDown() {
        configHelper.onTearDown();
    }

    @Test
    public void shouldTruncatePauseMessageIfGreaterThanAllowedLength() throws Exception {
        String name = "pipeline-name";
        CruiseConfig cruiseConfig = GoConfigMother.configWithPipelines(name);
        configHelper.writeConfigFile(cruiseConfig);
        mergedGoConfig.forceReload();

        Username userName = new Username(new CaseInsensitiveString("UserFoo"));
        pipelinePauseService.pause(name, "tiny pause cause", userName);
        assertThat(pipelinePauseService.pipelinePauseInfo(name).getPauseCause(), is("tiny pause cause"));

        String stringWith255Chars = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        pipelinePauseService.pause(name, stringWith255Chars + "aa", userName);
        assertThat(pipelinePauseService.pipelinePauseInfo(name).getPauseCause(), is(stringWith255Chars));
    }

    @Test
    public void shouldUnpauseAPausedPipeline() throws Exception {
        String name = "pipeline-name";
        CruiseConfig cruiseConfig = GoConfigMother.configWithPipelines(name);
        configHelper.writeConfigFile(cruiseConfig);
        mergedGoConfig.forceReload();

        Username userName = new Username(new CaseInsensitiveString("UserFoo"));
        pipelinePauseService.pause(name, "pause for testing", userName);
        assertThat(pipelinePauseService.pipelinePauseInfo(name).getPauseCause(), is("pause for testing"));
        assertThat(pipelinePauseService.pipelinePauseInfo(name).isPaused(), is(true));

        pipelinePauseService.unpause(name);

        assertThat(pipelinePauseService.pipelinePauseInfo(name).isPaused(), is(false));

    }
}
