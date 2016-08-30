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

package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.materials.postcommit.PostCommitHookImplementer;
import com.thoughtworks.go.server.materials.postcommit.PostCommitHookMaterialType;
import com.thoughtworks.go.server.materials.postcommit.PostCommitHookMaterialTypeResolver;
import com.thoughtworks.go.server.perf.MDUPerformanceLogger;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.serverhealth.*;
import com.thoughtworks.go.util.ProcessManager;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.internal.verification.AtMost;

import java.util.*;

import static com.thoughtworks.go.helper.MaterialUpdateMessageMatcher.matchMaterialUpdateMessage;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class MaterialUpdateServiceTest {
    private MaterialUpdateQueue queue;
    private MaterialUpdateCompletedTopic completed;
    private ConfigMaterialUpdateQueue configQueue;
    private GoConfigWatchList watchList;
    private GoConfigService goConfigService;
    private static final SvnMaterialConfig MATERIAL_CONFIG = MaterialConfigsMother.svnMaterialConfig();
    private static final SvnMaterial MATERIAL = MaterialsMother.svnMaterial();
    private MaterialUpdateService service;
    private Username username;
    private HttpLocalizedOperationResult result;
    private PostCommitHookMaterialTypeResolver postCommitHookMaterialType;
    private PostCommitHookMaterialType validMaterialType;
    private PostCommitHookMaterialType invalidMaterialType;
    private ServerHealthService serverHealthService;
    private SystemEnvironment systemEnvironment;
    private MaterialConfigConverter materialConfigConverter;

    @Before
    public void setUp() throws Exception {
        queue = mock(MaterialUpdateQueue.class);
        configQueue = mock(ConfigMaterialUpdateQueue.class);
        watchList = mock(GoConfigWatchList.class);
        completed = mock(MaterialUpdateCompletedTopic.class);
        goConfigService = mock(GoConfigService.class);
        postCommitHookMaterialType = mock(PostCommitHookMaterialTypeResolver.class);
        serverHealthService = mock(ServerHealthService.class);
        systemEnvironment = new SystemEnvironment();

        materialConfigConverter = mock(MaterialConfigConverter.class);
        MDUPerformanceLogger mduPerformanceLogger = mock(MDUPerformanceLogger.class);
        service = new MaterialUpdateService(queue,configQueue , completed,watchList, goConfigService, systemEnvironment,
                serverHealthService, postCommitHookMaterialType, mduPerformanceLogger, materialConfigConverter);
        HashSet<MaterialConfig> materialConfigs = new HashSet(Collections.singleton(MATERIAL_CONFIG));
        HashSet<Material> materials = new HashSet(Collections.singleton(MATERIAL));
        when(goConfigService.getSchedulableMaterials()).thenReturn(materialConfigs);
        when(materialConfigConverter.toMaterials(materialConfigs)).thenReturn(materials);
        username = new Username(new CaseInsensitiveString("loser"));
        result = new HttpLocalizedOperationResult();
        validMaterialType = mock(PostCommitHookMaterialType.class);
        when(validMaterialType.isKnown()).thenReturn(true);
        when(validMaterialType.isValid(anyString())).thenReturn(true);
        invalidMaterialType = mock(PostCommitHookMaterialType.class);
        when(invalidMaterialType.isKnown()).thenReturn(false);
        when(invalidMaterialType.isValid(anyString())).thenReturn(false);
    }

    @After
    public void teardown() throws Exception {
        systemEnvironment.reset(SystemEnvironment.MATERIAL_UPDATE_INACTIVE_TIMEOUT);
    }

    @Test
    public void shouldSendMaterialUpdateMessageOnlyToMaterialQueueWhenTimerIsCalled() throws Exception {
        service.onTimer();
        Mockito.verify(queue).post(matchMaterialUpdateMessage(MATERIAL));
        Mockito.verify(configQueue,times(0)).post(any(MaterialUpdateMessage.class));
    }
    @Test
    public void shouldSendMaterialUpdateMessageOnlyToConfigQueue_WhenTimerIsCalled_AndMaterialIsConfigRepo() throws Exception {
        when(watchList.hasConfigRepoWithFingerprint(MATERIAL.getFingerprint())).thenReturn(true);
        service.onTimer();
        Mockito.verify(configQueue).post(matchMaterialUpdateMessage(MATERIAL));
        Mockito.verify(queue,times(0)).post(any(MaterialUpdateMessage.class));
    }

    @Test
    public void shouldNotSendMaterialUpdateMessageIfMaterialIsStillBeingChecked() throws Exception {
        service.onTimer();
        service.onTimer();
        Mockito.verify(queue, new AtMost(1)).post(matchMaterialUpdateMessage(MATERIAL));
        Mockito.verify(configQueue,times(0)).post(any(MaterialUpdateMessage.class));
    }

    @Test
    public void shouldReturn401WhenUserIsNotAnAdmin_WhenInvokingPostCommitHookMaterialUpdate() {
        when(goConfigService.isUserAdmin(username)).thenReturn(false);
        service.notifyMaterialsForUpdate(username, new HashMap(), result);

        HttpLocalizedOperationResult unauthorizedResult = new HttpLocalizedOperationResult();
        unauthorizedResult.unauthorized(LocalizedMessage.string("API_ACCESS_UNAUTHORIZED"), HealthStateType.unauthorised());

        assertThat(result, is(unauthorizedResult));

        verify(goConfigService).isUserAdmin(username);
    }

    @Test
    public void shouldReturn400WhenTypeIsMissing_WhenInvokingPostCommitHookMaterialUpdate() {
        when(goConfigService.isUserAdmin(username)).thenReturn(true);
        service.notifyMaterialsForUpdate(username, new HashMap(), result);

        HttpLocalizedOperationResult badRequestResult = new HttpLocalizedOperationResult();
        badRequestResult.badRequest(LocalizedMessage.string("API_BAD_REQUEST"));

        assertThat(result, is(badRequestResult));

        verify(goConfigService).isUserAdmin(username);
    }

    @Test
    public void shouldReturn400WhenTypeIsInvalid_WhenInvokingPostCommitHookMaterialUpdate() {
        when(goConfigService.isUserAdmin(username)).thenReturn(true);
        when(postCommitHookMaterialType.toType("some_invalid_type")).thenReturn(invalidMaterialType);
        final HashMap params = new HashMap();
        params.put(MaterialUpdateService.TYPE, "some_invalid_type");
        service.notifyMaterialsForUpdate(username, params, result);

        HttpLocalizedOperationResult badRequestResult = new HttpLocalizedOperationResult();
        badRequestResult.badRequest(LocalizedMessage.string("API_BAD_REQUEST"));

        assertThat(result, is(badRequestResult));

        verify(goConfigService).isUserAdmin(username);
    }

    @Test
    public void shouldReturn404WhenThereAreNoMaterialsToSchedule_WhenInvokingPostCommitHookMaterialUpdate() {
        when(goConfigService.isUserAdmin(username)).thenReturn(true);

        PostCommitHookMaterialType materialType = mock(PostCommitHookMaterialType.class);
        when(postCommitHookMaterialType.toType("type")).thenReturn(materialType);

        PostCommitHookImplementer hookImplementer = mock(PostCommitHookImplementer.class);
        when(materialType.getImplementer()).thenReturn(hookImplementer);
        when(materialType.isKnown()).thenReturn(true);

        CruiseConfig config = mock(BasicCruiseConfig.class);
        when(goConfigService.currentCruiseConfig()).thenReturn(config);
        when(config.getGroups()).thenReturn(new PipelineGroups());

        when(hookImplementer.prune(anySet(), anyMap())).thenReturn(new HashSet<Material>());

        final HashMap params = new HashMap();
        params.put(MaterialUpdateService.TYPE, "type");

        service.notifyMaterialsForUpdate(username, params, result);

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        operationResult.notFound(LocalizedMessage.string("MATERIAL_SUITABLE_FOR_NOTIFICATION_NOT_FOUND"), HealthStateType.general(HealthStateScope.GLOBAL));

        assertThat(result, is(operationResult));

        verify(hookImplementer).prune(anySet(), anyMap());
    }

    @Test
    public void shouldReturnImplementerOfSvnPostCommitHookAndPerformMaterialUpdate_WhenInvokingPostCommitHookMaterialUpdate() {
        final HashMap params = new HashMap();
        params.put(MaterialUpdateService.TYPE, "svn");
        when(goConfigService.isUserAdmin(username)).thenReturn(true);
        final CruiseConfig cruiseConfig = new BasicCruiseConfig(PipelineConfigMother.createGroup("groupName", "pipeline1", "pipeline2"));
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(postCommitHookMaterialType.toType("svn")).thenReturn(validMaterialType);
        final PostCommitHookImplementer svnPostCommitHookImplementer = mock(PostCommitHookImplementer.class);
        final Material svnMaterial = mock(Material.class);
        when(svnPostCommitHookImplementer.prune(anySet(), eq(params))).thenReturn(new HashSet(Arrays.asList(svnMaterial)));
        when(validMaterialType.getImplementer()).thenReturn(svnPostCommitHookImplementer);
        final MaterialUpdateService spyService = spy(service);
        doNothing().when(spyService).updateMaterial(svnMaterial);
        spyService.notifyMaterialsForUpdate(username, params, result);
        verify(svnPostCommitHookImplementer).prune(anySet(), eq(params));
        verify(spyService).updateMaterial(svnMaterial);

        HttpLocalizedOperationResult acceptedResult = new HttpLocalizedOperationResult();
        acceptedResult.accepted(LocalizedMessage.string("MATERIAL_SCHEDULE_NOTIFICATION_ACCEPTED"));

        assertThat(result, is(acceptedResult));
    }

    @Test
    public void shouldUpdateServerHealthMessageWhenHung() {
        //given
        service = spy(service);
        systemEnvironment.set(SystemEnvironment.MATERIAL_UPDATE_INACTIVE_TIMEOUT, 1);
        ProcessManager processManager = mock(ProcessManager.class);
        Material material = mock(Material.class);
        service.updateMaterial(material);
        when(service.getProcessManager()).thenReturn(processManager);
        when(material.getFingerprint()).thenReturn("fingerprint");
        when(material.getUriForDisplay()).thenReturn("uri");
        when(material.getLongDescription()).thenReturn("details to uniquely identify a material");
        when(material.isAutoUpdate()).thenReturn(true);
        when(processManager.getIdleTimeFor("fingerprint")).thenReturn(60010L);

        //when
        service.updateMaterial(material);

        //then
        verify(serverHealthService).removeByScope(HealthStateScope.forMaterialUpdate(material));
        ArgumentCaptor<ServerHealthState> argumentCaptor = ArgumentCaptor.forClass(ServerHealthState.class);
        verify(serverHealthService).update(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getMessage(), is("Material update for uri hung:"));
        assertThat(argumentCaptor.getValue().getDescription(),
                is("Material update is currently running but has not shown any activity in the last 1 minute(s). This may be hung. Details - details to uniquely identify a material"));
        assertThat(argumentCaptor.getValue().getType(), is(HealthStateType.general(HealthStateScope.forMaterialUpdate(material))));
    }

    @Test
    public void shouldNotUpdateServerHealthMessageWhenIdleTimeLessThanConfigured() {
        //given
        service = spy(service);
        systemEnvironment.set(SystemEnvironment.MATERIAL_UPDATE_INACTIVE_TIMEOUT, 2);
        ProcessManager processManager = mock(ProcessManager.class);
        Material material = mock(Material.class);
        service.updateMaterial(material);
        when(service.getProcessManager()).thenReturn(processManager);
        when(material.getFingerprint()).thenReturn("fingerprint");
        when(processManager.getIdleTimeFor("fingerprint")).thenReturn(60010L);

        //when
        service.updateMaterial(material);

        //then
        verify(serverHealthService, never()).removeByScope(HealthStateScope.forMaterialUpdate(material));
        verify(serverHealthService, never()).update(Matchers.<ServerHealthState>any());
    }

    @Test
    public void shouldRemoveServerHealthMessageOnMaterialUpdateCompletion() {
        Material material = mock(Material.class);
        when(material.getFingerprint()).thenReturn("fingerprint");
        service.onMessage(new MaterialUpdateCompletedMessage(material, 0));
        verify(serverHealthService).removeByScope(HealthStateScope.forMaterialUpdate(material));
    }

    @Test
    public void shouldMaterialUpdateShouldNotBeInProgressIfUpdateMaterialMessagePostFails() {
        doThrow(new RuntimeException("failed")).when(queue).post(matchMaterialUpdateMessage(MATERIAL));
        try {
            service.updateMaterial(MATERIAL);
            fail("Should have failed");
        } catch (RuntimeException e) {
            // should re-throw exception
        }
        Map<Material, Date> inProgress = (Map<Material, Date>) ReflectionUtil.getField(service, "inProgress");
        assertThat(inProgress.containsKey(MATERIAL), is(false));
    }

    @Test
    public void shouldCacheSchedulableMaterials() {
        service.onTimer();
        service.onTimer();
        verify(goConfigService).getSchedulableMaterials();
    }

    @Test
    public void shouldClearSchedulableMaterialCacheOnConfigChange() {
        when(serverHealthService.getAllLogs()).thenReturn(new ServerHealthStates());
        service.onTimer();
        service.onConfigChange(mock(BasicCruiseConfig.class));
        service.onTimer();
        verify(goConfigService, times(2)).getSchedulableMaterials();
    }

    @Test
    public void shouldClearSchedulableMaterialCacheOnPipelineConfigChange() {
        ArgumentCaptor<ConfigChangedListener> captor = ArgumentCaptor.forClass(ConfigChangedListener.class);
        doNothing().when(goConfigService).register(captor.capture());
        service.initialize();
        List<ConfigChangedListener> listeners = captor.getAllValues();
        assertThat(listeners.get(1) instanceof EntityConfigChangedListener, is(true));
        EntityConfigChangedListener<PipelineConfig> pipelineConfigChangeListener= (EntityConfigChangedListener<PipelineConfig>) listeners.get(1);

        when(serverHealthService.getAllLogs()).thenReturn(new ServerHealthStates());
        when(goConfigService.getCurrentConfig()).thenReturn(mock(CruiseConfig.class));
        service.onTimer();
        PipelineConfig pipelineConfig = mock(PipelineConfig.class);
        when(pipelineConfig.materialConfigs()).thenReturn(new MaterialConfigs(new GitMaterialConfig("url")));

        pipelineConfigChangeListener.onEntityConfigChange(pipelineConfig);
        service.onTimer();
        verify(goConfigService, times(2)).getSchedulableMaterials();
    }

    @Test
    public void shouldAllowPostCommitNotificationsToPassThroughToTheQueue_WhenTheSameMaterialIsCurrentlyInProgressAndMaterialIsAutoUpdateFalse() throws Exception {
        ScmMaterial material = mock(ScmMaterial.class);
        when(material.isAutoUpdate()).thenReturn(false);
        MaterialUpdateMessage message = new MaterialUpdateMessage(material, 0);
        doNothing().when(queue).post(message);
        service.updateMaterial(material); //prune inprogress queue to have this material in it
        service.updateMaterial(material); // immediately notify another check-in
        verify(queue, times(2)).post(message);
        verify(material).isAutoUpdate();
    }

    @Test
    public void shouldNotAllowPostCommitNotificationsToPassThroughToTheConfigQueue_WhenTheSameMaterialIsCurrentlyInProgressAndMaterialIsAutoUpdateTrueAndMaterialIsConfigRepo() throws Exception {
        ScmMaterial material = mock(ScmMaterial.class);
        when(material.isAutoUpdate()).thenReturn(true);
        when(material.getFingerprint()).thenReturn("fingerprint");
        when(watchList.hasConfigRepoWithFingerprint("fingerprint")).thenReturn(true);
        MaterialUpdateMessage message = new MaterialUpdateMessage(material, 0);
        doNothing().when(configQueue).post(message);
        service.updateMaterial(material); //prune inprogress queue to have this material in it
        service.updateMaterial(material); // immediately notify another check-in
        verify(configQueue, times(1)).post(message);
        verify(material).isAutoUpdate();
    }

    @Test
    public void shouldNotAllowPostCommitNotificationsToPassThroughToTheQueue_WhenTheSameMaterialIsCurrentlyInProgressAndMaterialIsAutoUpdateTrue() throws Exception {
        ScmMaterial material = mock(ScmMaterial.class);
        when(material.isAutoUpdate()).thenReturn(true);
        MaterialUpdateMessage message = new MaterialUpdateMessage(material, 0);
        doNothing().when(queue).post(message);
        service.updateMaterial(material); //prune inprogress queue to have this material in it
        service.updateMaterial(material); // immediately notify another check-in
        verify(queue, times(1)).post(message);
        verify(material).isAutoUpdate();
    }

    @Test
    public void shouldAllowPostCommitNotificationsToPassThroughToTheQueue_WhenTheSameMaterialIsNotCurrentlyInProgressAndMaterialIsAutoUpdateTrue() throws Exception {
        ScmMaterial material = mock(ScmMaterial.class);
        when(material.isAutoUpdate()).thenReturn(true);
        MaterialUpdateMessage message = new MaterialUpdateMessage(material, 0);
        doNothing().when(queue).post(message);
        service.updateMaterial(material); // first call to the method
        verify(queue, times(1)).post(message);
        verify(material, never()).isAutoUpdate();
    }

    @Test
    public void shouldAllowPostCommitNotificationsToPassThroughToTheQueue_WhenTheSameMaterialIsNotCurrentlyInProgressAndMaterialIsAutoUpdateFalse() throws Exception {
        ScmMaterial material = mock(ScmMaterial.class);
        when(material.isAutoUpdate()).thenReturn(false);
        MaterialUpdateMessage message = new MaterialUpdateMessage(material, 0);
        doNothing().when(queue).post(message);
        service.updateMaterial(material); // first call to the method
        verify(queue, times(1)).post(message);
        verify(material, never()).isAutoUpdate();
    }
}
