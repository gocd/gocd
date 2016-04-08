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

package com.thoughtworks.go.server.scheduling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.domain.PipelineConfigDependencyGraph;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.materials.MaterialUpdateCompletedTopic;
import com.thoughtworks.go.server.materials.MaterialUpdateFailedMessage;
import com.thoughtworks.go.server.materials.MaterialUpdateService;
import com.thoughtworks.go.server.materials.MaterialUpdateStatusListener;
import com.thoughtworks.go.server.materials.MaterialUpdateStatusNotifier;
import com.thoughtworks.go.server.materials.MaterialUpdateSuccessfulMessage;
import com.thoughtworks.go.server.materials.SpecificMaterialRevisionFactory;
import com.thoughtworks.go.server.perf.SchedulingPerformanceLogger;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.AutoBuild;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.ManualBuild;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import com.thoughtworks.go.server.service.MaterialExpansionService;
import com.thoughtworks.go.server.service.NoModificationsPresentForDependentMaterialException;
import com.thoughtworks.go.server.service.PipelineScheduleQueue;
import com.thoughtworks.go.server.service.PipelineService;
import com.thoughtworks.go.server.service.SchedulingCheckerService;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.SystemEnvironment;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static com.thoughtworks.go.serverhealth.HealthStateScope.GLOBAL;
import static com.thoughtworks.go.serverhealth.ServerHealthState.error;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class BuildCauseProducerServiceTest {
    private static final ServerHealthState SERVER_ERROR = error("something", "else", HealthStateType.general(GLOBAL));

    private PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("pipeline"), new MaterialConfigs());
    @Mock private ServerHealthService mockServerHealthService;
    @Mock private SchedulingCheckerService mockSchedulingCheckerService;
    @Mock private MaterialUpdateStatusNotifier mockMaterialUpdateStatusNotifier;
    @Mock private MaterialUpdateService mockMaterialUpdateService;
    @Mock private OperationResult operationResult;
    @Mock private PipelineScheduleQueue pipelineScheduleQueue;
    @Mock private MaterialRepository materialRepository;
    @Mock private SpecificMaterialRevisionFactory specificMaterialRevisionFactory;
    @Mock private PipelineService pipelineService;
    @Mock private PipelineTimeline pipelineTimeline;
    @Mock private GoConfigService goConfigService;
    @Mock private MaterialConfigConverter materialConfigConverter;
    @Mock private MaterialExpansionService materialExpansionService;
    @Mock private SchedulingPerformanceLogger schedulingPerformanceLogger;

    private final Map<String, String> EMPTY_REVISIONS = new HashMap<String, String>();
    private HealthStateType healthStateType;
    private TriggerMonitor triggerMonitor;
    private BuildCauseProducerService buildCauseProducerService;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        triggerMonitor = new TriggerMonitor();
        healthStateType = HealthStateType.general(HealthStateScope.forPipeline(CaseInsensitiveString.str(pipelineConfig.name())));
        when(goConfigService.pipelineConfigNamed(pipelineConfig.name())).thenReturn(pipelineConfig);

        buildCauseProducerService = createBuildCauseProducerService(mockMaterialUpdateStatusNotifier);
    }

    private BuildCauseProducerService createBuildCauseProducerService(MaterialUpdateStatusNotifier materialUpdateStatusNotifier) {
        return new BuildCauseProducerService(mockSchedulingCheckerService, mockServerHealthService,
                pipelineScheduleQueue, goConfigService, materialRepository, materialUpdateStatusNotifier,
                mockMaterialUpdateService, specificMaterialRevisionFactory, triggerMonitor, pipelineService, new SystemEnvironment(), materialConfigConverter,
                materialExpansionService, schedulingPerformanceLogger);
    }

    @Test
    public void onErrorShouldUpdateServerHealthWhenUpdateServerHealthStatusByDefault() throws Exception {
        buildCauseProducerService.manualSchedulePipeline(Username.CRUISE_TIMER, pipelineConfig.name(), new ScheduleOptions(), errorResult());
        verify(mockServerHealthService).update(SERVER_ERROR);
    }

    @Test
    public void shouldAllowRetriggeringIfThePreviousTriggerFailed() throws Exception {

        buildCauseProducerService.manualSchedulePipeline(Username.CRUISE_TIMER, pipelineConfig.name(), new ScheduleOptions(), errorResult());

        HttpOperationResult result = new HttpOperationResult();
        buildCauseProducerService.manualSchedulePipeline(Username.CRUISE_TIMER, pipelineConfig.name(), new ScheduleOptions(), result);
        assertThat(result.httpCode(), is(202));

    }

    @Test
    public void shouldCheckForModificationsWhenManuallyScheduling() throws Exception {
        HgMaterialConfig hgMaterialConfig = new HgMaterialConfig("url", null);
        HgMaterial hgMaterial = new HgMaterial("url", null);
        SvnMaterial svnMaterial = new SvnMaterial("url", null, null, false);
        SvnMaterialConfig svnMaterialConfig = new SvnMaterialConfig("url", null, null, false);
        pipelineConfig.addMaterialConfig(hgMaterialConfig);
        pipelineConfig.addMaterialConfig(svnMaterialConfig);

        when(materialConfigConverter.toMaterial(hgMaterialConfig)).thenReturn(hgMaterial);
        when(materialConfigConverter.toMaterial(svnMaterialConfig)).thenReturn(svnMaterial);


        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        buildCauseProducerService.manualSchedulePipeline(Username.ANONYMOUS, pipelineConfig.name(), new ScheduleOptions(), result);
        assertThat(result.getServerHealthState().isSuccess(), is(true));

        verify(mockMaterialUpdateService, times(2)).updateMaterial(any(Material.class));
        verify(mockMaterialUpdateStatusNotifier).registerListenerFor(eq(pipelineConfig),
                any(MaterialUpdateStatusListener.class));
    }

    @Test
    public void shouldNotCheckForModificationsIfAlreadyChecking() throws Exception {
        Username user = Username.ANONYMOUS;
        final HttpOperationResult result = new HttpOperationResult();

        when(mockSchedulingCheckerService.canTriggerManualPipeline(pipelineConfig, CaseInsensitiveString.str(user.getUsername()), operationResult)).thenAnswer(new Answer<Boolean>() {
            public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
                result.accepted("junk", "junk", healthStateType);
                return true;
            }
        });

        buildCauseProducerService.markPipelineAsAlreadyTriggered(pipelineConfig);
        buildCauseProducerService.manualSchedulePipeline(user, pipelineConfig.name(), new ScheduleOptions(), result);

        assertThat(result.canContinue(), is(false));
        assertThat(result.message(), is("Failed to force pipeline: pipeline"));

        verify(mockMaterialUpdateService, never()).updateMaterial(any(Material.class));
        verify(mockMaterialUpdateStatusNotifier, never()).registerListenerFor(eq(pipelineConfig),
                any(MaterialUpdateStatusListener.class));
    }

    @Test
    public void shouldAllowTriggeringOfPipelineWhenThereIsAnErrorAfterPipelineIsMarkedAsTriggeredAndBeforeTheMaterialUpdateIsScheduled()
            throws Exception {
        try {
            when(operationResult.canContinue()).thenThrow(new RuntimeException("force a failure"));
            buildCauseProducerService.manualSchedulePipeline(Username.ANONYMOUS, pipelineConfig.name(), new ScheduleOptions(), operationResult);
            fail("expected exception, got none");
        } catch (Exception e) {
            assertThat(triggerMonitor.isAlreadyTriggered(CaseInsensitiveString.str(pipelineConfig.name())), is(false));
        }
    }

    @Test
    public void shouldAllowTriggeringOfPipelineAfterMaterialUpdate() throws Exception {
        HgMaterial hgMaterial = new HgMaterial("url", null);
        HgMaterialConfig hgMaterialConfig = new HgMaterialConfig("url", null);

        pipelineConfig.addMaterialConfig(hgMaterialConfig);

        when(materialConfigConverter.toMaterial(hgMaterialConfig)).thenReturn(hgMaterial);
        when(specificMaterialRevisionFactory.create("pipeline", new HashMap<String, String>())).thenReturn(new MaterialRevisions());
        when(pipelineScheduleQueue.mostRecentScheduled(CaseInsensitiveString.str(pipelineConfig.name()))).thenReturn(BuildCause.createNeverRun());
        when(materialRepository.findLatestModification(hgMaterial)).thenReturn(new MaterialRevisions(new MaterialRevision(hgMaterial, new ArrayList<Modification>())));

        buildCauseProducerService.manualSchedulePipeline(Username.ANONYMOUS, pipelineConfig.name(), new ScheduleOptions(), new ServerHealthStateOperationResult());
        assertThat(triggerMonitor.isAlreadyTriggered(CaseInsensitiveString.str(pipelineConfig.name())), is(true));
        sendMaterialUpdateCompleteMessage(extractMaterialListenerInstanceFromRegisterCall(), hgMaterial);
        assertThat(triggerMonitor.isAlreadyTriggered(CaseInsensitiveString.str(pipelineConfig.name())), is(false));
    }

    @Test
    public void manualTriggerShouldNotTriggerThePipelineIfMaterialUpdateFailed() throws Exception {
        HgMaterialConfig hgMaterialConfig = new HgMaterialConfig("url", null);
        HgMaterial hgMaterial = new HgMaterial("url", null);

        pipelineConfig.addMaterialConfig(hgMaterialConfig);
        when(materialConfigConverter.toMaterial(hgMaterialConfig)).thenReturn(hgMaterial);

        buildCauseProducerService.manualSchedulePipeline(Username.ANONYMOUS, pipelineConfig.name(), new ScheduleOptions(), new ServerHealthStateOperationResult());
        sendMaterialUpdateFailedMessage(extractMaterialListenerInstanceFromRegisterCall(), hgMaterial);
        assertThat(triggerMonitor.isAlreadyTriggered(CaseInsensitiveString.str(pipelineConfig.name())), is(false));
    }

    private void sendMaterialUpdateCompleteMessage(MaterialUpdateStatusListener materialUpdateStatusListener, HgMaterial material) {
        materialUpdateStatusListener.onMaterialUpdate(new MaterialUpdateSuccessfulMessage(material, 0));
    }

    private void sendMaterialUpdateFailedMessage(MaterialUpdateStatusListener materialUpdateStatusListener, HgMaterial material) {
        materialUpdateStatusListener.onMaterialUpdate(new MaterialUpdateFailedMessage(material, 0, new RuntimeException("Seven Nation Army")));
    }

    private MaterialUpdateStatusListener extractMaterialListenerInstanceFromRegisterCall() {
        final MaterialUpdateStatusListener[] listener = new MaterialUpdateStatusListener[1];
        verify(mockMaterialUpdateStatusNotifier).registerListenerFor(any(PipelineConfig.class),
                argThat(new BaseMatcher<MaterialUpdateStatusListener>() {
                    public boolean matches(Object o) {
                        listener[0] = (MaterialUpdateStatusListener) o;
                        return true;
                    }

                    public void describeTo(Description description) {
                    }
                }));
        return listener[0];
    }


    @Test
    public void shouldNotCheckForModificationsUnableToTriggerManualPipeline() throws Exception {
        buildCauseProducerService.manualSchedulePipeline(Username.ANONYMOUS, pipelineConfig.name(), new ScheduleOptions(), errorResult());
        verify(mockMaterialUpdateService, never()).updateMaterial(any(Material.class));
        verify(mockMaterialUpdateStatusNotifier, never()).registerListenerFor(eq(pipelineConfig),
                any(MaterialUpdateStatusListener.class));
    }

    @Test
    public void shouldScheduleAfterAllMaterialsAreUpdated() throws Exception {
        HgMaterial hgMaterial = new HgMaterial("url", null);
        HgMaterialConfig hgMaterialConfig = new HgMaterialConfig("url", null);
        SvnMaterial svnMaterial = new SvnMaterial("url", null, null, false);
        SvnMaterialConfig svnMaterialConfig = new SvnMaterialConfig("url", null, null, false);
        pipelineConfig.addMaterialConfig(hgMaterialConfig);
        pipelineConfig.addMaterialConfig(svnMaterialConfig);

        GoConfigService service = mock(GoConfigService.class);
        when(service.pipelineConfigNamed(pipelineConfig.name())).thenReturn(pipelineConfig);
        when(materialConfigConverter.toMaterial(hgMaterialConfig)).thenReturn(hgMaterial);
        when(materialConfigConverter.toMaterial(svnMaterialConfig)).thenReturn(svnMaterial);

        MaterialUpdateStatusNotifier notifier = new MaterialUpdateStatusNotifier(mock(MaterialUpdateCompletedTopic.class));
        buildCauseProducerService = spy(createBuildCauseProducerService(notifier));
        buildCauseProducerService.manualSchedulePipeline(Username.ANONYMOUS, pipelineConfig.name(), new ScheduleOptions(), new ServerHealthStateOperationResult());
        final HashMap<String, String> stringStringHashMap = new HashMap<String, String>();
        doReturn(ServerHealthState.success(healthStateType)).when(buildCauseProducerService).newProduceBuildCause(
                eq(pipelineConfig), any(ManualBuild.class),
                new ScheduleOptions(eq(EMPTY_REVISIONS), stringStringHashMap, new HashMap<String, String>()), any(ServerHealthStateOperationResult.class), eq(12345L));

        assertThat(notifier.hasListenerFor(pipelineConfig), is(true));
        notifier.onMessage(new MaterialUpdateSuccessfulMessage(hgMaterial, 1111L));

        assertThat(notifier.hasListenerFor(pipelineConfig), is(true));
        notifier.onMessage(new MaterialUpdateSuccessfulMessage(svnMaterial, 2222L));

        assertThat(notifier.hasListenerFor(pipelineConfig), is(false));
        verify(buildCauseProducerService).newProduceBuildCause(eq(pipelineConfig), any(ManualBuild.class), eq(new ScheduleOptions()), any(ServerHealthStateOperationResult.class), eq(2222L));
    }

    @Test
    public void shouldUpdateResultAsAcceptedOnSuccess() throws Exception {
        when(mockMaterialUpdateStatusNotifier.hasListenerFor(pipelineConfig)).thenReturn(false);
        when(operationResult.canContinue()).thenReturn(true);
        buildCauseProducerService.manualSchedulePipeline(Username.BLANK, pipelineConfig.name(), new ScheduleOptions(), operationResult);
        verify(operationResult).accepted(eq("Request to schedule pipeline pipeline accepted"), any(String.class),
                any(HealthStateType.class));
    }

    @Test
    public void shouldBeAbleToPassInSpecificRevisionForMaterialsAndScheduleABuild() throws Exception {
        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("upstream-pipeline"), new CaseInsensitiveString("stage"));
        SvnMaterial svnMaterial = new SvnMaterial("url", null, null, false);
        pipelineConfig.addMaterialConfig(dependencyMaterial.config());
        pipelineConfig.addMaterialConfig(svnMaterial.config());

        List<Modification> svnModifications = ModificationsMother.multipleModificationList();
        MaterialConfigs knownMaterialConfigs = new MaterialConfigs(pipelineConfig.materialConfigs());

        MaterialRevision specificMaterialRevision = new MaterialRevision(dependencyMaterial, new Modification(new Date(), "upstream-pipeline/2/stage/1", "MOCK_LABEL-12", null));
        when(specificMaterialRevisionFactory.create(eq("pipeline"), eq(Collections.singletonMap(dependencyMaterial.getPipelineUniqueFingerprint(), "upstream-pipeline/2/stage/1"))))
                .thenReturn(new MaterialRevisions(specificMaterialRevision));
        when(pipelineScheduleQueue.mostRecentScheduled("pipeline")).thenReturn(BuildCause.createNeverRun());
        when(materialRepository.findLatestModification(svnMaterial)).thenReturn(new MaterialRevisions(new MaterialRevision(svnMaterial, svnModifications)));

        when(materialConfigConverter.toMaterials(pipelineConfig.materialConfigs())).thenReturn(new Materials(dependencyMaterial, svnMaterial));
        when(materialExpansionService.expandMaterialConfigsForScheduling(pipelineConfig.materialConfigs())).thenReturn(knownMaterialConfigs);
        when(materialConfigConverter.toMaterials(knownMaterialConfigs)).thenReturn(new Materials(dependencyMaterial, svnMaterial));

        ManualBuild buildType = new ManualBuild(Username.ANONYMOUS);
        final HashMap<String, String> stringStringHashMap = new HashMap<String, String>();
        buildCauseProducerService.newProduceBuildCause(pipelineConfig, buildType,
                new ScheduleOptions(Collections.singletonMap(dependencyMaterial.getPipelineUniqueFingerprint(), "upstream-pipeline/2/stage/1"),
                        stringStringHashMap, new HashMap<String, String>()), new ServerHealthStateOperationResult(), 12345);

        verify(pipelineScheduleQueue).schedule(eq("pipeline"), argThat(containsRevisions(new MaterialRevision(svnMaterial, svnModifications), specificMaterialRevision)));
    }

    @Test
    public void shouldHandleCaseWhereSpecifiedRevisionDoesNotExist() throws Exception {
        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("upstream-pipeline"), new CaseInsensitiveString("stage"));
        when(specificMaterialRevisionFactory.create(eq("pipeline"), eq(Collections.singletonMap(dependencyMaterial.getPipelineUniqueFingerprint(), "upstream-pipeline/200/stage/1"))))
                .thenThrow(new RuntimeException("Invalid specified revision"));
        ManualBuild buildType = new ManualBuild(Username.ANONYMOUS);
        final HashMap<String, String> stringStringHashMap = new HashMap<String, String>();
        buildCauseProducerService.newProduceBuildCause(pipelineConfig, buildType,
                new ScheduleOptions(Collections.singletonMap(dependencyMaterial.getPipelineUniqueFingerprint(), "upstream-pipeline/200/stage/1"),
                        stringStringHashMap, new HashMap<String, String>()), new ServerHealthStateOperationResult(), 12345);
        verify(mockServerHealthService).update(argThat(hasErrorHealthState("Error while scheduling pipeline: pipeline", "Invalid specified revision")));
    }

    @Test
    public void shouldHandleCaseWhenExceptionWithoutMessageIsRaised() throws Exception {
        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("upstream-pipeline"), new CaseInsensitiveString("stage"));
        when(specificMaterialRevisionFactory.create(eq("pipeline"), eq(Collections.singletonMap(dependencyMaterial.getPipelineUniqueFingerprint(), "upstream-pipeline/200/stage/1"))))
                .thenThrow(new NullPointerException());
        ManualBuild buildType = new ManualBuild(Username.ANONYMOUS);
        final HashMap<String, String> stringStringHashMap = new HashMap<String, String>();
        buildCauseProducerService.newProduceBuildCause(pipelineConfig, buildType,
                new ScheduleOptions(Collections.singletonMap(dependencyMaterial.getPipelineUniqueFingerprint(), "upstream-pipeline/200/stage/1"), stringStringHashMap, new HashMap<String, String>()),
                new ServerHealthStateOperationResult(), 12345);
        verify(mockServerHealthService).update(argThat(hasErrorHealthState("Error while scheduling pipeline: pipeline", "Details not available, please check server logs.")));
    }

    @Test
    public void shouldUpdateOnlyOnceIfThereAreTwoMaterialsWithSameFingerPrintButDifferentDest() {
        HgMaterial material1 = new HgMaterial("url", null);
        HgMaterial material2 = new HgMaterial("url", null);
        HgMaterialConfig materialConfig1 = new HgMaterialConfig("url", null);
        HgMaterialConfig materialConfig2 = new HgMaterialConfig("url", null);
        material1.setFolder("folder1");
        material2.setFolder("folder2");

        assertThat(material1.getFingerprint(), is(material2.getFingerprint()));

        pipelineConfig.addMaterialConfig(materialConfig1);
        pipelineConfig.addMaterialConfig(materialConfig2);
        Material[] materials = new Material[]{material1, material2};

        when(materialConfigConverter.toMaterial(materialConfig1)).thenReturn(material1);
        when(materialConfigConverter.toMaterial(materialConfig2)).thenReturn(material2);

        buildCauseProducerService.manualSchedulePipeline(Username.ANONYMOUS, pipelineConfig.name(),
                new ScheduleOptions(new HashMap<String, String>(), new HashMap<String, String>(), new HashMap<String, String>()),
                new ServerHealthStateOperationResult());
        verify(mockMaterialUpdateService, times(1)).updateMaterial(any(Material.class));
        MaterialUpdateStatusListener statusListener = extractMaterialListenerInstanceFromRegisterCall();
        statusListener.onMaterialUpdate(new MaterialUpdateFailedMessage(materials[0], 0, new Exception("Cannot connect to repo")));
        verify(mockMaterialUpdateStatusNotifier).removeListenerFor(pipelineConfig);

    }

    @Test
    public void manualTrigger_shouldUpdatePipelineConfigWhenMaterialIsConfigRepo() {
        HgMaterial material1 = new HgMaterial("url", null);
        HgMaterialConfig materialConfig1 = new HgMaterialConfig("url", null);

        pipelineConfig.addMaterialConfig(materialConfig1);
        pipelineConfig.setOrigin(new RepoConfigOrigin(new ConfigRepoConfig(materialConfig1,"plug"),"revision1"));

        when(materialConfigConverter.toMaterial(materialConfig1)).thenReturn(material1);
        when(goConfigService.hasPipelineNamed(pipelineConfig.name())).thenReturn(true);

        buildCauseProducerService.manualSchedulePipeline(Username.ANONYMOUS, pipelineConfig.name(),
                new ScheduleOptions(new HashMap<String, String>(), new HashMap<String, String>(), new HashMap<String, String>()),
                new ServerHealthStateOperationResult());
        verify(goConfigService, times(1)).pipelineConfigNamed(pipelineConfig.name());

        MaterialUpdateStatusListener statusListener = extractMaterialListenerInstanceFromRegisterCall();
        statusListener.onMaterialUpdate(new MaterialUpdateSuccessfulMessage(material1, 0));
        verify(mockMaterialUpdateStatusNotifier).removeListenerFor(pipelineConfig);

        verify(goConfigService, times(2)).pipelineConfigNamed(pipelineConfig.name());
    }

    @Test
    public void manualTrigger_shouldNotUpdatePipelineConfigWhenConfigRepoIsNotInMaterials() {
        HgMaterial material1 = new HgMaterial("url", null);
        HgMaterialConfig materialConfig1 = new HgMaterialConfig("url", null);
        HgMaterialConfig materialConfig2 = new HgMaterialConfig("url2", null);

        pipelineConfig.addMaterialConfig(materialConfig1);
        pipelineConfig.setOrigin(new RepoConfigOrigin(new ConfigRepoConfig(materialConfig2,"plug"),"revision1"));

        when(materialConfigConverter.toMaterial(materialConfig1)).thenReturn(material1);

        buildCauseProducerService.manualSchedulePipeline(Username.ANONYMOUS, pipelineConfig.name(),
                new ScheduleOptions(new HashMap<String, String>(), new HashMap<String, String>(), new HashMap<String, String>()),
                new ServerHealthStateOperationResult());
        verify(goConfigService, times(1)).pipelineConfigNamed(pipelineConfig.name());

        MaterialUpdateStatusListener statusListener = extractMaterialListenerInstanceFromRegisterCall();
        statusListener.onMaterialUpdate(new MaterialUpdateSuccessfulMessage(material1, 0));
        verify(mockMaterialUpdateStatusNotifier).removeListenerFor(pipelineConfig);

        verify(goConfigService, times(1)).pipelineConfigNamed(pipelineConfig.name());
    }

    @Test
    public void manualTrigger_shouldRequestUpdateOfNewMaterials_WhenPipelineConfigInConfigRepo() {
        HgMaterial material1 = new HgMaterial("url", null);
        HgMaterial material2 = new HgMaterial("url2", null);
        HgMaterialConfig materialConfig1 = new HgMaterialConfig("url", null);
        HgMaterialConfig materialConfig2 = new HgMaterialConfig("url2", null);

        pipelineConfig.addMaterialConfig(materialConfig1);
        pipelineConfig.setOrigin(new RepoConfigOrigin(new ConfigRepoConfig(materialConfig1,"plug"),"revision1"));

        when(materialConfigConverter.toMaterial(materialConfig1)).thenReturn(material1);
        when(materialConfigConverter.toMaterial(materialConfig2)).thenReturn(material2);

        buildCauseProducerService.manualSchedulePipeline(Username.ANONYMOUS, pipelineConfig.name(),
                new ScheduleOptions(new HashMap<String, String>(), new HashMap<String, String>(), new HashMap<String, String>()),
                new ServerHealthStateOperationResult());
        verify(goConfigService, times(1)).pipelineConfigNamed(pipelineConfig.name());

        // updated pipeline config
        PipelineConfig pipelineConfig1 = new PipelineConfig(new CaseInsensitiveString("pipeline"), new MaterialConfigs());
        pipelineConfig1.addMaterialConfig(materialConfig1);
        pipelineConfig1.addMaterialConfig(materialConfig2);
        when(goConfigService.pipelineConfigNamed(pipelineConfig.name())).thenReturn(pipelineConfig1);
        when(goConfigService.hasPipelineNamed(pipelineConfig.name())).thenReturn(true);

        MaterialUpdateStatusListener statusListener = extractMaterialListenerInstanceFromRegisterCall();
        statusListener.onMaterialUpdate(new MaterialUpdateSuccessfulMessage(material1, 0));

        verify(goConfigService, times(2)).pipelineConfigNamed(pipelineConfig.name());

        verify(mockMaterialUpdateService,times(1)).updateMaterial(material1);
        verify(mockMaterialUpdateService,times(1)).updateMaterial(material2);

        statusListener.onMaterialUpdate(new MaterialUpdateSuccessfulMessage(material2, 0));

        verify(mockMaterialUpdateStatusNotifier).removeListenerFor(pipelineConfig1);
    }

    @Test
    public void shouldHandleNoModificationExceptionThrownByAutoBuild() {
        String pipelineName = "pipeline";
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        PipelineConfig config = PipelineConfigMother.pipelineConfig(pipelineName);

        Material svnMaterial = MaterialsMother.defaultMaterials().get(0);
        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("up"), new CaseInsensitiveString("s1"));

        config.materialConfigs().clear();
        config.addMaterialConfig(svnMaterial.config());
        config.addMaterialConfig(dependencyMaterial.config());

        when(pipelineService.getRevisionsBasedOnDependencies(Matchers.<MaterialRevisions>any(), Matchers.<BasicCruiseConfig>any(), Matchers.<CaseInsensitiveString>any())).thenThrow(
                new NoModificationsPresentForDependentMaterialException("P/1/S/1"));
        when(pipelineScheduleQueue.mostRecentScheduled(pipelineName)).thenReturn(BuildCause.createNeverRun());
        Modification modification = ModificationsMother.checkinWithComment("r", "c", new Date(), "f1");
        when(materialRepository.findLatestModification(svnMaterial)).thenReturn(ModificationsMother.createSvnMaterialWithMultipleRevisions(1, modification));
        when(materialRepository.findLatestModification(dependencyMaterial)).thenReturn(new MaterialRevisions(ModificationsMother.changedDependencyMaterialRevision("up", 1, "1", "s", 1, new Date())));
        when(specificMaterialRevisionFactory.create(Matchers.<String>any(), Matchers.<Map<String, String>>any())).thenReturn(MaterialRevisions.EMPTY);
        when(goConfigService.upstreamDependencyGraphOf(Matchers.<String>any(), Matchers.<BasicCruiseConfig>any())).thenReturn(new PipelineConfigDependencyGraph(config));

        MaterialConfigs knownMaterialConfigs = new MaterialConfigs(svnMaterial.config(), dependencyMaterial.config());
        Materials materials = new Materials(svnMaterial, dependencyMaterial);
        when(materialConfigConverter.toMaterials(config.materialConfigs())).thenReturn(materials);
        when(materialExpansionService.expandMaterialConfigsForScheduling(config.materialConfigs())).thenReturn(knownMaterialConfigs);
        when(materialConfigConverter.toMaterials(knownMaterialConfigs)).thenReturn(materials);

        AutoBuild autoBuild = new AutoBuild(goConfigService, pipelineService, pipelineName, new SystemEnvironment(), null, mockServerHealthService);
        ServerHealthState serverHealthState = buildCauseProducerService.newProduceBuildCause(config, autoBuild, result, 12345);

        assertThat(serverHealthState.isSuccess(), is(true));
    }

    private TypeSafeMatcher<ServerHealthState> hasErrorHealthState(final String message, final String description) {
        return new TypeSafeMatcher<ServerHealthState>() {
            @Override
            public boolean matchesSafely(ServerHealthState item) {
                assertThat("isSuccess", item.isSuccess(), is(false));
                assertThat("message", item.getMessage(), is(message));
                assertThat("description", item.getDescription(), is(description));
                return true;
            }
            public void describeTo(Description description) {
                description.appendText("message = [" + message + "] and description = [" + description + "]");
            }
        };
    }

    private TypeSafeMatcher<BuildCause> containsRevisions(final MaterialRevision... revisions) {
        return new TypeSafeMatcher<BuildCause>() {
            @Override
            public boolean matchesSafely(BuildCause item) {
                for (MaterialRevision revision : revisions) {
                    if (!item.getMaterialRevisions().getRevisions().contains(revision)) {
                        return false;
                    }
                }
                return true;
            }

            public void describeTo(Description description) {
                description.appendText("should contain revisions " + Arrays.toString(revisions));
            }
        };
    }

    private OperationResult errorResult() {
        HttpOperationResult operationResult1 = new HttpOperationResult();
        operationResult1.error("something", "else", HealthStateType.general(GLOBAL));
        return operationResult1;
    }
}
