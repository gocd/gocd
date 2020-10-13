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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static com.thoughtworks.go.util.DataStructureUtils.a;
import static com.thoughtworks.go.util.DataStructureUtils.m;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class StageConfigTest {

    @Test
    void shouldSetPrimitiveAttributes() throws Exception {
        StageConfig config = new StageConfig();
        config.setConfigAttributes(Collections.singletonMap(StageConfig.NAME, "foo_bar"));
        config.setConfigAttributes(Collections.singletonMap(StageConfig.FETCH_MATERIALS, "0"));
        config.setConfigAttributes(Collections.singletonMap(StageConfig.CLEAN_WORKING_DIR, "1"));
        assertThat(config.name()).isEqualTo(new CaseInsensitiveString("foo_bar"));
        assertThat(config.isFetchMaterials()).isFalse();
        assertThat(config.isCleanWorkingDir()).isTrue();
    }

    @Test
    void shouldSetArtifactCleanupOptOutAttribute() throws Exception {
        StageConfig config = new StageConfig();
        assertThat(config.isArtifactCleanupProhibited()).isFalse();
        config.setConfigAttributes(Collections.singletonMap(StageConfig.ARTIFACT_CLEANUP_PROHIBITED, "1"));
        assertThat(config.isArtifactCleanupProhibited()).isTrue();
        config.setConfigAttributes(new HashMap());
        assertThat(config.isArtifactCleanupProhibited()).isTrue();
        config.setConfigAttributes(Collections.singletonMap(StageConfig.ARTIFACT_CLEANUP_PROHIBITED, "0"));
        assertThat(config.isArtifactCleanupProhibited()).isFalse();
    }

    @Test
    void shouldRemoveStageLevelAuthorizationWhenInheritingPermissionsFromGroup() {
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

        assertThat(config.getApproval().getAuthConfig().isEmpty()).isTrue();
    }

    @Test
    void shouldSetOperateUsers() {
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

        assertThat(config.getOperateUsers().size()).isEqualTo(2);
        assertThat(config.getOperateUsers()).contains(new AdminUser(new CaseInsensitiveString("user1")));
        assertThat(config.getOperateUsers()).contains(new AdminUser(new CaseInsensitiveString("user2")));
    }

    @Test
    void shouldSetOperateRoles() {
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

        assertThat(config.getOperateRoles().size()).isEqualTo(2);
        assertThat(config.getOperateRoles()).contains(new AdminRole(new CaseInsensitiveString("role1")));
        assertThat(config.getOperateRoles()).contains(new AdminRole(new CaseInsensitiveString("role2")));
    }

    private Map nameMap(final String name) {
        Map valueHashMap = new HashMap();
        valueHashMap.put("name", name);
        return valueHashMap;
    }

    @Test
    void shouldPopulateEnvironmentVariablesFromAttributeMap() {
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
    void shouldSetApprovalFromConfigAttrs() throws Exception {
        StageConfig config = new StageConfig();
        config.setConfigAttributes(Collections.singletonMap(StageConfig.APPROVAL, Collections.singletonMap(Approval.TYPE, Approval.MANUAL)));
        assertThat(config.getApproval().getType()).isEqualTo(Approval.MANUAL);
        config.setConfigAttributes(new HashMap());
        assertThat(config.getApproval().getType()).isEqualTo(Approval.MANUAL);

        config.setConfigAttributes(Collections.singletonMap(StageConfig.APPROVAL, Collections.singletonMap(Approval.TYPE, Approval.SUCCESS)));
        assertThat(config.getApproval().getType()).isEqualTo(Approval.SUCCESS);
        config.setConfigAttributes(new HashMap());
        assertThat(config.getApproval().getType()).isEqualTo(Approval.SUCCESS);
    }

    @Test
    void shouldPickupJobConfigDetailsFromAttributeMap() throws Exception {
        StageConfig config = new StageConfig();
        Map stageAttrs = m(StageConfig.JOBS, a(m(JobConfig.NAME, "con-job"), m(JobConfig.NAME, "boring-job")));
        config.setConfigAttributes(stageAttrs);
        assertThat(config.getJobs().get(0).name()).isEqualTo(new CaseInsensitiveString("con-job"));
        assertThat(config.getJobs().get(1).name()).isEqualTo(new CaseInsensitiveString("boring-job"));
    }

    @Test
    void shouldFindCorrectJobIfJobIsOnAllAgents() throws Exception {
        JobConfig allAgentsJob = new JobConfig("job-for-all-agents");
        allAgentsJob.setRunOnAllAgents(true);

        JobConfigs jobs = new JobConfigs();
        jobs.add(allAgentsJob);
        jobs.add(new JobConfig("job"));
        StageConfig stage = new StageConfig(new CaseInsensitiveString("stage-name"), jobs);

        JobConfig found = stage.jobConfigByInstanceName("job-for-all-agents-" + RunOnAllAgentsJobTypeConfig.MARKER + "-1", true);
        assertThat(found).isEqualTo(allAgentsJob);
    }

    @Test
    void shouldFindCorrectJobIfJobIsOnAllAgentsAndAmbiguousName() throws Exception {
        JobConfig allAgentsJob = new JobConfig("job-for-all-agents");
        JobConfig ambiguousJob = new JobConfig("job-for-all");

        allAgentsJob.setRunOnAllAgents(true);
        ambiguousJob.setRunOnAllAgents(true);

        JobConfigs jobs = new JobConfigs();
        jobs.add(ambiguousJob);
        jobs.add(allAgentsJob);
        StageConfig stage = new StageConfig(new CaseInsensitiveString("stage-name"), jobs);

        JobConfig found = stage.jobConfigByInstanceName(RunOnAllAgents.CounterBasedJobNameGenerator.appendMarker("job-for-all-agents", 1), true);
        assertThat(found).isEqualTo(allAgentsJob);
    }

    @Test
    void shouldReturnTrueIfStageHasTests() {
        StageConfig stageWithTests = StageConfigMother.stageConfigWithArtifact("stage1", "job1", ArtifactType.test);

        StageConfig stageWithoutTests = StageConfigMother.stageConfigWithArtifact("stage2", "job2", ArtifactType.build);
        assertThat(stageWithTests.hasTests()).isTrue();
        assertThat(stageWithoutTests.hasTests()).isFalse();
    }

    @Test
    void shouldPopulateErrorMessagesWhenHasJobNamesRepeated() {
        CruiseConfig config = new BasicCruiseConfig();
        config.initializeServer();
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage-1", "con-job");
        config.addPipeline("group-foo", pipelineConfig);
        StageConfig stageConfig = pipelineConfig.get(0);

        JobConfig newJob = new JobConfig("foo!");
        StageConfig newlyAddedStage = new StageConfig(new CaseInsensitiveString("."), new JobConfigs(newJob));
        pipelineConfig.addStageWithoutValidityAssertion(newlyAddedStage);

        stageConfig.getJobs().addJobWithoutValidityAssertion(new JobConfig(new CaseInsensitiveString("con-job"), new ResourceConfigs(), new ArtifactTypeConfigs(), new Tasks(new ExecTask("ls", "-la", "foo"))));

        List<ConfigErrors> allErrors = config.validateAfterPreprocess();
        assertThat(allErrors.size()).isEqualTo(4);
        assertThat(allErrors.get(0).on(JobConfig.NAME)).isEqualTo("You have defined multiple jobs called 'con-job'. Job names are case-insensitive and must be unique.");
        assertThat(allErrors.get(1).on(JobConfig.NAME)).isEqualTo("You have defined multiple jobs called 'con-job'. Job names are case-insensitive and must be unique.");
        assertThat(allErrors.get(2).on(StageConfig.NAME)).isEqualTo("Invalid stage name '.'. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
        assertThat(allErrors.get(3).on(JobConfig.NAME)).isEqualTo("Invalid job name 'foo!'. This must be alphanumeric and may contain underscores and periods. The maximum allowed length is 255 characters.");
        assertThat(stageConfig.getJobs().get(0).errors().on(JobConfig.NAME)).isEqualTo("You have defined multiple jobs called 'con-job'. Job names are case-insensitive and must be unique.");
        assertThat(stageConfig.getJobs().get(1).errors().on(JobConfig.NAME)).isEqualTo("You have defined multiple jobs called 'con-job'. Job names are case-insensitive and must be unique.");
        assertThat(newlyAddedStage.errors().on(StageConfig.NAME)).isEqualTo("Invalid stage name '.'. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
        assertThat(newJob.errors().on(JobConfig.NAME)).isEqualTo("Invalid job name 'foo!'. This must be alphanumeric and may contain underscores and periods. The maximum allowed length is 255 characters.");
    }

    @Test
    void shouldReturnAllTheUsersAndRoleThatCanOperateThisStage() {
        StageConfig stage = StageConfigMother.stageConfig("stage");
        StageConfigMother.addApprovalWithUsers(stage, "user1", "user2");
        StageConfigMother.addApprovalWithRoles(stage, "role1", "role2");

        assertThat(stage.getOperateUsers()).isEqualTo(Arrays.asList(new AdminUser(new CaseInsensitiveString("user1")), new AdminUser(new CaseInsensitiveString("user2"))));
        assertThat(stage.getOperateRoles()).isEqualTo(Arrays.asList(new AdminRole(new CaseInsensitiveString("role1")), new AdminRole(new CaseInsensitiveString("role2"))));
    }

    @Test
    void shouldFailValidationWhenNameIsBlank() {
        StageConfig stageConfig = new StageConfig();
        stageConfig.validate(null);
        assertThat(stageConfig.errors().on(StageConfig.NAME)).contains("Invalid stage name 'null'");
        stageConfig.setName(null);
        stageConfig.validate(null);
        assertThat(stageConfig.errors().on(StageConfig.NAME)).contains("Invalid stage name 'null'");
        stageConfig.setName(new CaseInsensitiveString(""));
        stageConfig.validate(null);
        assertThat(stageConfig.errors().on(StageConfig.NAME)).contains("Invalid stage name 'null'");
    }

    @Test
    void shouldValidateTree() {
        EnvironmentVariablesConfig variables = mock(EnvironmentVariablesConfig.class);
        JobConfigs jobConfigs = mock(JobConfigs.class);
        Approval approval = mock(Approval.class);
        StageConfig stageConfig = new StageConfig(new CaseInsensitiveString("stage$"), jobConfigs, approval);
        stageConfig.setVariables(variables);

        stageConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new PipelineConfig(), stageConfig));

        assertThat(stageConfig.errors().on(StageConfig.NAME)).contains("Invalid stage name 'stage$'");
        ArgumentCaptor<PipelineConfigSaveValidationContext> captor = ArgumentCaptor.forClass(PipelineConfigSaveValidationContext.class);
        verify(jobConfigs).validateTree(captor.capture());
        PipelineConfigSaveValidationContext childContext = captor.getValue();
        assertThat(childContext.getParent()).isEqualTo(stageConfig);
        verify(approval).validateTree(childContext);
        verify(variables).validateTree(childContext);
    }

    @Test
    void shouldAddValidateTreeErrorsOnStageConfigIfPipelineIsAssociatedToATemplate() {
        Approval approval = mock(Approval.class);
        JobConfigs jobConfigs = mock(JobConfigs.class);
        ConfigErrors jobErrors = new ConfigErrors();
        jobErrors.add("KEY", "ERROR");
        when(jobConfigs.errors()).thenReturn(jobErrors);
        StageConfig stageConfig = new StageConfig(new CaseInsensitiveString("stage$"), jobConfigs, approval);

        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.setTemplateName(new CaseInsensitiveString("template"));
        stageConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", pipelineConfig, stageConfig));

        assertThat(stageConfig.errors().on(StageConfig.NAME)).contains("Invalid stage name 'stage$'");
    }

    @Test
    void shouldReturnTrueIfAllDescendentsAreValid() {
        EnvironmentVariablesConfig variables = mock(EnvironmentVariablesConfig.class);
        JobConfigs jobConfigs = mock(JobConfigs.class);
        Approval approval = mock(Approval.class);
        when(variables.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(true);
        when(jobConfigs.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(true);
        when(approval.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(true);

        StageConfig stageConfig = new StageConfig(new CaseInsensitiveString("p1"), jobConfigs);
        stageConfig.setVariables(variables);
        stageConfig.setApproval(approval);

        boolean isValid = stageConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new PipelineConfig(), stageConfig));
        assertTrue(isValid);

        verify(jobConfigs).validateTree(any(PipelineConfigSaveValidationContext.class));
        verify(variables).validateTree(any(PipelineConfigSaveValidationContext.class));
        verify(approval).validateTree(any(PipelineConfigSaveValidationContext.class));
    }

    @Test
    void shouldReturnFalseIfAnyDescendentIsInValid() {
        EnvironmentVariablesConfig variables = mock(EnvironmentVariablesConfig.class);
        JobConfigs jobConfigs = mock(JobConfigs.class);
        Approval approval = mock(Approval.class);
        when(variables.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(false);
        when(jobConfigs.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(false);
        when(approval.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(false);

        StageConfig stageConfig = new StageConfig(new CaseInsensitiveString("p1"), jobConfigs);
        stageConfig.setVariables(variables);
        stageConfig.setApproval(approval);

        boolean isValid = stageConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new PipelineConfig(), stageConfig));
        assertFalse(isValid);

        verify(jobConfigs).validateTree(any(PipelineConfigSaveValidationContext.class));
        verify(variables).validateTree(any(PipelineConfigSaveValidationContext.class));
        verify(approval).validateTree(any(PipelineConfigSaveValidationContext.class));

    }

    @Test
    void shouldValidateElasticProfileId() {
        StageConfig stageConfig = new StageConfig(new CaseInsensitiveString("p1"), new JobConfigs());
        stageConfig.setElasticProfileId("non-existent-profile-id");

        ValidationContext validationContext = mock(ValidationContext.class);
        when(validationContext.isValidProfileId("non-existent-profile-id")).thenReturn(false);

        stageConfig.validate(validationContext);

        assertThat(stageConfig.errors().isEmpty()).isFalse();
        assertThat(stageConfig.errors().on(JobConfig.ELASTIC_PROFILE_ID)).isEqualTo("No profile defined corresponding to profile_id 'non-existent-profile-id'");
    }
}
