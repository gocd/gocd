/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.presentation.CanDeleteResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.helper.EnvironmentConfigMother.environment;
import static com.thoughtworks.go.helper.PipelineConfigMother.createGroup;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class PipelineConfigServiceTest {

    private PipelineConfigService pipelineConfigService;
    private CruiseConfig cruiseConfig;
    private GoConfigService goConfigService;
    private GoCache goCache;
    private SecurityService securityService;

    @Before
    public void setUp() throws Exception {
        PipelineConfigs configs = createGroup("group", "pipeline", "in_env");
        downstream(configs);
        cruiseConfig = new BasicCruiseConfig(configs);
        cruiseConfig.addEnvironment(environment("foo", "in_env"));
        PipelineConfig remotePipeline = PipelineConfigMother.pipelineConfig("remote");
        remotePipeline.setOrigin(new RepoConfigOrigin(new ConfigRepoConfig(new GitMaterialConfig("url"),"plugin"),"1234"));
        cruiseConfig.addPipeline("group",remotePipeline);

        goConfigService = mock(GoConfigService.class);
        securityService = mock(SecurityService.class);
        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
        when(goConfigService.getConfigForEditing()).thenReturn(cruiseConfig);
        goCache = mock(GoCache.class);
        PipelineConfigurationCache.getInstance().onConfigChange(cruiseConfig);
        pipelineConfigService = new PipelineConfigService(goConfigService, goCache, securityService);

    }

    @Test
    public void shouldBeAbleToGetTheCanDeleteStatusOfAllPipelines() {

        Map<CaseInsensitiveString, CanDeleteResult> pipelineToCanDeleteIt = pipelineConfigService.canDeletePipelines();

        assertThat(pipelineToCanDeleteIt.size(), is(4));
        assertThat(pipelineToCanDeleteIt.get(new CaseInsensitiveString("down")), is(new CanDeleteResult(true, LocalizedMessage.string("CAN_DELETE_PIPELINE"))));
        assertThat(pipelineToCanDeleteIt.get(new CaseInsensitiveString("in_env")), is(new CanDeleteResult(false, LocalizedMessage.string("CANNOT_DELETE_PIPELINE_IN_ENVIRONMENT", new CaseInsensitiveString("in_env"), new CaseInsensitiveString("foo")))));
        assertThat(pipelineToCanDeleteIt.get(new CaseInsensitiveString("pipeline")), is(new CanDeleteResult(false, LocalizedMessage.string("CANNOT_DELETE_PIPELINE_USED_AS_MATERIALS", new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("down")))));
        assertThat(pipelineToCanDeleteIt.get(new CaseInsensitiveString("remote")), is(new CanDeleteResult(false, LocalizedMessage.string("CANNOT_DELETE_REMOTE_PIPELINE", new CaseInsensitiveString("remote"), "url at 1234"))));
    }

    @Test
    public void shouldGetPipelineConfigBasedOnName() {
        String pipelineName = "pipeline";
        PipelineConfigurationCache.getInstance().onConfigChange(cruiseConfig);
        PipelineConfig pipeline = pipelineConfigService.getPipelineConfig(pipelineName);
        assertThat(pipeline, is(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName))));
    }

    @Test
    public void shouldRemovePipelineConfigFromCacheWhenPresentOnPipelineConfigChange() {
        EntityConfigChangedListener<PipelineConfig> pipelineConfigChangeListener = getPipelineConfigEntityConfigChangedListener();
        when(goCache.get("GO_PIPELINE_CONFIGS_ETAGS_CACHE", "p")).thenReturn(new Object());
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("p"), new MaterialConfigs());

        pipelineConfigChangeListener.onEntityConfigChange(pipelineConfig);
        verify(goCache).remove("GO_PIPELINE_CONFIGS_ETAGS_CACHE", "p");
    }

    private EntityConfigChangedListener<PipelineConfig> getPipelineConfigEntityConfigChangedListener() {
        ArgumentCaptor<ConfigChangedListener> captor = ArgumentCaptor.forClass(ConfigChangedListener.class);
        doNothing().when(goConfigService).register(captor.capture());
        pipelineConfigService.initialize();
        List<ConfigChangedListener> listeners = captor.getAllValues();
        assertThat(listeners.get(1) instanceof EntityConfigChangedListener, is(true));
        EntityConfigChangedListener<PipelineConfig> pipelineConfigChangeListener= (EntityConfigChangedListener<PipelineConfig>) listeners.get(1);
        return pipelineConfigChangeListener;
    }

    @Test
    public void shouldNotRemovePipelineConfigFromCacheWhenNotPresentOnPipelineConfigChange(){
        EntityConfigChangedListener<PipelineConfig> pipelineConfigChangeListener = getPipelineConfigEntityConfigChangedListener();
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("p"), new MaterialConfigs());
        pipelineConfigChangeListener.onEntityConfigChange(pipelineConfig);
        verify(goCache, never()).remove("GO_PIPELINE_CONFIGS_ETAGS_CACHE", "p");
    }

    @Test
    public void shouldRemovePipelineConfigsFromCacheWhenPresentOnGoConfigChange() {
        when(goCache.get("GO_PIPELINE_CONFIGS_ETAGS_CACHE")).thenReturn(new Object());
        pipelineConfigService.onConfigChange(cruiseConfig);
        verify(goCache).remove("GO_PIPELINE_CONFIGS_ETAGS_CACHE");
    }

    @Test
    public void shouldNotRemovePipelineConfigsFromCacheWhenNotPresentOnGoConfigChange(){
        pipelineConfigService.onConfigChange(cruiseConfig);
        verify(goCache, never()).remove("GO_PIPELINE_CONFIGS_ETAGS_CACHE");
    }

    private void downstream(PipelineConfigs configs) {
        PipelineConfig down = PipelineConfigMother.pipelineConfig("down");
        down.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("mingle")));
        configs.add(down);
    }

    @Test
    public void shouldGetAllViewableOrOperatablePipelineConfigs() throws Exception {
        CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);
        PipelineConfig p1 = PipelineConfigMother.pipelineConfig("P1");
        PipelineConfig p2 = PipelineConfigMother.pipelineConfig("P2");
        PipelineConfig p3 = PipelineConfigMother.pipelineConfig("P3");
        Username username = new Username(new CaseInsensitiveString("user"));

        when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);
        when(cruiseConfig.getGroups()).thenReturn(new PipelineGroups(new BasicPipelineConfigs("group1", null, p1),
                                                                     new BasicPipelineConfigs("group2", null, p2),
                                                                     new BasicPipelineConfigs("group3", null, p3)));

        when(securityService.hasViewPermissionForGroup(CaseInsensitiveString.str(username.getUsername()), "group1")).thenReturn(true);

        when(securityService.hasViewPermissionForGroup(CaseInsensitiveString.str(username.getUsername()), "group2")).thenReturn(false);
        when(securityService.hasOperatePermissionForGroup(username.getUsername(), "group2")).thenReturn(false);

        when(securityService.hasViewPermissionForGroup(CaseInsensitiveString.str(username.getUsername()), "group3")).thenReturn(false);
        when(securityService.hasOperatePermissionForGroup(username.getUsername(), "group3")).thenReturn(true);

        List<PipelineConfigs> pipelineConfigs = pipelineConfigService.viewableOrOperatableGroupsFor(username);

        assertThat(pipelineConfigs.size(), is(2));
        assertThat(pipelineConfigs.get(0).getGroup(), is("group1"));
        assertThat(pipelineConfigs.get(1).getGroup(), is("group3"));
    }

    @Test
    public void shouldGetAllViewablePipelineConfigs() throws Exception {
        CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);
        PipelineConfig p1 = PipelineConfigMother.pipelineConfig("P1");
        PipelineConfig p2 = PipelineConfigMother.pipelineConfig("P2");
        Username username = new Username(new CaseInsensitiveString("user"));

        when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);
        when(cruiseConfig.getGroups()).thenReturn(new PipelineGroups(new BasicPipelineConfigs("group1", null, p1),
                                                                     new BasicPipelineConfigs("group2", null, p2)));

        when(securityService.hasViewPermissionForGroup(CaseInsensitiveString.str(username.getUsername()), "group1")).thenReturn(true);
        when(securityService.hasViewPermissionForGroup(CaseInsensitiveString.str(username.getUsername()), "group2")).thenReturn(false);


        List<PipelineConfigs> pipelineConfigs = pipelineConfigService.viewableOrOperatableGroupsFor(username);

        assertThat(pipelineConfigs.size(), is(1));
        assertThat(pipelineConfigs.get(0).getGroup(), is("group1"));
    }
}
