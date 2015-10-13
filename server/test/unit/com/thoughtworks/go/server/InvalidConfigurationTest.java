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

package com.thoughtworks.go.server;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class InvalidConfigurationTest {
    private GoConfigDao goConfigDao = GoConfigFileHelper.createTestingDao();

    private void useConfig(String configContents) throws Exception {
        GoConfigFileHelper goConfigFileHelper = new GoConfigFileHelper(configContents);
        goConfigFileHelper.initializeConfigFile();
    }

    @After
    public void teardown() {
    }


    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionIfMultiplePipelinesExistWithTheSameName() throws Exception {
        useConfig(ConfigFileFixture.PIPELINES_WITH_SAME_NAME);
        goConfigDao.load();
    }

    @Test
    public void shouldThrowExceptionIfBuildPlansExistWithTheSameNameWithinAPipeline() throws Exception {
        try {
            useConfig(ConfigFileFixture.JOBS_WITH_SAME_NAME);
            goConfigDao.load();
            fail("Should throw Exception about duplicated job name");
        } catch (Exception e) {
            assertThat(
                    e.getMessage(),
                    containsString(
                            "Duplicate unique value [unit] declared "
                                    + "for identity constraint \"uniqueJob\" of element \"jobs\""));
        }
    }


    @Test
    public void shouldThrowExceptionIfPipelineDoesNotContainAnyBuildPlans() throws Exception {
        try {
            useConfig(ConfigFileFixture.STAGE_WITH_NO_JOBS);
            goConfigDao.load();
            fail("Should throw Exception about duplicated job name");
        } catch (Exception e) {
            assertThat(
                    e.getMessage(),
                    containsString(
                            "The content of element 'jobs' is not complete. One of '{job}' is expected"));
        }

    }

}
