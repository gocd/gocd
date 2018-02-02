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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.security.Permissions;
import com.thoughtworks.go.config.security.users.AllowedUsers;
import com.thoughtworks.go.config.security.users.Everyone;
import com.thoughtworks.go.config.security.users.NoOne;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.dashboard.*;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.domain.user.PipelineSelections;
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService;
import com.thoughtworks.go.server.service.support.toggle.Toggles;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;

import static com.thoughtworks.go.server.dashboard.GoDashboardPipelineMother.pipeline;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class GoDashboardServiceTest {
    @Mock
    private GoDashboardCache cache;
    @Mock
    private GoDashboardCurrentStateLoader dashboardCurrentStateLoader;
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private FeatureToggleService featureToggleService;
    @Mock
    private GoDashboardPipelines pipelines;

    private GoDashboardService service;

    private GoConfigMother configMother;
    private CruiseConfig config;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        configMother = new GoConfigMother();
        config = configMother.defaultCruiseConfig();
        Toggles.initializeWith(featureToggleService);
        when(featureToggleService.isToggleOn(Toggles.QUICKER_DASHBOARD_KEY)).thenReturn(true);
        when(cache.allEntries()).thenReturn(this.pipelines);
        service = new GoDashboardService(cache, dashboardCurrentStateLoader, goConfigService);
    }

    @Test
    public void shouldUpdateCacheForPipelineGivenItsName() throws Exception {
        PipelineConfig pipelineConfig = configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        PipelineConfigs groupConfig = config.findGroup("group1");
        GoDashboardPipeline pipeline = pipeline("pipeline1");

        when(goConfigService.findGroupByPipeline(new CaseInsensitiveString("pipeline1"))).thenReturn(groupConfig);
        when(dashboardCurrentStateLoader.pipelineFor(pipelineConfig, groupConfig)).thenReturn(pipeline);

        service.updateCacheForPipeline(new CaseInsensitiveString("pipeline1"));

        verify(cache).put(pipeline);
    }

    @Test
    public void shouldUpdateCacheForPipelineGivenItsConfig() throws Exception {
        PipelineConfig pipelineConfig = configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        PipelineConfigs groupConfig = config.findGroup("group1");
        GoDashboardPipeline pipeline = pipeline("pipeline1");

        when(goConfigService.findGroupByPipeline(new CaseInsensitiveString("pipeline1"))).thenReturn(groupConfig);
        when(dashboardCurrentStateLoader.pipelineFor(pipelineConfig, groupConfig)).thenReturn(pipeline);

        service.updateCacheForPipeline(pipelineConfig);

        verify(cache).put(pipeline);
    }

    @Test
    public void shouldUpdateCacheForAllPipelinesInAGivenConfig() throws Exception {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        configMother.addPipelineWithGroup(config, "group1", "pipeline2", "stage1", "job1");
        GoDashboardPipeline pipeline1 = pipeline("pipeline1");
        GoDashboardPipeline pipeline2 = pipeline("pipeline2");

        List<GoDashboardPipeline> pipelines = asList(pipeline1, pipeline2);
        when(dashboardCurrentStateLoader.allPipelines(config)).thenReturn(pipelines);

        service.updateCacheForAllPipelinesIn(config);

        verify(cache).replaceAllEntriesInCacheWith(pipelines);
    }

    @Test
    public void allPipelineGroupsForDashboard_shouldRetrieveTheLatestKnownSetOfPipelinesFromTheCache() throws Exception {
        PipelineSelections pipelineSelections = mock(PipelineSelections.class);

        configMother.addPipelineWithGroup(config, "group1", "pipeline2", "stage1A", "job1A1");
        GoDashboardPipeline pipeline2 = pipeline("pipeline2", "group1");

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1");
        GoDashboardPipeline pipeline1 = pipeline("pipeline1", "group1");

        addPipelinesToCache(pipeline1, pipeline2);
        when(pipelineSelections.includesPipeline(any(PipelineConfig.class))).thenReturn(true);

        List<GoDashboardPipelineGroup> pipelineGroups = allPipelineGroupsForDashboard(pipelineSelections, new Username("user1"));

        assertThat(pipelineGroups.size(), is(1));
        assertThat(pipelineGroups.get(0).allPipelineNames(), contains("pipeline1", "pipeline2"));
        assertThat(pipelineGroups.get(0).allPipelines(), contains(pipeline1, pipeline2));
    }

    @Test
    public void allPipelineGroupsForDashboard_shouldRetrieveOnlyPipelineGroupsViewableByTheUser() throws Exception {
        PipelineSelections pipelineSelections = mock(PipelineSelections.class);

        PipelineConfig pipelineConfig4 = configMother.addPipelineWithGroup(config, "group2", "pipeline4", "stage1A", "job1A1");
        GoDashboardPipeline pipeline4 = pipeline("pipeline4", "group2");

        PipelineConfig pipelineConfig3 = configMother.addPipelineWithGroup(config, "group2", "pipeline3", "stage1A", "job1A1");
        GoDashboardPipeline pipeline3 = pipeline("pipeline3", "group2");

        PipelineConfig pipelineConfig2 = configMother.addPipelineWithGroup(config, "group1", "pipeline2", "stage1A", "job1A1");
        GoDashboardPipeline pipeline2 = pipeline("pipeline2", "group1");

        PipelineConfig pipelineConfig1 = configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1");
        GoDashboardPipeline pipeline1 = pipeline("pipeline1", "group1");

        addPipelinesToCache(pipeline1, pipeline2, pipeline3, pipeline4);
        when(pipelineSelections.includesPipeline(pipelineConfig1)).thenReturn(true);
        when(pipelineSelections.includesPipeline(pipelineConfig2)).thenReturn(false);
        when(pipelineSelections.includesPipeline(pipelineConfig3)).thenReturn(true);
        when(pipelineSelections.includesPipeline(pipelineConfig4)).thenReturn(false);

        List<GoDashboardPipelineGroup> pipelineGroups = allPipelineGroupsForDashboard(pipelineSelections, new Username("user1"));

        assertThat(pipelineGroups.size(), is(2));
        assertThat(pipelineGroups.get(0).allPipelineNames(), contains("pipeline1"));
        assertThat(pipelineGroups.get(0).allPipelines(), contains(pipeline1));

        assertThat(pipelineGroups.get(1).allPipelineNames(), contains("pipeline3"));
        assertThat(pipelineGroups.get(1).allPipelines(), contains(pipeline3));
    }

    @Test
    public void allPipelineGroupsForDashboard_shouldRetrievePipelineGroupsBasedOnUsersPipelineSelections() throws Exception {
        PipelineSelections pipelineSelections = mock(PipelineSelections.class);

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1");
        GoDashboardPipeline pipeline1 = pipeline("pipeline1", "group1", new Permissions(new AllowedUsers(Collections.singleton("user1"), Collections.emptySet()),
                NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE));

        configMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage1A", "job1A1");
        GoDashboardPipeline pipeline2 = pipeline("pipeline2", "group2", new Permissions(NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE));

        addPipelinesToCache(pipeline1, pipeline2);
        when(pipelineSelections.includesPipeline(any(PipelineConfig.class))).thenReturn(true);

        List<GoDashboardPipelineGroup> pipelineGroups = allPipelineGroupsForDashboard(pipelineSelections, new Username("user1"));

        assertThat(pipelineGroups.size(), is(1));
        assertThat(pipelineGroups.get(0).allPipelineNames(), contains("pipeline1"));
        assertThat(pipelineGroups.get(0).allPipelines(), contains(pipeline1));
    }

    @Test
    public void allPipelineGroupsForDashboard_shouldNotListEmptyPipelineGroup() throws Exception {
        PipelineSelections pipelineSelections = mock(PipelineSelections.class);
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1");

        when(pipelineSelections.includesPipeline(any(PipelineConfig.class))).thenReturn(true);

        List<GoDashboardPipelineGroup> pipelineGroups = allPipelineGroupsForDashboard(pipelineSelections, new Username("user1"));

        assertThat(pipelineGroups.size(), is(0));
    }

    @Test
    public void allPipelineGroupsForDashboard_shouldNotListPipelinesExistingInConfigButNotInCache() throws Exception {
        PipelineSelections pipelineSelections = mock(PipelineSelections.class);

        configMother.addPipelineWithGroup(config, "group1", "pipeline2", "stage1A", "job1A1");

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1");
        GoDashboardPipeline pipeline1 = pipeline("pipeline1", "group1", new Permissions(Everyone.INSTANCE,
                Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE));


        addPipelinesToCache(pipeline1);
        when(pipelineSelections.includesPipeline(any(PipelineConfig.class))).thenReturn(true);

        List<GoDashboardPipelineGroup> pipelineGroups = allPipelineGroupsForDashboard(pipelineSelections, new Username("user1"));

        assertThat(pipelineGroups.size(), is(1));
        assertThat(pipelineGroups.get(0).allPipelineNames(), contains("pipeline1"));
        assertThat(pipelineGroups.get(0).allPipelines(), contains(pipeline1));
    }

    private List<GoDashboardPipelineGroup> allPipelineGroupsForDashboard(PipelineSelections pipelineSelections, Username username) {
        when(goConfigService.groups()).thenReturn(config.getGroups());

        return service.allPipelineGroupsForDashboard(pipelineSelections, username);
    }

    private void addPipelinesToCache(GoDashboardPipeline... pipelines) {
        for (GoDashboardPipeline pipeline : pipelines) {
            when(this.pipelines.find(pipeline.name())).thenReturn(pipeline);
        }
    }
}
