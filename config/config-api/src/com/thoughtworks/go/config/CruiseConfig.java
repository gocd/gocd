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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.ConfigOriginTraceable;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMs;
import com.thoughtworks.go.util.Node;
import com.thoughtworks.go.util.Pair;

import javax.annotation.PostConstruct;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @understands the configuration for cruise
 */
public interface CruiseConfig extends Validatable, ConfigOriginTraceable {
    String WORKING_BASE_DIR = "pipelines/";

    void merge(List<PartialConfig> partList, boolean forEdit);

    @PostConstruct
    void initializeServer();

    // TODO these should be removed, they suggest knowledge of config store
    String getMd5();

    int schemaVersion();

    Set<MaterialConfig> getAllUniquePostCommitSchedulableMaterials();

    ConfigReposConfig getConfigRepos();

    void setConfigRepos(ConfigReposConfig repos);

    void validate(ValidationContext validationContext);

    Hashtable<CaseInsensitiveString, Node> getDependencyTable();

    ConfigErrors errors();

    void addError(String fieldName, String message);

    StageConfig stageConfigByName(CaseInsensitiveString pipelineName, CaseInsensitiveString stageName);

    JobConfig findJob(String pipelineName, String stageName, String jobName);

    PipelineConfig pipelineConfigByName(CaseInsensitiveString name);

    boolean hasStageConfigNamed(CaseInsensitiveString pipelineName, CaseInsensitiveString stageName, boolean ignoreCase);

    PipelineConfig getPipelineConfigByName(CaseInsensitiveString pipelineName);

    boolean hasPipelineNamed(CaseInsensitiveString pipelineName);

    boolean hasNextStage(CaseInsensitiveString pipelineName, CaseInsensitiveString lastStageName);

    boolean hasPreviousStage(CaseInsensitiveString pipelineName, CaseInsensitiveString stageName);

    StageConfig nextStage(CaseInsensitiveString pipelineName, CaseInsensitiveString lastStageName);

    StageConfig previousStage(CaseInsensitiveString pipelineName, CaseInsensitiveString lastStageName);

    JobConfig jobConfigByName(String pipelineName, String stageName, String jobInstanceName, boolean ignoreCase);

    Agents agents();

    ServerConfig server();

    MailHost mailHost();

    EnvironmentsConfig getEnvironments();

    Map<String, List<Authorization.PrivilegeType>> groupsAffectedByDeletionOfRole(String roleName);

    Set<Pair<PipelineConfig, StageConfig>> stagesWithPermissionForRole(String roleName);

    void removeRole(Role roleToDelete);

    boolean doesAdminConfigContainRole(String roleToDelete);

    List<PipelineConfig> allPipelines();

    PipelineConfigs pipelines(String groupName);

    boolean hasBuildPlan(CaseInsensitiveString pipelineName, CaseInsensitiveString stageName, String buildName, boolean ignoreCase);

    boolean requiresApproval(CaseInsensitiveString pipelineName, CaseInsensitiveString stageName);

    void accept(JobConfigVisitor visitor);

    void accept(TaskConfigVisitor visitor);

    void accept(PiplineConfigVisitor visitor);

    void setGroup(PipelineGroups pipelineGroups);

    PipelineGroups getGroups();

    void addPipeline(String groupName, PipelineConfig pipelineConfig);

    void addPipelineWithoutValidation(String groupName, PipelineConfig pipelineConfig);

    void update(String groupName, String pipelineName, PipelineConfig pipeline);

    void deletePipeline(PipelineConfig pipelineConfig);

    boolean exist(int pipelineIndex);

    boolean hasPipeline();

    PipelineConfig find(String groupName, int pipelineIndex);

    int numberOfPipelines();

    int numbersOfPipeline(String groupName);

    void groups(List<String> allGroup);

    boolean exist(String groupName, String pipelineName);

    List<Task> tasksForJob(String pipelineName, String stageName, String jobName);

    boolean isSmtpEnabled();

    boolean isInFirstGroup(CaseInsensitiveString pipelineName);

    boolean hasMultiplePipelineGroups();

