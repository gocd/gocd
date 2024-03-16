/*
 * Copyright 2024 Thoughtworks, Inc.
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
package com.thoughtworks.go.serverhealth;


import com.google.common.collect.Sets;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.util.SystemTimeClock;
import com.thoughtworks.go.util.TestingClock;
import com.thoughtworks.go.util.Timeout;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.thoughtworks.go.serverhealth.HealthStateScope.*;
import static com.thoughtworks.go.serverhealth.ServerHealthMatcher.doesNotContainState;
import static com.thoughtworks.go.serverhealth.ServerHealthState.warning;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServerHealthServiceTest {
    private static final String PIPELINE_NAME = "pipeline";
    private ServerHealthService serverHealthService;
    private HealthStateType globalId;
    private HealthStateType pipelineId;
    private TestingClock testingClock;


    @BeforeEach
    public void setUp() throws Exception {
        serverHealthService = new ServerHealthService();
        globalId = HealthStateType.general(GLOBAL);
        pipelineId = HealthStateType.general(forPipeline(PIPELINE_NAME));
        testingClock = new TestingClock();
        ServerHealthState.clock = testingClock;
    }

    @AfterEach
    public void tearDown() {
        ServerHealthState.clock = new SystemTimeClock();
    }

    @Test
    public void shouldRemoveExpiredLogMessages() {
        testingClock.setTime(new DateTime(2002,10,10,10,10,10,10));
        ServerHealthState expiresInNinetySecs = warning("hg-message1", "description", HealthStateType.databaseDiskFull(), Timeout.NINETY_SECONDS);
        ServerHealthState expiresInThreeMinutes = warning("hg-message2", "description", HealthStateType.artifactsDirChanged(), Timeout.THREE_MINUTES);
        ServerHealthState expiresNever = warning("hg-message3", "description", HealthStateType.artifactsDiskFull(), Timeout.NEVER);
        serverHealthService.update(expiresInThreeMinutes);
        serverHealthService.update(expiresInNinetySecs);
        serverHealthService.update(expiresNever);
        serverHealthService.purgeStaleHealthMessages(new BasicCruiseConfig());
        ServerHealthStates logs = serverHealthService.logsSorted();
        assertThat(logs.size(), is(3));
        assertThat(logs, hasItems(expiresInThreeMinutes,expiresInNinetySecs, expiresNever));
        testingClock.addSeconds(100);
        serverHealthService.purgeStaleHealthMessages(new BasicCruiseConfig());
        logs = serverHealthService.logsSorted();
        assertThat(logs.size(), is(2));
        assertThat(logs,hasItems(expiresInThreeMinutes, expiresNever));
        testingClock.addMillis((int) Timeout.TWO_MINUTES.inMillis());
        serverHealthService.purgeStaleHealthMessages(new BasicCruiseConfig());
        logs = serverHealthService.logsSorted();
        assertThat(logs.size(), is(1));
        assertThat(logs,hasItem(expiresNever));
    }

    @Test
    public void shouldRemoveErrorLogWhenCorrespondingMaterialIsMissing() {
        serverHealthService.update(ServerHealthState.error("hg-message", "description", HealthStateType.general(forMaterial(MaterialsMother.hgMaterial()))));
        SvnMaterialConfig svnMaterialConfig = MaterialConfigsMother.svnMaterialConfig();
        serverHealthService.update(ServerHealthState.error("svn-message", "description", HealthStateType.general(forMaterialConfig(svnMaterialConfig))));
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.addPipeline("defaultGroup", new PipelineConfig(new CaseInsensitiveString("dev"), new MaterialConfigs(svnMaterialConfig), new StageConfig(new CaseInsensitiveString("first"), new JobConfigs())));
        serverHealthService.purgeStaleHealthMessages(cruiseConfig);
        assertThat(serverHealthService.logsSorted().size(), is(1));
    }

    @Test
    public void shouldRemoveErrorLogWhenCorrespondingPipelineIsMissing() {
        serverHealthService.update(ServerHealthState.error("message", "description", pipelineId));
        serverHealthService.update(ServerHealthState.error("message", "description", HealthStateType.general(forPipeline("other"))));

        serverHealthService.purgeStaleHealthMessages(new BasicCruiseConfig());
        assertThat(serverHealthService.logsSorted().size(), is(0));
    }

    @Test
    public void shouldRemoveErrorLogWhenCorrespondingGroupIsMissing() {
        serverHealthService.update(ServerHealthState.error("message", "description", HealthStateType.general(forGroup("group"))));

        serverHealthService.purgeStaleHealthMessages(new BasicCruiseConfig());
        assertThat(serverHealthService.logsSorted().size(), is(0));
    }

    @Test
    public void shouldReturnErrorLogs() {
        serverHealthService.update(ServerHealthState.error("message", "description", pipelineId));

        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        new GoConfigMother().addPipeline(cruiseConfig, PIPELINE_NAME, "stageName", "jon");
        serverHealthService.purgeStaleHealthMessages(cruiseConfig);
        assertThat(serverHealthService.logsSorted().size(), is(1));
    }

    @Test
    public void shouldUpdateLogInServerHealth() {
        ServerHealthState serverHealthState = ServerHealthState.error("message", "description", globalId);
        serverHealthService.update(serverHealthState);
        ServerHealthState newServerHealthState = ServerHealthState.error("updated message", "updated description", globalId);
        serverHealthService.update(newServerHealthState);

        assertThat(serverHealthService.logsSorted().size(), is(1));
        assertThat(serverHealthService, ServerHealthMatcher.containsState(globalId, HealthStateLevel.ERROR, "updated message"));
    }

    @Test
    public void shouldAddMultipleLogToServerHealth() {
        assertThat(serverHealthService.update(ServerHealthState.error("message", "description", globalId)), is(globalId));
        assertThat(serverHealthService.update(ServerHealthState.error("message", "description", pipelineId)), is(pipelineId));

        assertThat(serverHealthService.logsSorted().size(), is(2));
        assertThat(serverHealthService, ServerHealthMatcher.containsState(globalId));
        assertThat(serverHealthService, ServerHealthMatcher.containsState(pipelineId));
    }

    @Test
    public void shouldRemoveLogWhenUpdateIsFine() {
        serverHealthService.update(ServerHealthState.error("message", "description", globalId));
        assertThat(serverHealthService, ServerHealthMatcher.containsState(globalId));

        assertThat(serverHealthService.update(ServerHealthState.success(globalId)), is(nullValue()));
        assertThat(serverHealthService, doesNotContainState(globalId));
    }

    @Test
    public void shouldRemoveLogByCategoryFromServerHealth() {
        HealthStateScope scope = forPipeline(PIPELINE_NAME);

        serverHealthService.update(ServerHealthState.error("message", "description", HealthStateType.general(scope)));
        serverHealthService.update(ServerHealthState.error("message", "description", globalId));

        assertThat(serverHealthService.logsSorted().size(), is(2));

        serverHealthService.removeByScope(scope);
        assertThat(serverHealthService.logsSorted().size(), is(1));
        assertThat(serverHealthService, ServerHealthMatcher.containsState(globalId));
    }

    @Test
    public void stateRelatedPipelineNames() {
        HgMaterial hgMaterial = MaterialsMother.hgMaterial();
        CruiseConfig config = new BasicCruiseConfig();
        config.addPipeline("group", PipelineConfigMother.pipelineConfig(PIPELINE_NAME, new MaterialConfigs(hgMaterial.config())));
        config.addPipeline("group", PipelineConfigMother.pipelineConfig("pipeline2", new MaterialConfigs(hgMaterial.config())));
        config.addPipeline("group", PipelineConfigMother.pipelineConfig("pipeline3"));

        serverHealthService = new ServerHealthService();
        serverHealthService.update(ServerHealthState.error("message", "description", HealthStateType.general(forPipeline(PIPELINE_NAME))));
        assertThat((serverHealthService.logsSorted().get(0)).getPipelineNames(config), equalTo(Set.of(PIPELINE_NAME)));

        serverHealthService = new ServerHealthService();
        serverHealthService.update(ServerHealthState.error("message", "description", HealthStateType.general(forStage(PIPELINE_NAME, "stage1"))));
        assertThat((serverHealthService.logsSorted().get(0)).getPipelineNames(config), equalTo(Set.of(PIPELINE_NAME)));


        serverHealthService = new ServerHealthService();
        serverHealthService.update(ServerHealthState.error("message", "description", HealthStateType.general(forJob(PIPELINE_NAME, "stage1", "job1"))));
        assertThat((serverHealthService.logsSorted().get(0)).getPipelineNames(config), equalTo(Set.of(PIPELINE_NAME)));
    }

    @Test
    public void globalStateRelatedPipelineNames() {
        HgMaterial hgMaterial = MaterialsMother.hgMaterial();
        CruiseConfig config = new BasicCruiseConfig();
        config.addPipeline("group", PipelineConfigMother.pipelineConfig(PIPELINE_NAME, new MaterialConfigs(hgMaterial.config())));
        config.addPipeline("group", PipelineConfigMother.pipelineConfig("pipeline2", new MaterialConfigs(hgMaterial.config())));
        config.addPipeline("group", PipelineConfigMother.pipelineConfig("pipeline3"));

        serverHealthService.update(ServerHealthState.error("message", "description", HealthStateType.invalidConfig()));
        assertTrue((serverHealthService.logsSorted().get(0)).getPipelineNames(config).isEmpty());

    }

    @Test
    public void noPipelineMatchMaterialStateRelatedPipelineNames() {
        HgMaterial hgMaterial = MaterialsMother.hgMaterial();
        CruiseConfig config = new BasicCruiseConfig();
        config.addPipeline("group", PipelineConfigMother.pipelineConfig(PIPELINE_NAME, new MaterialConfigs(hgMaterial.config())));
        config.addPipeline("group", PipelineConfigMother.pipelineConfig("pipeline2", new MaterialConfigs(hgMaterial.config())));
        config.addPipeline("group", PipelineConfigMother.pipelineConfig("pipeline3"));

        serverHealthService.update(ServerHealthState.error("message", "description", HealthStateType.general(forMaterial(MaterialsMother.p4Material()))));
        assertTrue((serverHealthService.logsSorted().get(0)).getPipelineNames(config).isEmpty());
    }

    @Test
    public void materialStateRelatedPipelineNames() {
        HgMaterial hgMaterial = MaterialsMother.hgMaterial();
        CruiseConfig config = new BasicCruiseConfig();
        config.addPipeline("group", PipelineConfigMother.pipelineConfig(PIPELINE_NAME, new MaterialConfigs(hgMaterial.config())));
        config.addPipeline("group", PipelineConfigMother.pipelineConfig("pipeline2", new MaterialConfigs(hgMaterial.config())));
        config.addPipeline("group", PipelineConfigMother.pipelineConfig("pipeline3"));

        serverHealthService.update(ServerHealthState.error("message", "description", HealthStateType.general(forMaterial(hgMaterial))));
        Set<String> pipelines = (serverHealthService.logsSorted().get(0)).getPipelineNames(config);
        assertEquals(Sets.newHashSet("pipeline", "pipeline2"), pipelines);
    }

    @Test
    public void materialUpdateStateRelatedPipelineNames() {
        HgMaterial hgMaterial = MaterialsMother.hgMaterial();
        CruiseConfig config = new BasicCruiseConfig();
        config.addPipeline("group", PipelineConfigMother.pipelineConfig(PIPELINE_NAME, new MaterialConfigs(hgMaterial.config())));
        config.addPipeline("group", PipelineConfigMother.pipelineConfig("pipeline2", new MaterialConfigs(hgMaterial.config())));
        config.addPipeline("group", PipelineConfigMother.pipelineConfig("pipeline3"));

        serverHealthService.update(ServerHealthState.error("message", "description", HealthStateType.general(forMaterialUpdate(hgMaterial))));
        Set<String> pipelines = (serverHealthService.logsSorted().get(0)).getPipelineNames(config);
        assertEquals(Sets.newHashSet("pipeline", "pipeline2"), pipelines);
    }

    @Test
    public void faninErrorStateRelatedPipelineNames() {
        HgMaterial hgMaterial = MaterialsMother.hgMaterial();
        CruiseConfig config = new BasicCruiseConfig();
        config.addPipeline("group", PipelineConfigMother.pipelineConfig(PIPELINE_NAME, new MaterialConfigs(hgMaterial.config())));
        config.addPipeline("group", PipelineConfigMother.pipelineConfig("pipeline2", new MaterialConfigs(hgMaterial.config())));
        config.addPipeline("group", PipelineConfigMother.pipelineConfig("pipeline3"));

        serverHealthService.update(ServerHealthState.error("message", "description", HealthStateType.general(HealthStateScope.forFanin("pipeline2"))));
        Set<String> pipelines = (serverHealthService.logsSorted().get(0)).getPipelineNames(config);
        assertEquals(Sets.newHashSet("pipeline2"), pipelines);
    }
}
