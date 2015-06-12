package com.thoughtworks.go.config.merge;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMs;
import com.thoughtworks.go.feature.EnterpriseFeature;
import com.thoughtworks.go.licensing.Edition;
import com.thoughtworks.go.licensing.LicenseValidity;
import com.thoughtworks.go.util.Node;
import com.thoughtworks.go.util.Pair;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Composite of main CruiseConfig and parts.
 */
public class MergeCruiseConfig implements CruiseConfig {

    private BasicCruiseConfig main;

    private List<PartialConfig> parts;


    @Override
    public void initializeServer() {

    }

    @Override
    public String getMd5() {
        return null;
    }

    @Override
    public int schemaVersion() {
        return 0;
    }

    @Override
    public void validate(ValidationContext validationContext) {

    }

    @Override
    public Hashtable<CaseInsensitiveString, Node> getDependencyTable() {
        return null;
    }

    @Override
    public ConfigErrors errors() {
        return null;
    }

    @Override
    public void addError(String fieldName, String message) {

    }

    @Override
    public StageConfig stageConfigByName(CaseInsensitiveString pipelineName, CaseInsensitiveString stageName) {
        return null;
    }

    @Override
    public JobConfig findJob(String pipelineName, String stageName, String jobName) {
        return null;
    }

    @Override
    public PipelineConfig pipelineConfigByName(CaseInsensitiveString name) {
        return null;
    }

    @Override
    public boolean hasStageConfigNamed(CaseInsensitiveString pipelineName, CaseInsensitiveString stageName, boolean ignoreCase) {
        return false;
    }

    @Override
    public PipelineConfig getPipelineConfigByName(CaseInsensitiveString pipelineName) {
        return null;
    }

    @Override
    public boolean hasPipelineNamed(CaseInsensitiveString pipelineName) {
        return false;
    }

    @Override
    public boolean hasNextStage(CaseInsensitiveString pipelineName, CaseInsensitiveString lastStageName) {
        return false;
    }

    @Override
    public boolean hasPreviousStage(CaseInsensitiveString pipelineName, CaseInsensitiveString stageName) {
        return false;
    }

    @Override
    public StageConfig nextStage(CaseInsensitiveString pipelineName, CaseInsensitiveString lastStageName) {
        return null;
    }

    @Override
    public StageConfig previousStage(CaseInsensitiveString pipelineName, CaseInsensitiveString lastStageName) {
        return null;
    }

    @Override
    public JobConfig jobConfigByName(String pipelineName, String stageName, String jobInstanceName, boolean ignoreCase) {
        return null;
    }

    @Override
    public Agents agents() {
        return null;
    }

    @Override
    public ServerConfig server() {
        return null;
    }

    @Override
    public MailHost mailHost() {
        return null;
    }

    @Override
    public EnvironmentsConfig getEnvironments() {
        return null;
    }

    @Override
    public Map<String, List<Authorization.PrivilegeType>> groupsAffectedByDeletionOfRole(String roleName) {
        return null;
    }

    @Override
    public Set<Pair<PipelineConfig, StageConfig>> stagesWithPermissionForRole(String roleName) {
        return null;
    }

    @Override
    public void removeRole(Role roleToDelete) {

    }

    @Override
    public boolean doesAdminConfigContainRole(String roleToDelete) {
        return false;
    }

    @Override
    public List<PipelineConfig> allPipelines() {
        return null;
    }

    @Override
    public PipelineConfigs pipelines(String groupName) {
        return null;
    }

    @Override
    public boolean hasBuildPlan(CaseInsensitiveString pipelineName, CaseInsensitiveString stageName, String buildName, boolean ignoreCase) {
        return false;
    }

    @Override
    public boolean requiresApproval(CaseInsensitiveString pipelineName, CaseInsensitiveString stageName) {
        return false;
    }

    @Override
    public void accept(JobConfigVisitor visitor) {

    }

    @Override
    public void accept(TaskConfigVisitor visitor) {

    }

    @Override
    public void accept(PiplineConfigVisitor visitor) {

    }

    @Override
    public void setGroup(PipelineGroups pipelineGroups) {

    }

    @Override
    public PipelineGroups getGroups() {
        return null;
    }

    @Override
    public void addPipeline(String groupName, PipelineConfig pipelineConfig) {

    }

    @Override
    public void addPipelineWithoutValidation(String groupName, PipelineConfig pipelineConfig) {

    }

    @Override
    public void update(String groupName, String pipelineName, PipelineConfig pipeline) {

    }

    @Override
    public boolean exist(int pipelineIndex) {
        return false;
    }

    @Override
    public boolean hasPipeline() {
        return false;
    }

    @Override
    public PipelineConfig find(String groupName, int pipelineIndex) {
        return null;
    }

    @Override
    public int numberOfPipelines() {
        return 0;
    }

    @Override
    public int numbersOfPipeline(String groupName) {
        return 0;
    }

    @Override
    public void groups(List<String> allGroup) {

    }

    @Override
    public boolean exist(String groupName, String pipelineName) {
        return false;
    }

    @Override
    public List<Task> tasksForJob(String pipelineName, String stageName, String jobName) {
        return null;
    }

    @Override
    public boolean isSmtpEnabled() {
        return false;
    }

    @Override
    public boolean isInFirstGroup(CaseInsensitiveString pipelineName) {
        return false;
    }

    @Override
    public boolean hasMultiplePipelineGroups() {
        return false;
    }

