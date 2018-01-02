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
import com.thoughtworks.go.utils.Timeout;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;

import static com.thoughtworks.go.serverhealth.HealthStateScope.*;
import static com.thoughtworks.go.serverhealth.ServerHealthMatcher.doesNotContainState;
import static com.thoughtworks.go.serverhealth.ServerHealthState.warning;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.*;

public class ServerHealthServiceTest {
    private ServerHealthService serverHealthService;
    private HealthStateType globalId;
    private HealthStateType groupId;
    private HealthStateType pipelineId;
    private static final String PIPELINE_NAME = "pipeline";
    private TestingClock testingClock;


    @Before
    public void setUp() throws Exception {
        serverHealthService = new ServerHealthService();
        globalId = HealthStateType.general(GLOBAL);
        pipelineId = HealthStateType.general(forPipeline(PIPELINE_NAME));
        groupId = HealthStateType.invalidLicense(forGroup("group"));
        testingClock = new TestingClock();
        ServerHealthState.clock = testingClock;
    }

    @After
    public void tearDown() {
        ServerHealthState.clock = new SystemTimeClock();
    }

    @Test
    public void shouldRemoveExpiredLogMessages() throws Exception {
        testingClock.setTime(new DateTime(2002,10,10,10,10,10,10));
        ServerHealthState expiresInNintySecs = warning("hg-message1", "description", HealthStateType.databaseDiskFull(), Timeout.NINETY_SECONDS);
        ServerHealthState expiresInThreeMins = warning("hg-message2", "description", HealthStateType.artifactsDirChanged(), Timeout.THREE_MINUTES);
        ServerHealthState expiresNever = warning("hg-message3", "description", HealthStateType.artifactsDiskFull(), Timeout.NEVER);
        serverHealthService.update(expiresInThreeMins);
        serverHealthService.update(expiresInNintySecs);
        serverHealthService.update(expiresNever);
        ServerHealthStates logs = serverHealthService.getAllValidLogs(new BasicCruiseConfig());
        assertThat(logs.size(), is(3));
        assertThat(logs, hasItems(expiresInThreeMins,expiresInNintySecs, expiresNever));
        testingClock.addSeconds(100);
        logs = serverHealthService.getAllValidLogs(new BasicCruiseConfig());
        assertThat(logs.size(), is(2));
        assertThat(logs,hasItems(expiresInThreeMins, expiresNever));
        testingClock.addMillis((int) Timeout.TWO_MINUTES.inMillis());
        logs = serverHealthService.getAllValidLogs(new BasicCruiseConfig());
        assertThat(logs.size(), is(1));
        assertThat(logs,hasItem(expiresNever));
    }

    @Test
    public void shouldRemoveErrorLogWhenCorrespondingMaterialIsMissing() throws Exception {
        serverHealthService.update(ServerHealthState.error("hg-message", "description", HealthStateType.general(forMaterial(MaterialsMother.hgMaterial()))));
        SvnMaterialConfig svnMaterialConfig = MaterialConfigsMother.svnMaterialConfig();
        serverHealthService.update(ServerHealthState.error("svn-message", "description", HealthStateType.general(forMaterialConfig(svnMaterialConfig))));
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.addPipeline("defaultGroup", new PipelineConfig(new CaseInsensitiveString("dev"), new MaterialConfigs(svnMaterialConfig), new StageConfig(new CaseInsensitiveString("first"), new JobConfigs())));
        assertThat(serverHealthService.getAllValidLogs(cruiseConfig).size(), is(1));
    }

    @Test
    public void shouldRemoveErrorLogWhenCorrespondingPipelineIsMissing() throws Exception {
        serverHealthService.update(ServerHealthState.error("message", "description", pipelineId));
        serverHealthService.update(ServerHealthState.error("message", "description", HealthStateType.general(forPipeline("other"))));

        assertThat(serverHealthService.getAllValidLogs(new BasicCruiseConfig()).size(), is(0));
    }

    @Test
    public void shouldRemoveErrorLogWhenCorrespondingGroupIsMissing() throws Exception {
        serverHealthService.update(ServerHealthState.error("message", "description", groupId));

        assertThat(serverHealthService.getAllValidLogs(new BasicCruiseConfig()).size(), is(0));
    }

    @Test
    public void shouldReturnErrorLogs() throws Exception {
        serverHealthService.update(ServerHealthState.error("message", "description", pipelineId));

        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        new GoConfigMother().addPipeline(cruiseConfig, PIPELINE_NAME, "stageName", "jon");
        assertThat(serverHealthService.getAllValidLogs(cruiseConfig).size(), is(1));
    }

    @Test
    public void shouldUpdateLogInServerHealth() throws Exception {
        ServerHealthState serverHealthState = ServerHealthState.error("message", "description", globalId);
        serverHealthService.update(serverHealthState);
        ServerHealthState newServerHealthState = ServerHealthState.error("updated message", "updated description", globalId);
        serverHealthService.update(newServerHealthState);

        assertThat(serverHealthService.getAllLogs().size(), is(1));
        assertThat(serverHealthService, ServerHealthMatcher.containsState(globalId, HealthStateLevel.ERROR, "updated message"));
    }

