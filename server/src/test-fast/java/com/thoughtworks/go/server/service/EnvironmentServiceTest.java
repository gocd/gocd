/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.exceptions.NoSuchEnvironmentException;
import com.thoughtworks.go.domain.PipelinePauseInfo;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.presentation.pipelinehistory.Environment;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineModel;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;


public class EnvironmentServiceTest {
    @Mock
    private EnvironmentConfigService environmentConfigService;
    private EnvironmentService environmentService;
    @Mock
    private PipelineHistoryService pipelineHistoryService;
    @Mock
    private SystemEnvironment systemEnvironment;
    private static final Username USER_FOO = new Username(new CaseInsensitiveString("Foo"));

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        environmentService = new EnvironmentService(environmentConfigService, pipelineHistoryService, systemEnvironment);
        when(systemEnvironment.displayPipelineInstancesOnEnvironmentsPage()).thenReturn(true);
    }

    @Test
    public void shouldReturnPipelineHistoryForPipelinesInAnEnvironment() throws Exception {
        Username username = new Username(new CaseInsensitiveString("Foo"));

        when(environmentConfigService.pipelinesFor(new CaseInsensitiveString("uat"))).thenReturn(
                Arrays.asList(new CaseInsensitiveString("uat-pipeline"), new CaseInsensitiveString("staging-pipeline")));
        PipelineInstanceModel uatInstance = stubPipelineHistoryServiceToReturnPipelines("uat-pipeline");
        PipelineInstanceModel stagingInstance = stubPipelineHistoryServiceToReturnPipelines("staging-pipeline");
        ArrayList<Environment> environments = new ArrayList<>();

        environmentService.addEnvironmentFor(new CaseInsensitiveString("uat"), username, environments);

        assertThat(environments.size(), is(1));
        Environment environment = environments.get(0);
        assertThat(environment.getName(), is("uat"));
        List<PipelineModel> models = environment.getPipelineModels();
        assertThat(models.size(), is(2));
        PipelineModel model1 = new PipelineModel(uatInstance.getName(), true, true, PipelinePauseInfo.notPaused());
        model1.addPipelineInstance(uatInstance);
        assertThat(models, hasItem(model1));
        PipelineModel model2 = new PipelineModel(stagingInstance.getName(), true, true, PipelinePauseInfo.notPaused());
        model2.addPipelineInstance(stagingInstance);
        assertThat(models, hasItem(model2));
    }

    private void stubPipelineHistoryServiceToReturnPipelines(final String... pipelineNames) {
        for (String pipelineName : pipelineNames) {
            stubPipelineHistoryServiceToReturnPipelines(pipelineName);
        }
    }

    private PipelineInstanceModel stubPipelineHistoryServiceToReturnPipelines(final String pipelineName) {
        PipelineInstanceModel pipelineInstanceModel = PipelineInstanceModel.createPipeline(pipelineName, -1, "1", BuildCause.createManualForced(), new StageInstanceModels());
        PipelineModel pipelineModel = new PipelineModel(pipelineInstanceModel.getName(), true, true, PipelinePauseInfo.notPaused());
        pipelineModel.addPipelineInstance(pipelineInstanceModel);
        when(pipelineHistoryService.latestPipelineModel(new Username(new CaseInsensitiveString("Foo")), pipelineName)).thenReturn(pipelineModel);
        return pipelineInstanceModel;
    }

    @Test
    public void shouldReturnAllTheEnvironments() throws Exception {
        when(environmentConfigService.environmentNames()).thenReturn(Arrays.asList(new CaseInsensitiveString("uat"), new CaseInsensitiveString("preprod")));
        when(environmentConfigService.pipelinesFor(new CaseInsensitiveString("uat"))).thenReturn(Arrays.asList(new CaseInsensitiveString("uat-pipeline"), new CaseInsensitiveString("staging-pipeline")));
        when(environmentConfigService.pipelinesFor(new CaseInsensitiveString("preprod"))).thenReturn(Arrays.asList(new CaseInsensitiveString("preprod-pipeline")));
        stubPipelineHistoryServiceToReturnPipelines("uat-pipeline", "preprod-pipeline", "staging-pipeline");

        List<Environment> environments = environmentService.getEnvironments(USER_FOO);

        assertThat(environments.size(), is(2));
        assertThat(environments.get(0).getPipelineModels().size(), is(2));
        assertThat(environments.get(1).getPipelineModels().size(), is(1));
    }

    @Test
    public void shouldOmitEnvironmentsHavePipelinesConfiguredButHaveNoPermissionsOnThePipelines() throws Exception {
        when(environmentConfigService.environmentNames()).thenReturn(Arrays.asList(new CaseInsensitiveString("uat"), new CaseInsensitiveString("preprod")));
        when(environmentConfigService.pipelinesFor(new CaseInsensitiveString("uat"))).thenReturn(Arrays.asList(new CaseInsensitiveString("staging-pipeline")));
        when(environmentConfigService.pipelinesFor(new CaseInsensitiveString("preprod"))).thenReturn(Arrays.asList(new CaseInsensitiveString("preprod-pipeline")));

        stubPipelineHistoryServiceToReturnPipelines("preprod-pipeline");
        when(pipelineHistoryService.latest("staging-pipeline", USER_FOO)).thenReturn(null);

        List<Environment> environments = environmentService.getEnvironments(USER_FOO);

        assertThat(environments.size(), is(1));
        assertThat(environments.get(0).getPipelineModels().size(), is(1));
    }


    @Test
    public void shouldAddEnvironmentsThatHaveNoPipelinesConfigured() throws Exception {
        when(environmentConfigService.environmentNames()).thenReturn(Arrays.asList(new CaseInsensitiveString("uat"), new CaseInsensitiveString("preprod")));
        when(environmentConfigService.pipelinesFor(new CaseInsensitiveString("uat"))).thenReturn(new ArrayList<>());
        when(environmentConfigService.pipelinesFor(new CaseInsensitiveString("preprod"))).thenReturn(Arrays.asList(new CaseInsensitiveString("preprod-pipeline")));
        stubPipelineHistoryServiceToReturnPipelines("preprod-pipeline");

        List<Environment> environments = environmentService.getEnvironments(USER_FOO);

        assertThat(environments.size(), is(2));
        assertThat(environments.get(0).getPipelineModels().size(), is(0));
        assertThat(environments.get(1).getPipelineModels().size(), is(1));
    }

    @Test
    public void shouldNotReturnPipelineInstancesWithEnvironmentsIfDisplayPipelineInstancesOnEnvironmentsPageFlagIsTurnedOff() throws NoSuchEnvironmentException {
        when(systemEnvironment.displayPipelineInstancesOnEnvironmentsPage()).thenReturn(false);

        when(environmentConfigService.environmentNames()).thenReturn(Arrays.asList(new CaseInsensitiveString("uat"), new CaseInsensitiveString("preprod")));
        when(environmentConfigService.pipelinesFor(new CaseInsensitiveString("uat"))).thenReturn(Arrays.asList(new CaseInsensitiveString("uat-pipeline"), new CaseInsensitiveString("staging-pipeline")));
        when(environmentConfigService.pipelinesFor(new CaseInsensitiveString("preprod"))).thenReturn(Arrays.asList(new CaseInsensitiveString("preprod-pipeline")));
        stubPipelineHistoryServiceToReturnPipelines("uat-pipeline", "preprod-pipeline", "staging-pipeline");

        List<Environment> environments = environmentService.getEnvironments(USER_FOO);

        assertThat(environments.size(), is(2));
        assertTrue(environments.get(0).getPipelineModels().isEmpty() );
        assertTrue(environments.get(1).getPipelineModels().isEmpty());

    }

}