    void accept(PipelineGroupVisitor visitor);

    boolean isSecurityEnabled();

    void setServerConfig(ServerConfig serverConfig);

    String adminEmail();

    boolean hasPipelineGroup(String groupName);

    PipelineConfigs findGroup(String groupName);

    void updateGroup(PipelineConfigs pipelineConfigs, String groupName);

    boolean isMailHostConfigured();

    List<PipelineConfig> getAllPipelineConfigs();

    List<CaseInsensitiveString> getAllPipelineNames();

    boolean isAdministrator(String username);

    boolean doesRoleHaveAdminPrivileges(String rolename);

    void setEnvironments(EnvironmentsConfig environments);

    Set<MaterialConfig> getAllUniqueMaterialsBelongingToAutoPipelines();

    Set<MaterialConfig> getAllUniqueMaterialsBelongingToAutoPipelinesAndConfigRepos();

    Set<MaterialConfig> getAllUniqueMaterials();

    Set<StageConfig> getStagesUsedAsMaterials(PipelineConfig pipelineConfig);

    EnvironmentConfig addEnvironment(String environmentName);

    void addEnvironment(BasicEnvironmentConfig config);

    Boolean isPipelineLocked(String pipelineName);

    Set<Resource> getAllResources();

    TemplatesConfig getTemplates();

    PipelineTemplateConfig findTemplate(CaseInsensitiveString templateName);

    void addTemplate(PipelineTemplateConfig pipelineTemplate);

    PipelineTemplateConfig getTemplateByName(CaseInsensitiveString pipeline);

    void setTemplates(TemplatesConfig templates);

    void makePipelineUseTemplate(CaseInsensitiveString pipelineName, CaseInsensitiveString templateName);

    Iterable<PipelineConfig> getDownstreamPipelines(String pipelineName);

    boolean hasVariableInScope(String pipelineName, String variableName);

    EnvironmentVariablesConfig variablesFor(String pipelineName);

    boolean isGroupAdministrator(CaseInsensitiveString userName);

    List<ConfigErrors> getAllErrors();

    List<ConfigErrors> getAllErrorsExceptFor(Validatable skipValidatable);

    List<ConfigErrors> validateAfterPreprocess();

    void copyErrorsTo(CruiseConfig to);

    PipelineConfigs findGroupOfPipeline(PipelineConfig pipelineConfig);

    PipelineConfig findPipelineUsingThisPipelineAsADependency(String pipelineName);

    Map<String, List<PipelineConfig>> generatePipelineVsDownstreamMap();

    List<PipelineConfig> pipelinesForFetchArtifacts(String pipelineName);

    Map<CaseInsensitiveString, List<CaseInsensitiveString>> templatesWithPipelinesForUser(String username);

    List<CaseInsensitiveString> pipelinesAssociatedWithTemplate(CaseInsensitiveString templateName);

    boolean isArtifactCleanupProhibited(String pipelineName, String stageName);

    MaterialConfig materialConfigFor(String fingerprint);

    MaterialConfig materialConfigFor(CaseInsensitiveString pipelineName, String fingerprint);

    String sanitizedGroupName(String name);

    void removePackageRepository(String id);

    PackageRepositories getPackageRepositories();

    void savePackageRepository(PackageRepository packageRepository);

    void savePackageDefinition(PackageDefinition packageDefinition);

    void setPackageRepositories(PackageRepositories packageRepositories);

    SCMs getSCMs();

    void setSCMs(SCMs scms);

    boolean canDeletePackageRepository(PackageRepository repository);

    boolean canDeletePluggableSCMMaterial(SCM scmConfig);

    List<PipelineConfig> getAllLocalPipelineConfigs();

    void setPartials(List<PartialConfig> partials);

    List<PartialConfig> getPartials();

    List<PipelineConfig> getAllLocalPipelineConfigs(boolean excludeMembersOfRemoteEnvironments);

    boolean isLocal();

    /**
     * Gets remote config parts currently active in this configuration.
     * Note: It does NOT guarantee that these partials are valid.
     */
    List<PartialConfig> getMergedPartials();

    CruiseConfig cloneForValidation();
}