    @Test
    public void shouldAddMultipleLogToServerHealth() throws Exception {
        assertThat(serverHealthService.update(ServerHealthState.error("message", "description", globalId)), is(globalId));
        assertThat(serverHealthService.update(ServerHealthState.error("message", "description", pipelineId)), is(pipelineId));

        assertThat(serverHealthService.getAllLogs().size(), is(2));
        assertThat(serverHealthService, ServerHealthMatcher.containsState(globalId));
        assertThat(serverHealthService, ServerHealthMatcher.containsState(pipelineId));
    }

    @Test
    public void shouldRemoveLogWhenUpdateIsFine() throws Exception {
        serverHealthService.update(ServerHealthState.error("message", "description", globalId));
        assertThat(serverHealthService, ServerHealthMatcher.containsState(globalId));

        assertThat(serverHealthService.update(ServerHealthState.success(globalId)), is(nullValue()));
        assertThat(serverHealthService, doesNotContainState(globalId));
    }

    @Test
    public void shouldRemoveLogByCategoryFromServerHealth() throws Exception {
        HealthStateScope scope = forPipeline(PIPELINE_NAME);

        serverHealthService.update(ServerHealthState.error("message", "description", HealthStateType.general(scope)));
        serverHealthService.update(ServerHealthState.error("message", "description", HealthStateType.invalidLicense(scope)));
        serverHealthService.update(ServerHealthState.error("message", "description", globalId));

        assertThat(serverHealthService.getAllLogs().size(), is(3));

        serverHealthService.removeByScope(scope);
        assertThat(serverHealthService.getAllLogs().size(), is(1));
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
        assertThat((serverHealthService.getAllLogs().get(0)).getPipelineNames(config), equalTo(Collections.singleton(PIPELINE_NAME)));

        serverHealthService = new ServerHealthService();
        serverHealthService.update(ServerHealthState.error("message", "description", HealthStateType.general(forStage(PIPELINE_NAME, "stage1"))));
        assertThat((serverHealthService.getAllLogs().get(0)).getPipelineNames(config), equalTo(Collections.singleton(PIPELINE_NAME)));


        serverHealthService = new ServerHealthService();
        serverHealthService.update(ServerHealthState.error("message", "description", HealthStateType.general(forJob(PIPELINE_NAME, "stage1", "job1"))));
        assertThat((serverHealthService.getAllLogs().get(0)).getPipelineNames(config), equalTo(Collections.singleton(PIPELINE_NAME)));
    }

    @Test
    public void globalStateRelatedPipelineNames() {
        HgMaterial hgMaterial = MaterialsMother.hgMaterial();
        CruiseConfig config = new BasicCruiseConfig();
        config.addPipeline("group", PipelineConfigMother.pipelineConfig(PIPELINE_NAME, new MaterialConfigs(hgMaterial.config())));
        config.addPipeline("group", PipelineConfigMother.pipelineConfig("pipeline2", new MaterialConfigs(hgMaterial.config())));
        config.addPipeline("group", PipelineConfigMother.pipelineConfig("pipeline3"));

        serverHealthService.update(ServerHealthState.error("message", "description", HealthStateType.invalidConfig()));
        assertTrue((serverHealthService.getAllLogs().get(0)).getPipelineNames(config).isEmpty());

    }

    @Test
    public void noPipelineMatchMaterialStateRelatedPipelineNames() {
        HgMaterial hgMaterial = MaterialsMother.hgMaterial();
        CruiseConfig config = new BasicCruiseConfig();
        config.addPipeline("group", PipelineConfigMother.pipelineConfig(PIPELINE_NAME, new MaterialConfigs(hgMaterial.config())));
        config.addPipeline("group", PipelineConfigMother.pipelineConfig("pipeline2", new MaterialConfigs(hgMaterial.config())));
        config.addPipeline("group", PipelineConfigMother.pipelineConfig("pipeline3"));

        serverHealthService.update(ServerHealthState.error("message", "description", HealthStateType.general(forMaterial(MaterialsMother.p4Material()))));
        assertTrue((serverHealthService.getAllLogs().get(0)).getPipelineNames(config).isEmpty());
    }

    @Test
    public void materialStateRelatedPipelineNames() {
        HgMaterial hgMaterial = MaterialsMother.hgMaterial();
        CruiseConfig config = new BasicCruiseConfig();
        config.addPipeline("group", PipelineConfigMother.pipelineConfig(PIPELINE_NAME, new MaterialConfigs(hgMaterial.config())));
        config.addPipeline("group", PipelineConfigMother.pipelineConfig("pipeline2", new MaterialConfigs(hgMaterial.config())));
        config.addPipeline("group", PipelineConfigMother.pipelineConfig("pipeline3"));

        serverHealthService.update(ServerHealthState.error("message", "description", HealthStateType.general(forMaterial(hgMaterial))));
        Set<String> pipelines = (serverHealthService.getAllLogs().get(0)).getPipelineNames(config);
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
        Set<String> pipelines = (serverHealthService.getAllLogs().get(0)).getPipelineNames(config);
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
        Set<String> pipelines = (serverHealthService.getAllLogs().get(0)).getPipelineNames(config);
        assertEquals(Sets.newHashSet("pipeline2"), pipelines);
    }
}
