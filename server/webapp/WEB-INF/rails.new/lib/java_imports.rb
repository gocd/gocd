##########################GO-LICENSE-START################################
# Copyright 2017 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################GO-LICENSE-END##################################

module JavaImports
  java_import com.rits.cloning.Cloner unless defined? Cloner
  java_import com.thoughtworks.go.config.ExecTask unless defined? ExecTask
  java_import com.thoughtworks.go.config.AntTask unless defined? AntTask
  java_import com.thoughtworks.go.config.NantTask unless defined? NantTask
  java_import com.thoughtworks.go.config.RakeTask unless defined? RakeTask
  java_import com.thoughtworks.go.config.FetchTask unless defined? FetchTask
  java_import com.thoughtworks.go.config.TimerConfig unless defined? TimerConfig
  java_import com.thoughtworks.go.config.MagicalGoConfigXmlLoader unless defined? MagicalGoConfigXmlLoader
  java_import com.thoughtworks.go.config.MailHost unless defined? MailHost
  java_import com.thoughtworks.go.config.AgentConfig unless defined? AgentConfig
  java_import com.thoughtworks.go.config.Approval unless defined? Approval
  java_import com.thoughtworks.go.config.ArtifactPlans unless defined? ArtifactPlans
  java_import com.thoughtworks.go.config.ArtifactPlan unless defined? ArtifactPlan
  java_import com.thoughtworks.go.config.preprocessor.ParamSubstitutionHandler unless defined? ParamSubstitutionHandler
  java_import com.thoughtworks.go.domain.ArtifactType unless defined? ArtifactType
  java_import com.thoughtworks.go.domain.buildcause.BuildCause unless defined? BuildCause
  java_import com.thoughtworks.go.domain.ConfigErrors unless defined? ConfigErrors
  java_import com.thoughtworks.go.config.CruiseConfig unless defined? CruiseConfig
  java_import com.thoughtworks.go.config.BasicCruiseConfig unless defined? BasicCruiseConfig
  java_import com.thoughtworks.go.config.EnvironmentConfig unless defined? EnvironmentConfig
  java_import com.thoughtworks.go.config.BasicEnvironmentConfig unless defined? BasicEnvironmentConfig
  java_import com.thoughtworks.go.config.merge.MergeEnvironmentConfig unless defined? MergeEnvironmentConfig
  java_import com.thoughtworks.go.config.EnvironmentVariablesConfig unless defined? EnvironmentVariablesConfig
  java_import com.thoughtworks.go.config.ExecTask unless defined? ExecTask
  java_import com.thoughtworks.go.domain.feed.FeedEntries unless defined? FeedEntries
  java_import com.thoughtworks.go.domain.feed.stage.StageFeedEntry unless defined? StageFeedEntry
  java_import com.thoughtworks.go.config.JobConfigs unless defined? JobConfigs
  java_import com.thoughtworks.go.config.JobConfig unless defined? JobConfig
  java_import com.thoughtworks.go.domain.JobIdentifier unless defined? JobIdentifier
  java_import com.thoughtworks.go.domain.JobInstance unless defined? JobInstance
  java_import com.thoughtworks.go.domain.JobInstances unless defined? JobInstances
  java_import com.thoughtworks.go.domain.JobResult unless defined? JobResult
  java_import com.thoughtworks.go.domain.JobState unless defined? JobState

  java_import com.thoughtworks.go.domain.MaterialRevisions unless defined? MaterialRevisions
  java_import com.thoughtworks.go.domain.MaterialRevision unless defined? MaterialRevision
  java_import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision unless defined? DependencyMaterialRevision
  java_import com.thoughtworks.go.config.materials.dependency.DependencyMaterial unless defined? DependencyMaterial
  java_import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig unless defined? DependencyMaterialConfig
  java_import com.thoughtworks.go.domain.materials.MatchedRevision unless defined? MatchedRevision
  java_import com.thoughtworks.go.domain.materials.Material unless defined? Material
  java_import com.thoughtworks.go.config.materials.Materials unless defined? Materials
  java_import com.thoughtworks.go.config.materials.Filter unless defined? Filter
  java_import com.thoughtworks.go.config.materials.IgnoredFiles unless defined? IgnoredFiles
  java_import com.thoughtworks.go.domain.materials.MaterialConfig unless defined? MaterialConfig
  java_import com.thoughtworks.go.config.materials.MaterialConfigs unless defined? MaterialConfigs
  java_import com.thoughtworks.go.domain.materials.Modification unless defined? Modification
  java_import com.thoughtworks.go.domain.materials.ModifiedAction unless defined? ModifiedAction
  java_import com.thoughtworks.go.domain.materials.ModifiedFile unless defined? ModifiedFile
  java_import com.thoughtworks.go.config.materials.ScmMaterial unless defined? ScmMaterial
  java_import com.thoughtworks.go.config.materials.ScmMaterialConfig unless defined? ScmMaterialConfig
  java_import com.thoughtworks.go.config.materials.PackageMaterial unless defined? PackageMaterial
  java_import com.thoughtworks.go.config.materials.PackageMaterialConfig unless defined? PackageMaterialConfig
  java_import com.thoughtworks.go.config.materials.PluggableSCMMaterial unless defined? PluggableSCMMaterial
  java_import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig unless defined? PluggableSCMMaterialConfig
  java_import com.thoughtworks.go.config.materials.svn.SvnMaterial unless defined? SvnMaterial
  java_import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig unless defined? SvnMaterialConfig
  java_import com.thoughtworks.go.config.materials.mercurial.HgMaterial unless defined? HgMaterial
  java_import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig unless defined? HgMaterialConfig
  java_import com.thoughtworks.go.config.materials.perforce.P4Material unless defined? P4Material
  java_import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig unless defined? P4MaterialConfig
  java_import com.thoughtworks.go.config.materials.git.GitMaterial unless defined? GitMaterial
  java_import com.thoughtworks.go.config.materials.git.GitMaterialConfig unless defined? GitMaterialConfig
  java_import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig unless defined? TfsMaterialConfig
  java_import com.thoughtworks.go.config.MingleConfig unless defined? MingleConfig
  java_import com.thoughtworks.go.config.ParamConfig unless defined? ParamConfig
  java_import com.thoughtworks.go.config.ParamsConfig unless defined? ParamsConfig
  java_import com.thoughtworks.go.config.PipelineConfigs unless defined? PipelineConfigs
  java_import com.thoughtworks.go.config.BasicPipelineConfigs unless defined? BasicPipelineConfigs
  java_import com.thoughtworks.go.config.PipelineConfig unless defined? PipelineConfig
  java_import com.thoughtworks.go.domain.NotificationFilter unless defined? NotificationFilter
  java_import com.thoughtworks.go.domain.exception.UncheckedValidationException unless defined? UncheckedValidationException
  java_import com.thoughtworks.go.domain.PipelineGroups unless defined? PipelineGroups
  java_import com.thoughtworks.go.domain.PipelineDependencyGraphOld unless defined? PipelineDependencyGraphOld
  java_import com.thoughtworks.go.domain.valuestreammap.ValueStreamMap unless defined? ValueStreamMap
  java_import com.thoughtworks.go.domain.PipelineIdentifier unless defined? PipelineIdentifier
  java_import com.thoughtworks.go.domain.PipelinePauseInfo unless defined? PipelinePauseInfo
  java_import com.thoughtworks.go.domain.PipelineTimelineEntry unless defined? PipelineTimelineEntry
  java_import com.thoughtworks.go.config.Resources unless defined? Resources
  java_import com.thoughtworks.go.config.Resource unless defined? Resource
  java_import com.thoughtworks.go.config.SecurityConfig unless defined? SecurityConfig
  java_import com.thoughtworks.go.config.ServerConfig unless defined? ServerConfig
  java_import com.thoughtworks.go.config.StageConfig unless defined? StageConfig
  java_import com.thoughtworks.go.domain.StageIdentifier unless defined? StageIdentifier
  java_import com.thoughtworks.go.domain.StageIdentifier unless defined? StageIdentifier
  java_import com.thoughtworks.go.domain.StageEvent unless defined? StageEvent
  java_import com.thoughtworks.go.domain.StageResult unless defined? StageResult
  java_import com.thoughtworks.go.domain.StageState unless defined? StageState
  java_import com.thoughtworks.go.domain.Stages unless defined? Stages
  java_import com.thoughtworks.go.domain.Stage unless defined? Stage
  java_import com.thoughtworks.go.config.Tasks unless defined? Tasks
  java_import com.thoughtworks.go.domain.Task unless defined? Task
  java_import com.thoughtworks.go.config.TestArtifactPlan unless defined? TestArtifactPlan
  java_import com.thoughtworks.go.domain.testinfo.FailureDetails unless defined? FailureDetails
  java_import com.thoughtworks.go.domain.testinfo.StageTestRuns unless defined? StageTestRuns
  java_import com.thoughtworks.go.domain.testinfo.TestStatus unless defined? TestStatus
  java_import com.thoughtworks.go.config.TrackingTool unless defined? TrackingTool
  java_import com.thoughtworks.go.domain.User unless defined? User
  java_import com.thoughtworks.go.i18n.Localizer unless defined? Localizer
  java_import com.thoughtworks.go.i18n.LocalizedMessage unless defined? LocalizedMessage
  java_import com.thoughtworks.go.presentation.GoPluginDescriptorModel unless defined? GoPluginDescriptorModel
  java_import com.thoughtworks.go.presentation.environment.EnvironmentPipelineModel unless defined? EnvironmentPipelineModel
  java_import com.thoughtworks.go.presentation.FlashMessageModel unless defined? FlashMessageModel
  java_import com.thoughtworks.go.presentation.ConfigForEdit unless defined? ConfigForEdit
  java_import com.thoughtworks.go.presentation.pipelinehistory.Environment unless defined? Environment
  java_import com.thoughtworks.go.presentation.pipelinehistory.JobHistory unless defined? JobHistory
  java_import com.thoughtworks.go.presentation.pipelinehistory.NullStageHistoryItem unless defined? NullStageHistoryItem
  java_import com.thoughtworks.go.presentation.pipelinehistory.PipelineGroupModel unless defined? PipelineGroupModel
  java_import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels unless defined? PipelineInstanceModels
  java_import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel unless defined? PipelineInstanceModel
  java_import com.thoughtworks.go.presentation.pipelinehistory.PipelineModel unless defined? PipelineModel
  java_import com.thoughtworks.go.presentation.pipelinehistory.StageHistoryPage unless defined? StageHistoryPage
  java_import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels unless defined? StageInstanceModels
  java_import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel unless defined? StageInstanceModel
  java_import com.thoughtworks.go.presentation.TriStateSelection unless defined? TriStateSelection
  java_import com.thoughtworks.go.presentation.UserModel unless defined? UserModel
  java_import com.thoughtworks.go.presentation.UserSearchModel unless defined? UserSearchModel
  java_import com.thoughtworks.go.presentation.UserSourceType unless defined? UserSourceType
  java_import com.thoughtworks.go.remote.AgentIdentifier unless defined? AgentIdentifier
  java_import com.thoughtworks.go.server.domain.JobDurationStrategy unless defined? JobDurationStrategy
  java_import com.thoughtworks.go.server.domain.Username unless defined? Username
  java_import com.thoughtworks.go.server.domain.user.PipelineSelections unless defined? PipelineSelections
  java_import com.thoughtworks.go.serverhealth.HealthStateScope unless defined? HealthStateScope
  java_import com.thoughtworks.go.serverhealth.HealthStateType unless defined? HealthStateType
  java_import com.thoughtworks.go.serverhealth.ServerHealthService unless defined? ServerHealthService
  java_import com.thoughtworks.go.serverhealth.ServerHealthStates unless defined? ServerHealthStates
  java_import com.thoughtworks.go.serverhealth.ServerHealthState unless defined? ServerHealthState
  java_import com.thoughtworks.go.server.scheduling.ScheduleOptions unless defined? ScheduleOptions
  java_import com.thoughtworks.go.server.service.AgentRuntimeInfo unless defined? AgentRuntimeInfo
  java_import com.thoughtworks.go.server.service.AgentBuildingInfo unless defined? AgentBuildingInfo
  java_import com.thoughtworks.go.server.service.ChangesetService unless defined? ChangesetService
  java_import com.thoughtworks.go.server.service.PipelineStagesFeedService unless defined? PipelineStagesFeedService
  java_import com.thoughtworks.go.server.service.result.DefaultLocalizedResult unless defined? DefaultLocalizedResult
  java_import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult unless defined? HttpLocalizedOperationResult
  java_import com.thoughtworks.go.server.service.result.HttpOperationResult unless defined? HttpOperationResult
  java_import com.thoughtworks.go.server.service.result.SubsectionLocalizedOperationResult unless defined? SubsectionLocalizedOperationResult
  java_import com.thoughtworks.go.server.service.UserService unless defined? UserService
  java_import com.thoughtworks.go.server.ui.AgentsViewModel unless defined? AgentsViewModel
  java_import com.thoughtworks.go.server.ui.AgentViewModel unless defined? AgentViewModel
  java_import com.thoughtworks.go.server.ui.JobInstanceModel unless defined? JobInstanceModel
  java_import com.thoughtworks.go.server.ui.StageSummaryModel unless defined? StageSummaryModel
  java_import com.thoughtworks.go.server.ui.StageSummaryModels unless defined? StageSummaryModels
  java_import com.thoughtworks.go.server.ui.ViewCacheKey unless defined? ViewCacheKey
  java_import com.thoughtworks.go.server.util.Pagination unless defined? Pagination
  java_import com.thoughtworks.go.server.util.UserHelper unless defined? UserHelper
  java_import com.thoughtworks.go.util.GoConstants unless defined? GoConstants
  java_import com.thoughtworks.go.util.SystemEnvironment unless defined? SystemEnvironment
  java_import com.thoughtworks.go.util.SystemUtil unless defined? SystemUtil
  java_import com.thoughtworks.go.util.TimeConverter unless defined? TimeConverter
  java_import com.thoughtworks.go.util.TriState unless defined? TriState
  #java_import com.thoughtworks.go.domain.config.Admin unless defined? com.thoughtworks.go.domain.config.Admin
  java_import java.lang.System unless defined? System
  java_import java.util.HashMap unless defined? HashMap
  java_import org.joda.time.Duration unless defined? Duration
  java_import com.thoughtworks.go.listener.BaseUrlChangeListener unless defined? BaseUrlChangeListener
  java_import com.thoughtworks.go.config.Authorization unless defined? Authorization
  java_import com.thoughtworks.go.i18n.LocalizedMessage unless defined? LocalizedMessage
  java_import com.thoughtworks.go.config.PipelineTemplateConfig unless defined? PipelineTemplateConfig
  java_import com.thoughtworks.go.server.domain.xml.StageXmlViewModel unless defined? StageXmlViewModel
  java_import com.thoughtworks.go.server.domain.xml.PipelineXmlViewModel unless defined? PipelineXmlViewModel
  java_import com.thoughtworks.go.server.domain.xml.JobXmlViewModel unless defined? JobXmlViewModel
  java_import com.thoughtworks.go.server.domain.xml.JobPlanXmlViewModel unless defined? JobPlanXmlViewModel
  java_import org.dom4j.io.OutputFormat unless defined? OutputFormat
  java_import org.dom4j.io.XMLWriter unless defined? XMLWriter
  java_import java.io.ByteArrayOutputStream unless defined? ByteArrayOutputStream
  java_import com.thoughtworks.go.domain.XmlWriterContext unless defined? XmlWriterContext
  java_import com.thoughtworks.go.domain.StageFinder unless defined? StageFinder
  java_import com.thoughtworks.go.domain.Properties unless defined? Properties
  java_import com.thoughtworks.go.domain.Property unless defined? Property
  java_import com.thoughtworks.go.domain.DefaultJobPlan unless defined? DefaultJobPlan
  java_import com.thoughtworks.go.server.ui.JobInstancesModel unless defined? JobInstancesModel
  java_import com.thoughtworks.go.config.CaseInsensitiveString unless defined? CaseInsensitiveString
  java_import com.thoughtworks.go.server.ui.SortOrder unless defined? SortOrder
  java_import com.thoughtworks.go.domain.AgentStatus unless defined? AgentStatus
  java_import com.thoughtworks.go.domain.GoConfigRevision unless defined? GoConfigRevision
  java_import com.thoughtworks.go.util.TimeProvider unless defined? TimeProvider
  java_import com.thoughtworks.go.util.DateUtils unless defined? DateUtils
  java_import com.thoughtworks.go.domain.AgentRuntimeStatus unless defined? AgentRuntimeStatus
  java_import com.thoughtworks.go.security.GoCipher unless defined? GoCipher
  java_import com.thoughtworks.go.util.command.UrlArgument unless defined? UrlArgument
  java_import com.thoughtworks.go.presentation.TaskViewModel unless defined? TaskViewModel
  java_import com.thoughtworks.go.presentation.PipelineTemplateConfigViewModel unless defined? PipelineTemplateConfigViewModel
  java_import com.thoughtworks.go.server.service.dd.reporting.ReportingFanInGraph unless defined? ReportingFanInGraph
  java_import com.thoughtworks.go.server.service.lookups.CommandSnippet unless defined? CommandSnippet
  java_import com.thoughtworks.go.util.ProcessManager unless defined? ProcessManager
  java_import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor unless defined? GoPluginDescriptor
  java_import com.thoughtworks.go.domain.valuestreammap.DependencyNodeType unless defined? DependencyNodeType
  java_import com.thoughtworks.go.server.service.responses.GoConfigOperationalResponse unless defined? GoConfigOperationalResponse
  java_import com.thoughtworks.go.domain.packagerepository.PackageRepository unless defined? PackageRepository
  java_import com.thoughtworks.go.domain.packagerepository.PackageDefinition unless defined? PackageDefinition
  java_import com.thoughtworks.go.domain.packagerepository.PackageRepositories unless defined? PackageRepositories
  java_import com.thoughtworks.go.domain.scm.SCM unless defined? SCM
  java_import com.thoughtworks.go.domain.config.Configuration unless defined? Configuration
  java_import com.thoughtworks.go.domain.config.ConfigurationProperty unless defined? ConfigurationProperty
  java_import com.thoughtworks.go.domain.packagerepository.Packages unless defined? Packages
  java_import com.thoughtworks.go.domain.config.PluginConfiguration unless defined? PluginConfiguration
  java_import com.thoughtworks.go.config.update.ConfigUpdateAjaxResponse unless defined? ConfigUpdateAjaxResponse
  java_import com.thoughtworks.go.plugin.access.packagematerial.PackageMetadataStore unless defined? PackageMetadataStore
  java_import com.thoughtworks.go.plugin.access.packagematerial.PackageConfigurations unless defined? PackageConfigurations
  java_import com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration unless defined? PackageConfiguration
  java_import com.thoughtworks.go.plugin.access.scm.SCMMetadataStore unless defined? SCMMetadataStore
  java_import com.thoughtworks.go.plugin.access.scm.SCMConfigurations unless defined? SCMConfigurations
  java_import com.thoughtworks.go.plugin.access.scm.SCMConfiguration unless defined? SCMConfiguration
  java_import com.thoughtworks.go.server.service.PluginService unless defined? PluginService
  java_import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsMetadataStore unless defined? PluginSettingsMetadataStore
  java_import com.thoughtworks.go.server.domain.PluginSettings unless defined? PluginSettings
  java_import com.thoughtworks.go.domain.config.ConfigurationKey unless defined? ConfigurationKey
  java_import com.thoughtworks.go.domain.config.ConfigurationValue unless defined? ConfigurationValue
  java_import com.thoughtworks.go.domain.config.EncryptedConfigurationValue unless defined? EncryptedConfigurationValue
  java_import com.thoughtworks.go.plugin.api.config.Option unless defined? Option
  java_import com.thoughtworks.go.plugin.access.packagematerial.RepositoryMetadataStore unless defined? RepositoryMetadataStore
  java_import com.thoughtworks.go.server.service.materials.commands.PackageMaterialAddWithNewPackageDefinitionCommand unless defined? PackageMaterialAddWithNewPackageDefinitionCommand
  java_import com.thoughtworks.go.server.service.materials.commands.PackageMaterialAddWithExistingPackageDefinitionCommand unless defined? PackageMaterialAddWithExistingPackageDefinitionCommand
  java_import com.thoughtworks.go.server.service.materials.commands.PackageMaterialUpdateWithNewPackageDefinitionCommand unless defined? PackageMaterialUpdateWithNewPackageDefinitionCommand
  java_import com.thoughtworks.go.server.service.materials.commands.PackageMaterialUpdateWithExistingPackageDefinitionCommand unless defined? PackageMaterialUpdateWithExistingPackageDefinitionCommand
  java_import com.thoughtworks.go.server.service.materials.commands.PackageDefinitionCreator unless defined? PackageDefinitionCreator
  java_import com.thoughtworks.go.util.Pair unless defined? Pair
  java_import com.thoughtworks.go.presentation.MissingPluggableTaskViewModel unless defined? MissingPluggableTaskViewModel
  java_import com.thoughtworks.go.config.pluggabletask.PluggableTask unless defined? PluggableTask
  java_import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore unless defined? PluggableTaskConfigStore
  java_import com.thoughtworks.go.server.service.support.toggle.Toggles unless defined? Toggles
  java_import com.thoughtworks.go.config.AuthConfig unless defined? AuthConfig
  java_import com.thoughtworks.go.config.AdminsConfig unless defined? AdminsConfig
  java_import com.thoughtworks.go.config.RunIfConfig unless defined? RunIfConfig
  java_import com.thoughtworks.go.domain.RunIfConfigs unless defined? RunIfConfigs
  java_import com.thoughtworks.go.plugin.api.task.TaskConfigProperty unless defined? TaskConfigProperty
  java_import com.thoughtworks.go.config.PipelineConfigSaveValidationContext unless defined? PipelineConfigSaveValidationContext
  java_import com.thoughtworks.go.domain.GoVersion unless defined? GoVersion
  java_import com.thoughtworks.go.domain.VersionInfo unless defined? VersionInfo
  java_import com.thoughtworks.go.server.ui.plugins.PluginInfo unless defined? PluginInfo
  java_import com.thoughtworks.go.server.ui.plugins.PluggableInstanceSettings unless defined? PluggableInstanceSettings
  java_import com.thoughtworks.go.server.service.plugins.InvalidPluginTypeException unless defined? InvalidPluginTypeException
  java_import com.thoughtworks.go.server.service.EntityHashingService unless defined? EntityHashingService
  java_import com.thoughtworks.go.config.EnvironmentAgentConfig unless defined? EnvironmentAgentConfig
  java_import com.thoughtworks.go.config.EnvironmentVariableConfig unless defined? EnvironmentVariableConfig
  java_import com.thoughtworks.go.config.elastic.ElasticProfile unless defined? ElasticProfile
  java_import com.thoughtworks.go.server.ui.TemplatesViewModel unless defined? TemplatesViewModel
  java_import com.thoughtworks.go.config.remote.RepoConfigOrigin unless defined? RepoConfigOrigin
  java_import com.thoughtworks.go.config.remote.FileConfigOrigin unless defined? FileConfigOrigin
  java_import com.thoughtworks.go.config.remote.ConfigRepoConfig unless defined? ConfigRepoConfig
  java_import com.thoughtworks.go.config.PluginRoleConfig unless defined? PluginRoleConfig
  java_import com.thoughtworks.go.config.RoleConfig unless defined? RoleConfig
  java_import com.thoughtworks.go.config.RoleUser unless defined? RoleUser
  java_import com.thoughtworks.go.config.SecurityAuthConfig unless defined? SecurityAuthConfig
  java_import com.thoughtworks.go.plugin.access.authorization.models.Capabilities unless defined? Capabilities
  java_import com.thoughtworks.go.plugin.access.authorization.models.SupportedAuthType unless defined? SupportedAuthType
  java_import com.thoughtworks.go.plugin.domain.common.VerifyConnectionResponse unless defined? VerifyConnectionResponse
  java_import com.thoughtworks.go.plugin.domain.common.ValidationResult unless defined? ValidationResult
  java_import com.thoughtworks.go.plugin.domain.common.ValidationError unless defined? ValidationError
  java_import com.thoughtworks.go.config.AdminUser unless defined? AdminUser
  java_import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension unless defined? ElasticAgentExtension
  java_import com.thoughtworks.go.plugin.access.elastic.ElasticAgentMetadataStore unless defined? ElasticAgentMetadataStore
  java_import com.thoughtworks.go.server.service.result.BulkDeletionFailureResult unless defined? BulkDeletionFailureResult
end
