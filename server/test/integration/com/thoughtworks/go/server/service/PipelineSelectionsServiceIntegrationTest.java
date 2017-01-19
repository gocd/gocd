/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class PipelineSelectionsServiceIntegrationTest {
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private PipelineSelectionsService pipelineSelectionsService;

    private GoConfigFileHelper configHelper;

    @Before
    public void setup() throws Exception {
        dbHelper.onSetUp();
    }

    @After
    public void tearDown() throws Exception {
        dbHelper.onTearDown();
    }

    @Test
    public void shouldUpdateExistingPipelineSelectionsForAnonymousUserIfItAlreadyExistsInsteadOfAddingDuplicates() {
        long firstSaveId = pipelineSelectionsService.persistSelectedPipelines(null, Arrays.asList("pipeline1", "pipeline2"), false);
        long secondSaveId = pipelineSelectionsService.persistSelectedPipelines(null, Arrays.asList("pipeline1"), false);

        assertThat(firstSaveId, is(secondSaveId));
        assertThat(pipelineSelectionsService.getSelectedPipelines(null).getSelections(), is("pipeline1"));
    }
}