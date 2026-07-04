/*
 * Copyright Thoughtworks, Inc.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.config.Approval.*;
import static com.thoughtworks.go.config.CaseInsensitiveString.cis;
import static com.thoughtworks.go.config.RunOnAllAgentsJobTypeConfig.MARKER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class StageConfigTest {

    @Test
    public void shouldSetPrimitiveAttributes() {
        StageConfig config = new StageConfig();
        config.setConfigAttributes(Map.of(StageConfig.NAME, "foo_bar"));
        config.setConfigAttributes(Map.of(StageConfig.FETCH_MATERIALS, "0"));
        config.setConfigAttributes(Map.of(StageConfig.CLEAN_WORKING_DIR, "1"));
        assertThat(config.name()).isEqualTo(cis("foo_bar"));
        assertThat(config.isFetchMaterials()).isFalse();
        assertThat(config.isCleanWorkingDir()).isTrue();
    }

    @Test
    public void shouldSetArtifactCleanupOptOutAttribute() {
        StageConfig config = new StageConfig();
        assertThat(config.isArtifactCleanupProhibited()).isFalse();
        config.setConfigAttributes(Map.of(StageConfig.ARTIFACT_CLEANUP_PROHIBITED, "1"));
        assertThat(config.isArtifactCleanupProhibited()).isTrue();
        config.setConfigAttributes(new HashMap<>());
        assertThat(config.isArtifactCleanupProhibited()).isTrue();
        config.setConfigAttributes(Map.of(StageConfig.ARTIFACT_CLEANUP_PROHIBITED, "0"));
        assertThat(config.isArtifactCleanupProhibited()).isFalse();
    }

    @Test
    public void shouldRemoveStageLevelAuthorizationWhenInheritingPermissionsFromGroup() {
        StageConfig config = new StageConfig();
        StageConfigMother.addApprovalWithRoles(config, "role1");
        StageConfigMother.addApprovalWithUsers(config, "user1");

        Map<String, Object> map = new HashMap<>();
        List<Map<String, String>> operateUsers = new ArrayList<>();
        operateUsers.add(nameMap("user1"));
        map.put(StageConfig.OPERATE_USERS, operateUsers);

        List<Map<String, String>> operateRoles = new ArrayList<>();
        operateRoles.add(nameMap("role1"));
        map.put(StageConfig.OPERATE_ROLES, operateRoles);

        map.put(StageConfig.SECURITY_MODE, "inherit");

        config.setConfigAttributes(map);

        assertThat(config.getApproval().getAuthConfig().isEmpty()).isTrue();
    }

    @Test
    public void shouldSetOperateUsers() {
        StageConfig config = new StageConfig();
        Map<String, Object> map = new HashMap<>();
        List<Map<String, String>> operateUsers = new ArrayList<>();
        operateUsers.add(nameMap("user1"));
        operateUsers.add(nameMap("user1"));
        operateUsers.add(nameMap("user2"));
        map.put(StageConfig.OPERATE_USERS, operateUsers);
        map.put(StageConfig.OPERATE_ROLES, new ArrayList<>());
        map.put(StageConfig.SECURITY_MODE, "define");

        config.setConfigAttributes(map);

        assertThat(config.getOperateUsers()).hasSize(2);
        assertThat(config.getOperateUsers()).contains(new AdminUser(cis("user1")));
        assertThat(config.getOperateUsers()).contains(new AdminUser(cis("user2")));
    }

    @Test
    public void shouldSetOperateRoles() {
        StageConfig config = new StageConfig();
        Map<String, Object> map = new HashMap<>();
        List<Map<String, String>> operateRoles = new ArrayList<>();
        operateRoles.add(nameMap("role1"));
        operateRoles.add(nameMap("role1"));
        operateRoles.add(nameMap("role2"));
        map.put(StageConfig.OPERATE_ROLES, operateRoles);
        map.put(StageConfig.OPERATE_USERS, new ArrayList<>());
        map.put(StageConfig.SECURITY_MODE, "define");

        config.setConfigAttributes(map);

        assertThat(config.getOperateRoles().size()).isEqualTo(2);
        assertThat(config.getOperateRoles()).contains(new AdminRole(cis("role1")));
        assertThat(config.getOperateRoles()).contains(new AdminRole(cis("role2")));
    }

    private Map<String, String> nameMap(final String name) {
        return Map.of("name", name);
    }

    @Test
    public void shouldPopulateEnvironmentVariablesFromAttributeMap() {
        StageConfig stageConfig = new StageConfig();
        Map<String, Object> map = new HashMap<>();
        Map<String, String> valueMap = new HashMap<>();
        valueMap.put("name", "FOO");
        valueMap.put("value", "BAR");
        map.put(StageConfig.ENVIRONMENT_VARIABLES, valueMap);
        EnvironmentVariablesConfig mockEnvironmentVariablesConfig = mock(EnvironmentVariablesConfig.class);
        stageConfig.setVariables(mockEnvironmentVariablesConfig);

        stageConfig.setConfigAttributes(map);

        verify(mockEnvironmentVariablesConfig).setConfigAttributes(valueMap);
    }

    @Test
    public void shouldSetApprovalFromConfigAttrs() {
        StageConfig config = new StageConfig();
        config.setConfigAttributes(Map.of(StageConfig.APPROVAL, Map.of(TYPE, TYPE_MANUAL)));
        assertThat(config.getApproval().getType()).isEqualTo(TYPE_MANUAL);
        config.setConfigAttributes(new HashMap<>());
        assertThat(config.getApproval().getType()).isEqualTo(TYPE_MANUAL);

        config.setConfigAttributes(Map.of(StageConfig.APPROVAL, Map.of(TYPE, TYPE_SUCCESS)));
        assertThat(config.getApproval().getType()).isEqualTo(TYPE_SUCCESS);
        config.setConfigAttributes(new HashMap<>());
        assertThat(config.getApproval().getType()).isEqualTo(TYPE_SUCCESS);
    }

    @Test
    public void shouldPickupJobConfigDetailsFromAttributeMap() {
        StageConfig config = new StageConfig();
        Map<String, List<Map<String, String>>> stageAttrs = Map.of(StageConfig.JOBS, List.of(Map.of(JobConfig.NAME, "con-job"), Map.of(JobConfig.NAME, "boring-job")));
        config.setConfigAttributes(stageAttrs);
        assertThat(config.getJobs().getFirst().name()).isEqualTo(cis("con-job"));
        assertThat(config.getJobs().getLast().name()).isEqualTo(cis("boring-job"));
    }

    @Test
    public void shouldFindCorrectJobIfJobIsOnAllAgents() {
        JobConfig allAgentsJob = new JobConfig("job-for-all-agents");
        allAgentsJob.setRunOnAllAgents(true);

        JobConfigs jobs = new JobConfigs();
        jobs.add(allAgentsJob);
        jobs.add(new JobConfig("job"));
        StageConfig stage = new StageConfig(cis("stage-name"), jobs);

        JobConfig found = stage.jobConfigByInstanceName("job-for-all-agents-" + MARKER + "-1", true);
        assertThat(found).isEqualTo(allAgentsJob);
    }

    @Test
    public void shouldFindCorrectJobIfJobIsOnAllAgentsAndAmbiguousName() {
        JobConfig allAgentsJob = new JobConfig("job-for-all-agents");
        JobConfig ambiguousJob = new JobConfig("job-for-all");

        allAgentsJob.setRunOnAllAgents(true);
        ambiguousJob.setRunOnAllAgents(true);

        JobConfigs jobs = new JobConfigs();
        jobs.add(ambiguousJob);
        jobs.add(allAgentsJob);
        StageConfig stage = new StageConfig(cis("stage-name"), jobs);

        JobConfig found = stage.jobConfigByInstanceName("job-for-all-agents-" + MARKER + "-1", true);
        assertThat(found).isEqualTo(allAgentsJob);
    }

    @Test
    public void shouldPopulateErrorMessagesWhenHasJobNamesRepeated() {
        CruiseConfig config = new BasicCruiseConfig();
        config.initializeServer();
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage-1", "con-job");
        config.addPipeline("group-foo", pipelineConfig);
        StageConfig stageConfig = pipelineConfig.getFirst();

        JobConfig newJob = new JobConfig("foo!");
        StageConfig newlyAddedStage = new StageConfig(cis("."), new JobConfigs(newJob));
        pipelineConfig.addStageWithoutValidityAssertion(newlyAddedStage);

        stageConfig.getJobs().addJobWithoutValidityAssertion(new JobConfig(cis("con-job"), new ResourceConfigs(), new ArtifactTypeConfigs(), new Tasks(new ExecTask("ls", "-la", "foo"))));

        List<ConfigErrors> allErrors = config.validateAfterPreprocess();
        assertThat(allErrors).hasSize(4);
        assertThat(allErrors.get(0).firstErrorOn(JobConfig.NAME)).isEqualTo("You have defined multiple jobs called 'con-job'. Job names are case-insensitive and must be unique.");
        assertThat(allErrors.get(1).firstErrorOn(JobConfig.NAME)).isEqualTo("You have defined multiple jobs called 'con-job'. Job names are case-insensitive and must be unique.");
        assertThat(allErrors.get(2).firstErrorOn(StageConfig.NAME)).isEqualTo("Invalid stage name '.'. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
        assertThat(allErrors.get(3).firstErrorOn(JobConfig.NAME)).isEqualTo("Invalid job name 'foo!'. This must be alphanumeric and may contain underscores and periods. The maximum allowed length is 255 characters.");
        assertThat(stageConfig.getJobs().getFirst().errors().firstErrorOn(JobConfig.NAME)).isEqualTo("You have defined multiple jobs called 'con-job'. Job names are case-insensitive and must be unique.");
        assertThat(stageConfig.getJobs().getLast().errors().firstErrorOn(JobConfig.NAME)).isEqualTo("You have defined multiple jobs called 'con-job'. Job names are case-insensitive and must be unique.");
        assertThat(newlyAddedStage.errors().firstErrorOn(StageConfig.NAME)).isEqualTo("Invalid stage name '.'. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
        assertThat(newJob.errors().firstErrorOn(JobConfig.NAME)).isEqualTo("Invalid job name 'foo!'. This must be alphanumeric and may contain underscores and periods. The maximum allowed length is 255 characters.");
    }

    @Test
    public void shouldReturnAllTheUsersAndRoleThatCanOperateThisStage() {
        StageConfig stage = StageConfigMother.stageConfig("stage");
        StageConfigMother.addApprovalWithUsers(stage, "user1", "user2");
        StageConfigMother.addApprovalWithRoles(stage, "role1", "role2");

        assertThat(stage.getOperateUsers()).isEqualTo(List.of(new AdminUser(cis("user1")), new AdminUser(cis("user2"))));
        assertThat(stage.getOperateRoles()).isEqualTo(List.of(new AdminRole(cis("role1")), new AdminRole(cis("role2"))));
    }

    @Test
    public void shouldFailValidationWhenNameIsBlank() {
        StageConfig stageConfig = new StageConfig();
        stageConfig.validate(null);
        assertThat(stageConfig.errors().firstErrorOn(StageConfig.NAME)).contains("Invalid stage name 'null'");
        stageConfig.setName(null);
        stageConfig.validate(null);
        assertThat(stageConfig.errors().firstErrorOn(StageConfig.NAME)).contains("Invalid stage name 'null'");
        stageConfig.setName(cis(""));
        stageConfig.validate(null);
        assertThat(stageConfig.errors().firstErrorOn(StageConfig.NAME)).contains("Invalid stage name 'null'");
    }

    @Test
    public void shouldValidateTree() {
        EnvironmentVariablesConfig variables = mock(EnvironmentVariablesConfig.class);
        JobConfigs jobConfigs = mock(JobConfigs.class);
        Approval approval = mock(Approval.class);
        StageConfig stageConfig = new StageConfig(cis("stage$"), jobConfigs, approval);
        stageConfig.setVariables(variables);

        stageConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new PipelineConfig(), stageConfig));

        assertThat(stageConfig.errors().firstErrorOn(StageConfig.NAME)).contains("Invalid stage name 'stage$'");
        ArgumentCaptor<PipelineConfigSaveValidationContext> captor = ArgumentCaptor.forClass(PipelineConfigSaveValidationContext.class);
        verify(jobConfigs).validateTree(captor.capture());
        PipelineConfigSaveValidationContext childContext = captor.getValue();
        assertThat(childContext.getParent()).isEqualTo(stageConfig);
        verify(approval).validateTree(childContext);
        verify(variables).validateTree(childContext);
    }

    @Test
    public void shouldAddValidateTreeErrorsOnStageConfigIfPipelineIsAssociatedToATemplate() {
        Approval approval = mock(Approval.class);
        JobConfigs jobConfigs = mock(JobConfigs.class);
        ConfigErrors jobErrors = new ConfigErrors();
        jobErrors.add("KEY", "ERROR");
        when(jobConfigs.errors()).thenReturn(jobErrors);
        StageConfig stageConfig = new StageConfig(cis("stage$"), jobConfigs, approval);

        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.setTemplateName(cis("template"));
        stageConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", pipelineConfig, stageConfig));

        assertThat(stageConfig.errors().firstErrorOn(StageConfig.NAME)).contains("Invalid stage name 'stage$'");
    }

    @Test
    public void shouldReturnTrueIfAllDescendentsAreValid() {
        EnvironmentVariablesConfig variables = mock(EnvironmentVariablesConfig.class);
        JobConfigs jobConfigs = mock(JobConfigs.class);
        Approval approval = mock(Approval.class);
        when(variables.validateTree(any())).thenReturn(true);
        when(jobConfigs.validateTree(any())).thenReturn(true);
        when(approval.validateTree(any())).thenReturn(true);

        StageConfig stageConfig = new StageConfig(cis("p1"), jobConfigs);
        stageConfig.setVariables(variables);
        stageConfig.setApproval(approval);

        boolean isValid = stageConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new PipelineConfig(), stageConfig));
        assertThat(isValid).isTrue();

        verify(jobConfigs).validateTree(any());
        verify(variables).validateTree(any());
        verify(approval).validateTree(any());
    }

    @Test
    public void shouldReturnFalseIfAnyDescendentIsInValid() {
        EnvironmentVariablesConfig variables = mock(EnvironmentVariablesConfig.class);
        JobConfigs jobConfigs = mock(JobConfigs.class);
        Approval approval = mock(Approval.class);
        when(variables.validateTree(any())).thenReturn(false);
        when(jobConfigs.validateTree(any())).thenReturn(false);
        when(approval.validateTree(any())).thenReturn(false);

        StageConfig stageConfig = new StageConfig(cis("p1"), jobConfigs);
        stageConfig.setVariables(variables);
        stageConfig.setApproval(approval);

        boolean isValid = stageConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new PipelineConfig(), stageConfig));
        assertThat(isValid).isFalse();

        verify(jobConfigs).validateTree(any());
        verify(variables).validateTree(any());
        verify(approval).validateTree(any());

    }
}
