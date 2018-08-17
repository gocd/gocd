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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.security.Permissions;
import com.thoughtworks.go.config.security.users.AllowedUsers;
import com.thoughtworks.go.config.security.users.Everyone;
import com.thoughtworks.go.config.security.users.NoOne;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.dashboard.*;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.domain.user.DashboardFilter;
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
        DashboardFilter filter = mock(DashboardFilter.class);

        configMother.addPipelineWithGroup(config, "group1", "pipeline2", "stage1A", "job1A1");
        GoDashboardPipeline pipeline2 = pipeline("pipeline2", "group1");

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1");
        GoDashboardPipeline pipeline1 = pipeline("pipeline1", "group1");

        addPipelinesToCache(pipeline1, pipeline2);
        when(filter.isPipelineVisible(any(CaseInsensitiveString.class), any())).thenReturn(true);

        List<GoDashboardPipelineGroup> pipelineGroups = allPipelineGroupsForDashboard(filter, new Username("user1"));

        assertThat(pipelineGroups.size(), is(1));
        assertThat(pipelineGroups.get(0).allPipelineNames(), contains("pipeline1", "pipeline2"));
        assertThat(pipelineGroups.get(0).allPipelines(), contains(pipeline1, pipeline2));
    }

    @Test
    public void allPipelineGroupsForDashboard_shouldRetrieveOnlyPipelineGroupsViewableByTheUser() throws Exception {
        DashboardFilter filter = mock(DashboardFilter.class);

        PipelineConfig pipelineConfig4 = configMother.addPipelineWithGroup(config, "group2", "pipeline4", "stage1A", "job1A1");
        GoDashboardPipeline pipeline4 = pipeline("pipeline4", "group2");

        PipelineConfig pipelineConfig3 = configMother.addPipelineWithGroup(config, "group2", "pipeline3", "stage1A", "job1A1");
        GoDashboardPipeline pipeline3 = pipeline("pipeline3", "group2");

        PipelineConfig pipelineConfig2 = configMother.addPipelineWithGroup(config, "group1", "pipeline2", "stage1A", "job1A1");
        GoDashboardPipeline pipeline2 = pipeline("pipeline2", "group1");

        PipelineConfig pipelineConfig1 = configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1");
        GoDashboardPipeline pipeline1 = pipeline("pipeline1", "group1");

        addPipelinesToCache(pipeline1, pipeline2, pipeline3, pipeline4);
        when(filter.isPipelineVisible(pipelineConfig1.name(), null)).thenReturn(true);
        when(filter.isPipelineVisible(pipelineConfig2.name(), null)).thenReturn(false);
        when(filter.isPipelineVisible(pipelineConfig3.name(), null)).thenReturn(true);
        when(filter.isPipelineVisible(pipelineConfig4.name(), null)).thenReturn(false);

        List<GoDashboardPipelineGroup> pipelineGroups = allPipelineGroupsForDashboard(filter, new Username("user1"));

        assertThat(pipelineGroups.size(), is(2));
        assertThat(pipelineGroups.get(0).allPipelineNames(), contains("pipeline1"));
        assertThat(pipelineGroups.get(0).allPipelines(), contains(pipeline1));

        assertThat(pipelineGroups.get(1).allPipelineNames(), contains("pipeline3"));
        assertThat(pipelineGroups.get(1).allPipelines(), contains(pipeline3));
    }

    @Test
    public void allPipelineGroupsForDashboard_shouldRetrievePipelineGroupsBasedOnUsersPipelineSelections() throws Exception {
        DashboardFilter filter = mock(DashboardFilter.class);

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1");
        GoDashboardPipeline pipeline1 = pipeline("pipeline1", "group1", new Permissions(new AllowedUsers(Collections.singleton("user1"), Collections.emptySet()),
                NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE));

        configMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage1A", "job1A1");
        GoDashboardPipeline pipeline2 = pipeline("pipeline2", "group2", new Permissions(NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE));

        addPipelinesToCache(pipeline1, pipeline2);
        when(filter.isPipelineVisible(any(CaseInsensitiveString.class), any())).thenReturn(true);

        List<GoDashboardPipelineGroup> pipelineGroups = allPipelineGroupsForDashboard(filter, new Username("user1"));

        assertThat(pipelineGroups.size(), is(1));
        assertThat(pipelineGroups.get(0).allPipelineNames(), contains("pipeline1"));
        assertThat(pipelineGroups.get(0).allPipelines(), contains(pipeline1));
    }

    @Test
    public void allPipelineGroupsForDashboard_shouldNotListEmptyPipelineGroup() throws Exception {
        DashboardFilter filter = mock(DashboardFilter.class);
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1");

        when(filter.isPipelineVisible(any(CaseInsensitiveString.class), any())).thenReturn(true);

        List<GoDashboardPipelineGroup> pipelineGroups = allPipelineGroupsForDashboard(filter, new Username("user1"));

        assertThat(pipelineGroups.size(), is(0));
    }

    @Test
    public void allPipelineGroupsForDashboard_shouldNotListPipelinesExistingInConfigButNotInCache() throws Exception {
        DashboardFilter filter = mock(DashboardFilter.class);

        configMother.addPipelineWithGroup(config, "group1", "pipeline2", "stage1A", "job1A1");

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1");
        GoDashboardPipeline pipeline1 = pipeline("pipeline1", "group1", new Permissions(Everyone.INSTANCE,
                Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE));

        addPipelinesToCache(pipeline1);
        when(filter.isPipelineVisible(any(CaseInsensitiveString.class), any())).thenReturn(true);

        List<GoDashboardPipelineGroup> pipelineGroups = allPipelineGroupsForDashboard(filter, new Username("user1"));

        assertThat(pipelineGroups.size(), is(1));
        assertThat(pipelineGroups.get(0).allPipelineNames(), contains("pipeline1"));
        assertThat(pipelineGroups.get(0).allPipelines(), contains(pipeline1));
    }

    @Test
    public void shouldRemoveExistingPipelineEntryInCacheWhenPipelineConfigIsRemoved() throws Exception {
        BasicCruiseConfig config = GoConfigMother.defaultCruiseConfig();
        PipelineConfig pipelineConfig = new GoConfigMother().addPipeline(config, "pipeline1", "stage1", "job1");
        config.findGroupOfPipeline(pipelineConfig).remove(pipelineConfig);

        when(goConfigService.findGroupByPipeline(any())).thenReturn(null);
        // simulate the event
        service.updateCacheForPipeline(pipelineConfig);
        verify(cache).remove(pipelineConfig.getName());
        verify(dashboardCurrentStateLoader).clearEntryFor(pipelineConfig.getName());
        verifyZeroInteractions(dashboardCurrentStateLoader);
    }

    private List<GoDashboardPipelineGroup> allPipelineGroupsForDashboard(DashboardFilter filter, Username username) {
        when(goConfigService.groups()).thenReturn(config.getGroups());

        return service.allPipelineGroupsForDashboard(filter, username);
    }

    private void addPipelinesToCache(GoDashboardPipeline... pipelines) {
        for (GoDashboardPipeline pipeline : pipelines) {
            when(this.pipelines.find(pipeline.name())).thenReturn(pipeline);
        }
    }
}
