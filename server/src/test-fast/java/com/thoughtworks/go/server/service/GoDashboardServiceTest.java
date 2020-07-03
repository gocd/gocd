/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.security.GoConfigPipelinePermissionsAuthority;
import com.thoughtworks.go.config.security.Permissions;
import com.thoughtworks.go.config.security.permissions.EveryonePermission;
import com.thoughtworks.go.config.security.permissions.NoOnePermission;
import com.thoughtworks.go.config.security.users.AllowedUsers;
import com.thoughtworks.go.config.security.users.Everyone;
import com.thoughtworks.go.config.security.users.NoOne;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.dashboard.*;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.domain.user.BlacklistFilter;
import com.thoughtworks.go.server.domain.user.DashboardFilter;
import com.thoughtworks.go.server.domain.user.Filters;
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
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class GoDashboardServiceTest {
    @Mock private GoDashboardCache cache;
    @Mock private GoDashboardCurrentStateLoader dashboardCurrentStateLoader;
    @Mock private GoConfigPipelinePermissionsAuthority permissionsAuthority;
    @Mock private GoConfigService goConfigService;
    @Mock private FeatureToggleService featureToggleService;
    @Mock private GoDashboardPipelines pipelines;

    private GoDashboardService service;

    private GoConfigMother configMother;
    private CruiseConfig config;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        configMother = new GoConfigMother();
        config = GoConfigMother.defaultCruiseConfig();
        Toggles.initializeWith(featureToggleService);
        when(cache.allEntries()).thenReturn(this.pipelines);
        service = new GoDashboardService(cache, dashboardCurrentStateLoader, permissionsAuthority, goConfigService);

        GoConfigMother.addUserAsSuperAdmin(config, "superduper");
        configMother.addRoleAsSuperAdmin(config, "supers");
    }

    @Test
    public void shouldUpdateCacheForPipelineGivenItsName() {
        PipelineConfig pipelineConfig = configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        PipelineConfigs groupConfig = config.findGroup("group1");
        GoDashboardPipeline pipeline = pipeline("pipeline1");

        when(goConfigService.findGroupByPipeline(new CaseInsensitiveString("pipeline1"))).thenReturn(groupConfig);
        when(dashboardCurrentStateLoader.pipelineFor(pipelineConfig, groupConfig)).thenReturn(pipeline);

        service.updateCacheForPipeline(new CaseInsensitiveString("pipeline1"));

        verify(cache).put(pipeline);
    }

    @Test
    public void shouldUpdateCacheForPipelineGivenItsConfig() {
        PipelineConfig pipelineConfig = configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        PipelineConfigs groupConfig = config.findGroup("group1");
        GoDashboardPipeline pipeline = pipeline("pipeline1");

        when(goConfigService.findGroupByPipeline(new CaseInsensitiveString("pipeline1"))).thenReturn(groupConfig);
        when(dashboardCurrentStateLoader.pipelineFor(pipelineConfig, groupConfig)).thenReturn(pipeline);

        service.updateCacheForPipeline(pipelineConfig);

        verify(cache).put(pipeline);
    }

    @Test
    public void shouldUpdateCacheForAllPipelinesInAGivenConfig() {
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
    public void allPipelineGroupsForDashboard_shouldRetrieveTheLatestKnownSetOfPipelinesFromTheCache() {
        configMother.addPipelineWithGroup(config, "group1", "pipeline2", "stage1A", "job1A1");
        GoDashboardPipeline pipeline2 = pipeline("pipeline2", "group1");

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1");
        GoDashboardPipeline pipeline1 = pipeline("pipeline1", "group1");

        addPipelinesToCache(pipeline1, pipeline2);

        List<GoDashboardPipelineGroup> pipelineGroups = allPipelineGroupsForDashboard(Filters.WILDCARD_FILTER, new Username("user1"));

        assertThat(pipelineGroups.size(), is(1));
        assertThat(pipelineGroups.get(0).pipelines(), contains("pipeline1", "pipeline2"));
        assertThat(pipelineGroups.get(0).allPipelines(), contains(pipeline1, pipeline2));
    }

    @Test
    public void allEnvironmentsForDashboard_shouldRetrieveTheLatestKnownSetOfPipelinesFromTheCache() {
        configMother.addPipelineWithGroup(config, "group1", "pipeline2", "stage1A", "job1A1");
        GoDashboardPipeline pipeline2 = pipeline("pipeline2", "group1");

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1");
        GoDashboardPipeline pipeline1 = pipeline("pipeline1", "group1");

        addPipelinesToCache(pipeline1, pipeline2);

        configMother.addEnvironmentConfig(config, "env1", "pipeline1", "pipeline2");
        List<GoDashboardEnvironment> envs = allEnvironmentsForDashboard(Filters.WILDCARD_FILTER, new Username("user1"));

        assertThat(envs.size(), is(1));
        assertThat(envs.get(0).pipelines(), contains("pipeline1", "pipeline2"));
    }

    @Test
    public void allPipelineGroupsForDashboard_shouldRetrieveOnlyPipelineGroupsViewableByTheUser() {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1");
        GoDashboardPipeline pipeline1 = pipeline("pipeline1", "group1", new Permissions(new AllowedUsers(Collections.singleton("user1"), Collections.emptySet()),
                NoOne.INSTANCE, NoOne.INSTANCE, NoOnePermission.INSTANCE));

        configMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage1A", "job1A1");
        GoDashboardPipeline pipeline2 = pipeline("pipeline2", "group2", new Permissions(NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE, NoOnePermission.INSTANCE));

        addPipelinesToCache(pipeline1, pipeline2);

        List<GoDashboardPipelineGroup> pipelineGroups = allPipelineGroupsForDashboard(Filters.WILDCARD_FILTER, new Username("user1"));

        assertThat(pipelineGroups.size(), is(1));
        assertThat(pipelineGroups.get(0).pipelines(), contains("pipeline1"));
    }

    @Test
    public void allEnvironmentsForDashboard_shouldRetrieveOnlyPipelinesViewableByTheUser() {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1");
        GoDashboardPipeline pipeline1 = pipeline("pipeline1", "group1", new Permissions(new AllowedUsers(Collections.singleton("user1"), Collections.emptySet()),
                NoOne.INSTANCE, NoOne.INSTANCE, NoOnePermission.INSTANCE));

        configMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage1A", "job1A1");
        GoDashboardPipeline pipeline2 = pipeline("pipeline2", "group2", new Permissions(NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE, NoOnePermission.INSTANCE));

        addPipelinesToCache(pipeline1, pipeline2);

        configMother.addEnvironmentConfig(config, "env1", "pipeline1", "pipeline2");
        List<GoDashboardEnvironment> envs = allEnvironmentsForDashboard(Filters.WILDCARD_FILTER, new Username("user1"));

        assertThat(envs.size(), is(1));
        assertThat(envs.get(0).pipelines(), contains("pipeline1"));
    }

    @Test
    public void allPipelineGroupsForDashboard_shouldRetrievePipelineGroupsBasedOnDashboardFilters() {
        DashboardFilter filter = new BlacklistFilter("foo", CaseInsensitiveString.list("pipeline2", "pipeline4"), Collections.emptySet());

        configMother.addPipelineWithGroup(config, "group2", "pipeline4", "stage1A", "job1A1");
        GoDashboardPipeline pipeline4 = pipeline("pipeline4", "group2");

        configMother.addPipelineWithGroup(config, "group2", "pipeline3", "stage1A", "job1A1");
        GoDashboardPipeline pipeline3 = pipeline("pipeline3", "group2");

        configMother.addPipelineWithGroup(config, "group1", "pipeline2", "stage1A", "job1A1");
        GoDashboardPipeline pipeline2 = pipeline("pipeline2", "group1");

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1");
        GoDashboardPipeline pipeline1 = pipeline("pipeline1", "group1");

        addPipelinesToCache(pipeline1, pipeline2, pipeline3, pipeline4);

        List<GoDashboardPipelineGroup> pipelineGroups = allPipelineGroupsForDashboard(filter, new Username("user1"));

        assertThat(pipelineGroups.size(), is(2));
        assertThat(pipelineGroups.get(0).pipelines(), contains("pipeline1"));
        assertThat(pipelineGroups.get(1).pipelines(), contains("pipeline3"));
    }

    @Test
    public void allEnvironmentsForDashboard_shouldRetrievePipelineGroupsBasedOnDashboardFilters() {
        DashboardFilter filter = new BlacklistFilter("foo", CaseInsensitiveString.list("pipeline2", "pipeline4"), Collections.emptySet());

        configMother.addPipelineWithGroup(config, "group2", "pipeline4", "stage1A", "job1A1");
        GoDashboardPipeline pipeline4 = pipeline("pipeline4", "group2");

        configMother.addPipelineWithGroup(config, "group2", "pipeline3", "stage1A", "job1A1");
        GoDashboardPipeline pipeline3 = pipeline("pipeline3", "group2");

        configMother.addPipelineWithGroup(config, "group1", "pipeline2", "stage1A", "job1A1");
        GoDashboardPipeline pipeline2 = pipeline("pipeline2", "group1");

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1");
        GoDashboardPipeline pipeline1 = pipeline("pipeline1", "group1");

        addPipelinesToCache(pipeline1, pipeline2, pipeline3, pipeline4);

        configMother.addEnvironmentConfig(config, "env1", "pipeline1", "pipeline2");
        configMother.addEnvironmentConfig(config, "env2", "pipeline3", "pipeline4");
        List<GoDashboardEnvironment> envs = allEnvironmentsForDashboard(filter, new Username("user1"));

        assertThat(envs.size(), is(2));
        assertThat(envs.get(0).pipelines(), contains("pipeline1"));

        assertThat(envs.get(1).pipelines(), contains("pipeline3"));
    }

    @Test
    public void allPipelineGroupsForDashboard_shouldNotListEmptyPipelineGroup() {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1");
        addPipelinesToCache(pipeline("pipeline1", "group1", new Permissions(NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE, NoOnePermission.INSTANCE)));

        List<GoDashboardPipelineGroup> pipelineGroups = allPipelineGroupsForDashboard(Filters.WILDCARD_FILTER, new Username("user1"));

        assertThat(pipelineGroups.size(), is(0));
    }

    @Test
    public void allEnvironmentsForDashboard_shouldNotListEmptyPipelineGroup() {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1");
        addPipelinesToCache(pipeline("pipeline1", "group1", new Permissions(NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE, NoOnePermission.INSTANCE)));

        configMother.addEnvironmentConfig(config, "env1", "pipeline1");
        List<GoDashboardEnvironment> pipelineGroups = allEnvironmentsForDashboard(Filters.WILDCARD_FILTER, new Username("user1"));

        assertThat(pipelineGroups.size(), is(0));
    }

    @Test
    public void allPipelineGroupsForDashboard_shouldNotListPipelinesExistingInConfigButNotInCache() {
        configMother.addPipelineWithGroup(config, "group1", "pipeline2", "stage1A", "job1A1");

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1");
        GoDashboardPipeline pipeline1 = pipeline("pipeline1", "group1", new Permissions(Everyone.INSTANCE,
                Everyone.INSTANCE, Everyone.INSTANCE, EveryonePermission.INSTANCE));

        addPipelinesToCache(pipeline1);

        List<GoDashboardPipelineGroup> pipelineGroups = allPipelineGroupsForDashboard(Filters.WILDCARD_FILTER, new Username("user1"));

        assertThat(pipelineGroups.size(), is(1));
        assertThat(pipelineGroups.get(0).pipelines(), contains("pipeline1"));
        assertThat(pipelineGroups.get(0).pipelines(), not(contains("pipeline2")));
    }

    @Test
    public void allEnvironmentsForDashboard_shouldNotListPipelinesExistingInConfigButNotInCache() {
        configMother.addPipelineWithGroup(config, "group1", "pipeline2", "stage1A", "job1A1");

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1");
        GoDashboardPipeline pipeline1 = pipeline("pipeline1", "group1", new Permissions(Everyone.INSTANCE,
                Everyone.INSTANCE, Everyone.INSTANCE, EveryonePermission.INSTANCE));

        addPipelinesToCache(pipeline1);
        configMother.addEnvironmentConfig(config, "env1", "pipeline1", "pipeline2");

        List<GoDashboardEnvironment> envs = allEnvironmentsForDashboard(Filters.WILDCARD_FILTER, new Username("user1"));

        assertThat(envs.size(), is(1));
        assertThat(envs.get(0).pipelines(), contains("pipeline1"));
        assertThat(envs.get(0).pipelines(), not(contains("pipeline2")));
    }

    @Test
    public void shouldRemoveExistingPipelineEntryInCacheWhenPipelineConfigIsRemoved() {
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

    @Test
    public void shouldRemoveExistingPipelineEntryInCacheWhenPipelineIsRemoved() {
        BasicCruiseConfig config = GoConfigMother.defaultCruiseConfig();
        PipelineConfig pipelineConfig = new GoConfigMother().addPipeline(config, "pipeline1", "stage1", "job1");
        config.findGroupOfPipeline(pipelineConfig).remove(pipelineConfig);

        when(goConfigService.findGroupByPipeline(any())).thenReturn(null);
        // simulate the event
        service.updateCacheForPipeline(pipelineConfig.name());
        verify(cache).remove(pipelineConfig.getName());
        verify(dashboardCurrentStateLoader).clearEntryFor(pipelineConfig.getName());
        verifyZeroInteractions(dashboardCurrentStateLoader);
    }

    private List<GoDashboardEnvironment> allEnvironmentsForDashboard(DashboardFilter filter, Username username) {
        when(goConfigService.getEnvironments()).thenReturn(config.getEnvironments());
        when(goConfigService.security()).thenReturn(config.server().security());

        return service.allEnvironmentsForDashboard(filter, username);
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
