/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import org.apache.commons.collections.map.SingletonMap;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

import static com.thoughtworks.go.util.DataStructureUtils.a;
import static com.thoughtworks.go.util.DataStructureUtils.m;
import static com.thoughtworks.go.util.TestUtils.contains;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StageConfigTest {

    private String md5 = "md5-test";

    @Test
    public void shouldSetPrimitiveAttributes() throws Exception{
        StageConfig config = new StageConfig();
        config.setConfigAttributes(new SingletonMap(StageConfig.NAME, "foo_bar"));
        config.setConfigAttributes(new SingletonMap(StageConfig.FETCH_MATERIALS, "0"));
        config.setConfigAttributes(new SingletonMap(StageConfig.CLEAN_WORKING_DIR, "1"));
        assertThat(config.name(), is(new CaseInsensitiveString("foo_bar")));
        assertThat(config.isFetchMaterials(), is(false));
        assertThat(config.isCleanWorkingDir(), is(true));
    }

    @Test
    public void shouldSetArtifactCleanupOptOutAttribute() throws Exception{
        StageConfig config = new StageConfig();
        assertThat(config.isArtifactCleanupProhibited(), is(false));
        config.setConfigAttributes(new SingletonMap(StageConfig.ARTIFACT_CLEANUP_PROHIBITED, "1"));
        assertThat(config.isArtifactCleanupProhibited(), is(true));
        config.setConfigAttributes(new HashMap());
        assertThat(config.isArtifactCleanupProhibited(), is(true));
        config.setConfigAttributes(new SingletonMap(StageConfig.ARTIFACT_CLEANUP_PROHIBITED, "0"));
        assertThat(config.isArtifactCleanupProhibited(), is(false));
    }

    @Test
    public void shouldRemoveStageLevelAuthorizationWhenInheritingPermissionsFromGroup() {
        StageConfig config = new StageConfig();
        StageConfigMother.addApprovalWithRoles(config, "role1");
        StageConfigMother.addApprovalWithUsers(config, "user1");

        HashMap map = new HashMap();
        List operateUsers = new ArrayList();
        operateUsers.add(nameMap("user1"));
        map.put(StageConfig.OPERATE_USERS, operateUsers);

        List operateRoles = new ArrayList();
        operateRoles.add(nameMap("role1"));
        map.put(StageConfig.OPERATE_ROLES, operateRoles);

        map.put(StageConfig.SECURITY_MODE, "inherit");

        config.setConfigAttributes(map);

        assertThat(config.getApproval().getAuthConfig().isEmpty(), is(true));
    }

    @Test
    public void shouldSetOperateUsers() {
        StageConfig config = new StageConfig();
        HashMap map = new HashMap();
        List operateUsers = new ArrayList();
        operateUsers.add(nameMap("user1"));
        operateUsers.add(nameMap("user1"));
        operateUsers.add(nameMap("user2"));
        map.put(StageConfig.OPERATE_USERS, operateUsers);
        map.put(StageConfig.OPERATE_ROLES, new ArrayList());
        map.put(StageConfig.SECURITY_MODE, "define");

        config.setConfigAttributes(map);

        assertThat(config.getOperateUsers().size(), is(2));
        assertThat(config.getOperateUsers(), hasItem(new AdminUser(new CaseInsensitiveString("user1"))));
        assertThat(config.getOperateUsers(), hasItem(new AdminUser(new CaseInsensitiveString("user2"))));
    }

    @Test
    public void shouldSetOperateRoles() {
        StageConfig config = new StageConfig();
        HashMap map = new HashMap();
        List operateRoles = new ArrayList();
        operateRoles.add(nameMap("role1"));
        operateRoles.add(nameMap("role1"));
        operateRoles.add(nameMap("role2"));
        map.put(StageConfig.OPERATE_ROLES, operateRoles);
        map.put(StageConfig.OPERATE_USERS, new ArrayList());
        map.put(StageConfig.SECURITY_MODE, "define");

        config.setConfigAttributes(map);

        assertThat(config.getOperateRoles().size(), is(2));
        assertThat(config.getOperateRoles(), hasItem(new AdminRole(new CaseInsensitiveString("role1"))));
        assertThat(config.getOperateRoles(), hasItem(new AdminRole(new CaseInsensitiveString("role2"))));
    }

    private Map nameMap(final String name) {
        Map valueHashMap = new HashMap();
        valueHashMap.put("name", name);
        return valueHashMap;
    }

    @Test
    public void shouldPopulateEnvironmentVariablesFromAttributeMap() {
        StageConfig stageConfig = new StageConfig();
        HashMap map = new HashMap();
        HashMap valueHashMap = new HashMap();
        valueHashMap.put("name", "FOO");
        valueHashMap.put("value", "BAR");
        map.put(StageConfig.ENVIRONMENT_VARIABLES, valueHashMap);
        EnvironmentVariablesConfig mockEnvironmentVariablesConfig = mock(EnvironmentVariablesConfig.class);
        stageConfig.setVariables(mockEnvironmentVariablesConfig);

        stageConfig.setConfigAttributes(map);

        verify(mockEnvironmentVariablesConfig).setConfigAttributes(valueHashMap);
    }

    @Test
    public void shouldSetApprovalFromConfigAttrs() throws Exception{
        StageConfig config = new StageConfig();
        config.setConfigAttributes(new SingletonMap(StageConfig.APPROVAL, new SingletonMap(Approval.TYPE, Approval.MANUAL)));
        assertThat(config.getApproval().getType(), is(Approval.MANUAL));
        config.setConfigAttributes(new HashMap());
        assertThat(config.getApproval().getType(), is(Approval.MANUAL));

        config.setConfigAttributes(new SingletonMap(StageConfig.APPROVAL, new SingletonMap(Approval.TYPE, Approval.SUCCESS)));
        assertThat(config.getApproval().getType(), is(Approval.SUCCESS));
        config.setConfigAttributes(new HashMap());
        assertThat(config.getApproval().getType(), is(Approval.SUCCESS));
    }

    @Test
    public void shouldPickupJobConfigDetailsFromAttributeMap() throws Exception{
        StageConfig config = new StageConfig();
        Map stageAttrs = m(StageConfig.JOBS, a(m(JobConfig.NAME, "con-job"), m(JobConfig.NAME, "boring-job")));
        config.setConfigAttributes(stageAttrs);
        assertThat(config.getJobs().get(0).name(), is(new CaseInsensitiveString("con-job")));
        assertThat(config.getJobs().get(1).name(), is(new CaseInsensitiveString("boring-job")));
    }

    @Test public void shouldFindCorrectJobIfJobIsOnAllAgents() throws Exception {
        JobConfig allAgentsJob = new JobConfig("job-for-all-agents");
        allAgentsJob.setRunOnAllAgents(true);

        JobConfigs jobs = new JobConfigs();
        jobs.add(allAgentsJob);
        jobs.add(new JobConfig("job"));
        StageConfig stage = new StageConfig(new CaseInsensitiveString("stage-name"), jobs);

        JobConfig found = stage.jobConfigByInstanceName("job-for-all-agents-" + RunOnAllAgentsJobTypeConfig.MARKER + "-1", true);
        assertThat(found, is(allAgentsJob));
    }

    @Test public void shouldFindCorrectJobIfJobIsOnAllAgentsAndAmbiguousName() throws Exception {
        JobConfig allAgentsJob = new JobConfig("job-for-all-agents");
        JobConfig ambiguousJob = new JobConfig("job-for-all");

        allAgentsJob.setRunOnAllAgents(true);
        ambiguousJob.setRunOnAllAgents(true);

        JobConfigs jobs = new JobConfigs();
        jobs.add(ambiguousJob);
        jobs.add(allAgentsJob);
        StageConfig stage = new StageConfig(new CaseInsensitiveString("stage-name"), jobs);

        JobConfig found = stage.jobConfigByInstanceName(RunOnAllAgents.CounterBasedJobNameGenerator.appendMarker("job-for-all-agents", 1), true);
        assertThat(found, is(allAgentsJob));
    }

    @Test
    public void shouldReturnTrueIfStageHasTests() {
        StageConfig stageWithTests = StageConfigMother.stageConfigWithArtifact("stage1", "job1", ArtifactType.unit);

        StageConfig stageWithoutTests = StageConfigMother.stageConfigWithArtifact("stage2", "job2", ArtifactType.file);
        assertThat(stageWithTests.hasTests(), is(true));
        assertThat(stageWithoutTests.hasTests(), is(false));
    }

    @Test
    public void shouldPopulateErrorMessagesWhenHasJobNamesRepeated() {
        CruiseConfig config = new BasicCruiseConfig();
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage-1", "con-job");
        config.addPipeline("group-foo", pipelineConfig);
        StageConfig stageConfig = pipelineConfig.get(0);

        JobConfig newJob = new JobConfig("foo!");
        StageConfig newlyAddedStage = new StageConfig(new CaseInsensitiveString("."), new JobConfigs(newJob));
        pipelineConfig.addStageWithoutValidityAssertion(newlyAddedStage);

        stageConfig.getJobs().addJobWithoutValidityAssertion(new JobConfig(new CaseInsensitiveString("con-job"), new Resources(), new ArtifactPlans(), new Tasks(new ExecTask("ls", "-la", "foo"))));

        List<ConfigErrors> allErrors = config.validateAfterPreprocess();
        assertThat(allErrors.size(), is(4));
        assertThat(allErrors.get(0).on(JobConfig.NAME), is("You have defined multiple jobs called 'con-job'. Job names are case-insensitive and must be unique."));
        assertThat(allErrors.get(1).on(JobConfig.NAME), is("You have defined multiple jobs called 'con-job'. Job names are case-insensitive and must be unique."));
        assertThat(allErrors.get(2).on(StageConfig.NAME), is("Invalid stage name '.'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
        assertThat(allErrors.get(3).on(JobConfig.NAME), is("Invalid job name 'foo!'. This must be alphanumeric and may contain underscores and periods. The maximum allowed length is 255 characters."));
        assertThat(stageConfig.getJobs().get(0).errors().on(JobConfig.NAME), is("You have defined multiple jobs called 'con-job'. Job names are case-insensitive and must be unique."));
        assertThat(stageConfig.getJobs().get(1).errors().on(JobConfig.NAME), is("You have defined multiple jobs called 'con-job'. Job names are case-insensitive and must be unique."));
        assertThat(newlyAddedStage.errors().on(StageConfig.NAME), is("Invalid stage name '.'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
        assertThat(newJob.errors().on(JobConfig.NAME), is("Invalid job name 'foo!'. This must be alphanumeric and may contain underscores and periods. The maximum allowed length is 255 characters."));
    }

    @Test
    public void shouldReturnAllTheUsersAndRoleThatCanOperateThisStage() {
        StageConfig stage = StageConfigMother.stageConfig("stage");
        StageConfigMother.addApprovalWithUsers(stage, "user1", "user2");
        StageConfigMother.addApprovalWithRoles(stage, "role1", "role2");

        assertThat(stage.getOperateUsers(), is(Arrays.asList(new AdminUser(new CaseInsensitiveString("user1")), new AdminUser(new CaseInsensitiveString("user2")))));
        assertThat(stage.getOperateRoles(), is(Arrays.asList(new AdminRole(new CaseInsensitiveString("role1")), new AdminRole(new CaseInsensitiveString("role2")))));
    }

    @Test
    public void shouldFailValidationWhenNameIsBlank(){
        StageConfig stageConfig = new StageConfig();
        stageConfig.validate(null);
        assertThat(stageConfig.errors().on(StageConfig.NAME), contains("Invalid stage name 'null'"));
        stageConfig.setName(null);
        stageConfig.validate(null);
        assertThat(stageConfig.errors().on(StageConfig.NAME), contains("Invalid stage name 'null'"));
        stageConfig.setName(new CaseInsensitiveString(""));
        stageConfig.validate(null);
        assertThat(stageConfig.errors().on(StageConfig.NAME), contains("Invalid stage name 'null'"));
    }

    @Test
    public void shouldValidateTree(){
        EnvironmentVariablesConfig variables = mock(EnvironmentVariablesConfig.class);
        JobConfigs jobConfigs = mock(JobConfigs.class);
        Approval approval = mock(Approval.class);
        StageConfig stageConfig = new StageConfig(new CaseInsensitiveString("stage$"), jobConfigs, approval);
        stageConfig.setVariables(variables);

        stageConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new PipelineConfig(), stageConfig));

        assertThat(stageConfig.errors().on(StageConfig.NAME), contains("Invalid stage name 'stage$'"));
        ArgumentCaptor<PipelineConfigSaveValidationContext> captor = ArgumentCaptor.forClass(PipelineConfigSaveValidationContext.class);
        verify(jobConfigs).validateTree(captor.capture());
        PipelineConfigSaveValidationContext childContext = captor.getValue();
        assertThat((StageConfig) childContext.getParent(), is(stageConfig));
        verify(approval).validateTree(childContext);
        verify(variables).validateTree(childContext);
    }

    @Test
    public void shouldAddValidateTreeErrorsOnStageConfigIfPipelineIsAssociatedToATemplate(){
        Approval approval = mock(Approval.class);
        JobConfigs jobConfigs = mock(JobConfigs.class);
        ConfigErrors jobErrors = new ConfigErrors();
        jobErrors.add("KEY", "ERROR");
        when(jobConfigs.errors()).thenReturn(jobErrors);
        StageConfig stageConfig = new StageConfig(new CaseInsensitiveString("stage$"), jobConfigs, approval);

        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.setTemplateName(new CaseInsensitiveString("template"));
        stageConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", pipelineConfig, stageConfig));

        assertThat(stageConfig.errors().on(StageConfig.NAME), contains("Invalid stage name 'stage$'"));
    }

    @Test
    public void shouldReturnTrueIfAllDescendentsAreValid(){
        EnvironmentVariablesConfig variables = mock(EnvironmentVariablesConfig.class);
        JobConfigs jobConfigs = mock(JobConfigs.class);
        Approval approval = mock(Approval.class);
        when(variables.validateTree(Matchers.<PipelineConfigSaveValidationContext>any())).thenReturn(true);
        when(jobConfigs.validateTree(Matchers.<PipelineConfigSaveValidationContext>any())).thenReturn(true);
        when(approval.validateTree(Matchers.<PipelineConfigSaveValidationContext>any())).thenReturn(true);

        StageConfig stageConfig = new StageConfig(new CaseInsensitiveString("p1"), jobConfigs);
        stageConfig.setVariables(variables);
        stageConfig.setApproval(approval);

        boolean isValid = stageConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new PipelineConfig(), stageConfig));
        assertTrue(isValid);

        verify(jobConfigs).validateTree(Matchers.<PipelineConfigSaveValidationContext>any());
        verify(variables).validateTree(Matchers.<PipelineConfigSaveValidationContext>any());
        verify(approval).validateTree(Matchers.<PipelineConfigSaveValidationContext>any());
    }

    @Test
    public void shouldReturnFalseIfAnyDescendentIsInValid(){
        EnvironmentVariablesConfig variables = mock(EnvironmentVariablesConfig.class);
        JobConfigs jobConfigs = mock(JobConfigs.class);
        Approval approval = mock(Approval.class);
        when(variables.validateTree(Matchers.<PipelineConfigSaveValidationContext>any())).thenReturn(false);
        when(jobConfigs.validateTree(Matchers.<PipelineConfigSaveValidationContext>any())).thenReturn(false);
        when(approval.validateTree(Matchers.<PipelineConfigSaveValidationContext>any())).thenReturn(false);

        StageConfig stageConfig = new StageConfig(new CaseInsensitiveString("p1"), jobConfigs);
        stageConfig.setVariables(variables);
        stageConfig.setApproval(approval);

        boolean isValid = stageConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new PipelineConfig(), stageConfig));
        assertFalse(isValid);

        verify(jobConfigs).validateTree(Matchers.<PipelineConfigSaveValidationContext>any());
        verify(variables).validateTree(Matchers.<PipelineConfigSaveValidationContext>any());
        verify(approval).validateTree(Matchers.<PipelineConfigSaveValidationContext>any());

    }
}