    @Override
    public void accept(PipelineGroupVisitor visitor) {

    }

    @Override
    public boolean isSecurityEnabled() {
        return false;
    }

    @Override
    public void setServerConfig(ServerConfig serverConfig) {

    }

    @Override
    public String adminEmail() {
        return null;
    }

    @Override
    public boolean hasPipelineGroup(String groupName) {
        return false;
    }

    @Override
    public PipelineConfigs findGroup(String groupName) {
        return null;
    }

    @Override
    public void updateGroup(PipelineConfigs pipelineConfigs, String groupName) {

    }

    @Override
    public boolean isMailHostConfigured() {
        return false;
    }

    @Override
    public List<PipelineConfig> getAllPipelineConfigs() {
        return null;
    }

    @Override
    public List<CaseInsensitiveString> getAllPipelineNames() {
        return null;
    }

    @Override
    public boolean isAdministrator(String username) {
        return false;
    }

    @Override
    public boolean doesRoleHaveAdminPrivileges(String rolename) {
        return false;
    }

    @Override
    public void setEnvironments(EnvironmentsConfig environments) {

    }

    @Override
    public Set<MaterialConfig> getAllUniqueMaterialsBelongingToAutoPipelines() {
        return null;
    }

    @Override
    public Set<MaterialConfig> getAllUniqueMaterials() {
        return null;
    }

    @Override
    public Set<StageConfig> getStagesUsedAsMaterials(PipelineConfig pipelineConfig) {
        return null;
    }

    @Override
    public boolean isLicenseValid() {
        return false;
    }

    @Override
    public FeatureSupported validateFeature(EnterpriseFeature enterpriseFeature) {
        return null;
    }

    @Override
    public Edition validEdition() {
        return null;
    }

    @Override
    public LicenseValidity licenseValidity() {
        return null;
    }

    @Override
    public EnvironmentConfig addEnvironment(String environmentName) {
        return null;
    }

    @Override
    public void addEnvironment(BasicEnvironmentConfig config) {

    }

    @Override
    public Boolean isPipelineLocked(String pipelineName) {
        return null;
    }

    @Override
    public Set<Resource> getAllResources() {
        return null;
    }

    @Override
    public TemplatesConfig getTemplates() {
        return null;
    }

    @Override
    public PipelineTemplateConfig findTemplate(CaseInsensitiveString templateName) {
        return null;
    }

    @Override
    public void addTemplate(PipelineTemplateConfig pipelineTemplate) {

    }

    @Override
    public PipelineTemplateConfig getTemplateByName(CaseInsensitiveString pipeline) {
        return null;
    }

    @Override
    public void setTemplates(TemplatesConfig templates) {

    }

    @Override
    public void makePipelineUseTemplate(CaseInsensitiveString pipelineName, CaseInsensitiveString templateName) {

    }

    @Override
    public Iterable<PipelineConfig> getDownstreamPipelines(String pipelineName) {
        return null;
    }

    @Override
    public boolean hasVariableInScope(String pipelineName, String variableName) {
        return false;
    }

    @Override
    public EnvironmentVariablesConfig variablesFor(String pipelineName) {
        return null;
    }

    @Override
    public boolean isGroupAdministrator(CaseInsensitiveString userName) {
        return false;
    }

    @Override
    public List<ConfigErrors> getAllErrors() {
        return null;
    }

    @Override
    public List<ConfigErrors> getAllErrorsExceptFor(Validatable skipValidatable) {
        return null;
    }

    @Override
    public List<ConfigErrors> validateAfterPreprocess() {
        return null;
    }

    @Override
    public void copyErrorsTo(CruiseConfig to) {

    }

    @Override
    public Edition edition() {
        return null;
    }

    @Override
    public PipelineConfigs findGroupOfPipeline(PipelineConfig pipelineConfig) {
        return null;
    }

    @Override
    public PipelineConfig findPipelineUsingThisPipelineAsADependency(String pipelineName) {
        return null;
    }

    @Override
    public Map<String, List<PipelineConfig>> generatePipelineVsDownstreamMap() {
        return null;
    }

    @Override
    public List<PipelineConfig> pipelinesForFetchArtifacts(String pipelineName) {
        return null;
    }

    @Override
    public Map<CaseInsensitiveString, List<CaseInsensitiveString>> templatesWithPipelinesForUser(String username) {
        return null;
    }

    @Override
    public boolean isArtifactCleanupProhibited(String pipelineName, String stageName) {
        return false;
    }

    @Override
    public MaterialConfig materialConfigFor(String fingerprint) {
        return null;
    }

    @Override
    public String sanitizedGroupName(String name) {
        return null;
    }

    @Override
    public void removePackageRepository(String id) {

    }

    @Override
    public PackageRepositories getPackageRepositories() {
        return null;
    }

    @Override
    public void savePackageRepository(PackageRepository packageRepository) {

    }

    @Override
    public void savePackageDefinition(PackageDefinition packageDefinition) {

    }

    @Override
    public void setPackageRepositories(PackageRepositories packageRepositories) {

    }

    @Override
    public SCMs getSCMs() {
        return null;
    }

    @Override
    public void setSCMs(SCMs scms) {

    }

    @Override
    public boolean canDeletePackageRepository(PackageRepository repository) {
        return false;
    }

    @Override
    public boolean canDeletePluggableSCMMaterial(SCM scmConfig) {
        return false;
    }

    @Override
    public ConfigOrigin getOrigin() {
        return null;
    }
}
