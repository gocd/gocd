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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.elastic.ElasticConfig;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Understands the configuration for cruise
 */
public interface CruiseConfig extends Validatable, ConfigOriginTraceable {
    String WORKING_BASE_DIR = "pipelines/";

    void merge(List<PartialConfig> partList, boolean forEdit);

    @PostConstruct
    void initializeServer();

    // TODO these should be removed, they suggest knowledge of config store
    String getMd5();

    @TestOnly
    int schemaVersion();

    Set<MaterialConfig> getAllUniquePostCommitSchedulableMaterials();

    ConfigReposConfig getConfigRepos();

    @TestOnly
    void setConfigRepos(ConfigReposConfig repos);

    @Override
    void validate(ValidationContext validationContext);

    Map<CaseInsensitiveString, Node> getDependencyTable();

    @Override
    ConfigErrors errors();

    @Override
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

    ServerConfig server();

    MailHost mailHost();

    EnvironmentsConfig getEnvironments();

    List<PipelineConfig> allPipelines();

    @NotNull PipelineConfigs pipelines(String groupName);

    boolean hasBuildPlan(CaseInsensitiveString pipelineName, CaseInsensitiveString stageName, String buildName, boolean ignoreCase);

    boolean requiresApproval(CaseInsensitiveString pipelineName, CaseInsensitiveString stageName);

    void accept(JobConfigVisitor visitor);

    void accept(PipelineConfigVisitor visitor);

    @TestOnly
    void setGroup(PipelineGroups pipelineGroups);

    PipelineGroups getGroups();

    @TestOnly
    List<String> getGroupsForUser(CaseInsensitiveString username, List<Role> roles);

    @TestOnly
    void addPipeline(String groupName, PipelineConfig pipelineConfig);

    void addPipelineWithoutValidation(String groupName, PipelineConfig pipelineConfig);

    void update(String groupName, String pipelineName, PipelineConfig pipeline);

    void deletePipeline(PipelineConfig pipelineConfig);

    boolean exist(String groupName, String pipelineName);

    List<Task> tasksForJob(String pipelineName, String stageName, String jobName);

    boolean isSmtpEnabled();

    void accept(PipelineGroupVisitor visitor);

    boolean isSecurityEnabled();

    @TestOnly
    void setServerConfig(ServerConfig serverConfig);

    String adminEmail();

    boolean hasPipelineGroup(String groupName);

    @NotNull PipelineConfigs findGroup(String groupName);

    boolean isMailHostConfigured();

    List<PipelineConfig> getAllPipelineConfigs();

    Map<CaseInsensitiveString, PipelineConfig> pipelineConfigsAsMap();

    List<CaseInsensitiveString> getAllPipelineNames();

    boolean isAdministrator(String username);

    @TestOnly
    Set<MaterialConfig> getAllUniqueMaterialsBelongingToAutoPipelines();

    Set<MaterialConfig> getAllUniqueMaterialsBelongingToAutoPipelinesAndConfigRepos();

    Set<MaterialConfig> getAllUniqueMaterials();

    Set<MaterialConfig> getAllUniqueMaterialsOfPipelinesAndConfigRepos();

    @TestOnly
    EnvironmentConfig addEnvironment(String environmentName);

    void addEnvironment(EnvironmentConfig config);

    boolean isPipelineLockable(String pipelineName);

    boolean isPipelineUnlockableWhenFinished(String pipelineName);

    Set<ResourceConfig> getAllResources();

    TemplatesConfig getTemplates();

    @Nullable PipelineTemplateConfig findTemplate(CaseInsensitiveString templateName);

    void addTemplate(PipelineTemplateConfig pipelineTemplate);

    PipelineTemplateConfig getTemplateByName(CaseInsensitiveString pipeline);

    void setTemplates(TemplatesConfig templates);

    boolean hasVariableInScope(String pipelineName, String variableName);

    EnvironmentVariablesConfig variablesFor(String pipelineName);

    boolean isGroupAdministrator(CaseInsensitiveString userName);

    List<ConfigErrors> getAllErrors();

    List<ConfigErrors> validateAfterPreprocess();

    PipelineConfigs findGroupOfPipeline(PipelineConfig pipelineConfig);

    Map<CaseInsensitiveString, List<PipelineConfig>> generatePipelineVsDownstreamMap();

    List<CaseInsensitiveString> pipelinesAssociatedWithTemplate(CaseInsensitiveString templateName);

    List<PipelineConfig> pipelineConfigsAssociatedWithTemplate(CaseInsensitiveString templateName);

    MaterialConfig materialConfigFor(CaseInsensitiveString pipelineName, String fingerprint);

    String sanitizedGroupName(String name);

    PackageRepositories getPackageRepositories();

    void savePackageDefinition(PackageDefinition packageDefinition);

    void setPackageRepositories(PackageRepositories packageRepositories);

    SCMs getSCMs();

    void setSCMs(SCMs scms);

    boolean canDeletePackageRepository(PackageRepository repository);

    boolean canDeletePluggableSCMMaterial(SCM scmConfig);

    List<PipelineConfig> pipelinesAssociatedWithPluggableSCM(SCM scmConfig);

    List<PipelineConfig> pipelinesAssociatedWithPackage(PackageDefinition packageDefinition);

    List<PipelineConfig> pipelinesAssociatedWithPackageRepository(PackageRepository packageRepository);

    void setPartials(List<PartialConfig> partials);

    List<PartialConfig> getPartials();

    List<PipelineConfig> getAllLocalPipelineConfigs(boolean excludeMembersOfRemoteEnvironments);

    boolean isLocal();

    ElasticConfig getElasticConfig();

    @TestOnly
    void setElasticConfig(ElasticConfig elasticConfig);

    @TestOnly
    CruiseConfig cloneForValidation();

    boolean canViewAndEditTemplates(CaseInsensitiveString username);

    boolean isAuthorizedToEditTemplate(CaseInsensitiveString templateName, CaseInsensitiveString username);

    boolean isAuthorizedToEditTemplate(PipelineTemplateConfig templateConfig, CaseInsensitiveString username);

    boolean isAuthorizedToViewTemplate(CaseInsensitiveString templateName, CaseInsensitiveString username);

    boolean isAuthorizedToViewTemplate(PipelineTemplateConfig templateConfig, CaseInsensitiveString username);

    boolean isAuthorizedToViewTemplates(CaseInsensitiveString username);

    Map<CaseInsensitiveString, Map<CaseInsensitiveString, Authorization>> templatesWithAssociatedPipelines();

    ArtifactStores getArtifactStores();

    @TestOnly
    void setArtifactStores(ArtifactStores artifactStores);

    void encryptSecureProperties(CruiseConfig preprocessed);

    void deletePipelineGroup(String groupName);

    SecretConfigs getSecretConfigs();
}
