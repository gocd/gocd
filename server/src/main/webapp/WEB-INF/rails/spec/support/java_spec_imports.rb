#
# Copyright 2021 ThoughtWorks, Inc.
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
#

module JavaSpecImports
  java_import java.util.HashSet unless defined? HashSet
  java_import java.util.ArrayList unless defined? ArrayList
  java_import java.util.Arrays unless defined? Arrays
  java_import java.util.HashMap unless defined? HashMap
  java_import java.util.LinkedHashMap unless defined? LinkedHashMap
  java_import com.thoughtworks.go.helper.ConfigFileFixture unless defined? ConfigFileFixture
  java_import com.thoughtworks.go.config.ConfigMigrator unless defined? ConfigMigrator
  java_import com.thoughtworks.go.helper.AgentInstanceMother unless defined? AgentInstanceMother
  java_import com.thoughtworks.go.server.ui.helper.AgentsViewModelMother unless defined? AgentsViewModelMother
  java_import com.thoughtworks.go.server.ui.AgentsViewModel unless defined? AgentsViewModel
  java_import com.thoughtworks.go.server.ui.AgentViewModel unless defined? AgentViewModel
  java_import com.thoughtworks.go.domain.DiskSpace unless defined? DiskSpace
  java_import com.thoughtworks.go.domain.AgentConfigStatus unless defined? AgentConfigStatus
  java_import com.thoughtworks.go.helper.GoConfigMother unless defined? GoConfigMother
  java_import com.thoughtworks.go.helper.JobIdentifierMother unless defined? JobIdentifierMother
  java_import com.thoughtworks.go.helper.JobInstanceMother unless defined? JobInstanceMother
  java_import com.thoughtworks.go.helper.JobInstanceMother unless defined? JobInstanceMother
  java_import com.thoughtworks.go.helper.MaterialsMother unless defined? MaterialsMother
  java_import com.thoughtworks.go.helper.MaterialConfigsMother unless defined? MaterialConfigsMother
  java_import com.thoughtworks.go.helper.ModificationsMother unless defined? ModificationsMother
  java_import com.thoughtworks.go.helper.PipelineConfigMother unless defined? PipelineConfigMother
  java_import com.thoughtworks.go.helper.PipelineTemplateConfigMother unless defined? PipelineTemplateConfigMother
  java_import com.thoughtworks.go.helper.PipelineHistoryMother unless defined? PipelineHistoryMother
  java_import com.thoughtworks.go.helper.PipelineMother unless defined? PipelineMother
  java_import com.thoughtworks.go.helper.StageConfigMother unless defined? StageConfigMother
  java_import com.thoughtworks.go.helper.BuildPlanMother unless defined? BuildPlanMother
  java_import com.thoughtworks.go.helper.StageMother unless defined? StageMother
  java_import com.thoughtworks.go.util.LogFixture unless defined? LogFixture
  java_import com.thoughtworks.go.util.GoConfigFileHelper unless defined? GoConfigFileHelper
  java_import com.thoughtworks.go.util.ReflectionUtil unless defined? ReflectionUtil
  java_import com.thoughtworks.go.server.service.UserService unless defined? UserService
  java_import com.thoughtworks.go.server.service.AdminService unless defined? AdminService
  java_import com.thoughtworks.go.presentation.TriStateSelection unless defined? TriStateSelection
  java_import com.thoughtworks.go.server.domain.Username unless defined? Username
  java_import com.thoughtworks.go.config.materials.AbstractMaterial unless defined? AbstractMaterial
  java_import com.thoughtworks.go.config.materials.AbstractMaterialConfig unless defined? AbstractMaterialConfig
  java_import com.thoughtworks.go.config.EnvironmentVariableConfig unless defined? EnvironmentVariableConfig
  java_import com.thoughtworks.go.config.EnvironmentAgentConfig unless defined? EnvironmentAgentConfig
  java_import com.thoughtworks.go.config.EnvironmentAgentsConfig unless defined? EnvironmentAgentsConfig
  java_import com.thoughtworks.go.config.EncryptedVariableValueConfig unless defined? EncryptedVariableValueConfig
  java_import com.thoughtworks.go.config.VariableValueConfig unless defined? VariableValueConfig
  java_import com.thoughtworks.go.presentation.pipelinehistory.StageHistoryEntry unless defined? StageHistoryEntry
  java_import com.thoughtworks.go.config.ExecTask unless defined? ExecTask
  java_import com.thoughtworks.go.config.AntTask unless defined? AntTask
  java_import com.thoughtworks.go.config.RakeTask unless defined? RakeTask
  java_import com.thoughtworks.go.domain.Task unless defined? Task
  java_import com.thoughtworks.go.config.Argument unless defined? Argument
  java_import com.thoughtworks.go.domain.config.Arguments unless defined? Arguments
  java_import com.thoughtworks.go.config.RunIfConfig unless defined? RunIfConfig
  java_import com.thoughtworks.go.config.AdminUser unless defined? AdminUser
  java_import com.thoughtworks.go.config.AdminRole unless defined? AdminRole
  java_import com.thoughtworks.go.config.Authorization unless defined? Authorization
  java_import com.thoughtworks.go.config.ArtifactStore unless defined? ArtifactStore
  java_import com.thoughtworks.go.config.PipelineTemplateConfig unless defined? PipelineTemplateConfig
  java_import com.thoughtworks.go.config.TemplatesConfig unless defined? TemplatesConfig
  java_import com.thoughtworks.go.server.presentation.CanDeleteResult unless defined? CanDeleteResult
  java_import com.thoughtworks.go.security.GoCipher unless defined? GoCipher
  java_import com.thoughtworks.go.config.update.ConfigUpdateResponse unless defined? ConfigUpdateResponse
  java_import com.thoughtworks.go.config.materials.perforce.P4MaterialView unless defined? P4MaterialView
  java_import com.thoughtworks.go.config.materials.perforce.P4MaterialViewConfig unless defined? P4MaterialViewConfig
  java_import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig unless defined? TfsMaterialConfig
  java_import com.thoughtworks.go.server.domain.xml.JobPlanXmlViewModel unless defined? JobPlanXmlViewModel
  java_import com.thoughtworks.go.domain.WaitingJobPlan unless defined? WaitingJobPlan
  java_import com.thoughtworks.go.domain.feed.Author unless defined? Author
  java_import com.thoughtworks.go.domain.NullAgentInstance unless defined? NullAgentInstance
  java_import com.thoughtworks.go.domain.buildcause.BuildCause unless defined? BuildCause
  java_import com.thoughtworks.go.server.web.PipelineRevisionRange unless defined? PipelineRevisionRange
  java_import com.thoughtworks.go.domain.valuestreammap.DependencyNodeType unless defined? DependencyNodeType
  java_import com.thoughtworks.go.server.valuestreammap.DummyNodeCreation unless defined? DummyNodeCreation
  java_import com.thoughtworks.go.domain.valuestreammap.PipelineRevision unless defined? PipelineRevision
  java_import com.thoughtworks.go.domain.valuestreammap.SCMRevision unless defined? SCMRevision
  java_import com.thoughtworks.go.config.validation.GoConfigValidity unless defined? GoConfigValidity
  java_import com.thoughtworks.go.domain.valuestreammap.UnrunPipelineRevision unless defined? UnrunPipelineRevision
  java_import com.thoughtworks.go.domain.valuestreammap.PipelineDependencyNode unless defined? PipelineDependencyNode
  java_import com.thoughtworks.go.domain.valuestreammap.SCMDependencyNode unless defined? SCMDependencyNode
  java_import com.thoughtworks.go.domain.packagerepository.PackageRepositoryMother unless defined? PackageRepositoryMother
  java_import com.thoughtworks.go.domain.packagerepository.PackageDefinitionMother unless defined? PackageDefinitionMother
  java_import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother unless defined? ConfigurationPropertyMother
  java_import com.thoughtworks.go.domain.scm.SCMMother unless defined? SCMMother
  java_import com.thoughtworks.go.helper.EnvironmentConfigMother unless defined? EnvironmentConfigMother
  java_import com.thoughtworks.go.domain.config.RepositoryMetadataStoreHelper unless defined? RepositoryMetadataStoreHelper
  java_import com.thoughtworks.go.config.pluggabletask.PluggableTask unless defined? PluggableTask
  java_import com.thoughtworks.go.plugin.access.pluggabletask.TaskPreference unless defined? TaskPreference
  java_import com.thoughtworks.go.presentation.PluggableTaskViewModel unless defined? PluggableTaskViewModel
  java_import com.thoughtworks.go.plugin.infra.PluginManager unless defined? PluginManager
  java_import com.thoughtworks.go.plugin.api.task.TaskView unless defined? TaskView
  java_import com.thoughtworks.go.plugins.presentation.PluggableTaskViewModelFactory unless defined? PluggableTaskViewModelFactory
  java_import com.thoughtworks.go.plugin.api.task.TaskView unless defined? TaskView
  java_import com.thoughtworks.go.domain.TaskViewStub unless defined? TaskViewStub
  java_import com.thoughtworks.go.server.domain.helper.FeatureToggleMother unless defined? FeatureToggleMother
  java_import com.thoughtworks.go.helper.EnvironmentVariablesConfigMother unless defined? EnvironmentVariablesConfigMother
  java_import com.thoughtworks.go.config.validation.FilePathTypeValidator unless defined? FilePathTypeValidator
  java_import com.thoughtworks.go.domain.GoVersion unless defined? GoVersion
  java_import com.thoughtworks.go.domain.VersionInfo unless defined? VersionInfo
  java_import com.thoughtworks.go.server.service.CheckConnectionSubprocessExecutionContext unless defined? CheckConnectionSubprocessExecutionContext
  java_import com.thoughtworks.go.config.remote.RepoConfigOrigin unless defined? RepoConfigOrigin
  java_import com.thoughtworks.go.config.remote.ConfigRepoConfig unless defined? ConfigRepoConfig
  java_import com.thoughtworks.go.config.remote.FileConfigOrigin unless defined? FileConfigOrigin
  java_import com.thoughtworks.go.config.TemplateToPipelines unless defined? TemplateToPipelines
  java_import com.thoughtworks.go.config.PipelineEditabilityInfo unless defined? PipelineEditabilityInfo
  java_import com.thoughtworks.go.config.CaseInsensitiveString unless defined? CaseInsensitiveString
  java_import com.thoughtworks.go.server.domain.PluginSettings unless defined? PluginSettings
  java_import com.thoughtworks.go.server.service.SecurityService unless defined? SecurityService
  java_import com.thoughtworks.go.server.service.GoConfigService unless defined? GoConfigService
  java_import com.thoughtworks.go.server.service.ServerConfigService unless defined? ServerConfigService
  java_import com.thoughtworks.go.domain.materials.ValidationBean unless defined? ValidationBean
  java_import com.thoughtworks.go.config.preprocessor.ConfigParamPreprocessor unless defined? ConfigParamPreprocessor
  java_import com.thoughtworks.go.config.PipelineConfig unless defined? PipelineConfig
  java_import com.thoughtworks.go.server.service.MaterialConfigConverter unless defined? MaterialConfigConverter
  java_import com.thoughtworks.go.config.security.Permissions unless defined? Permissions
  java_import com.thoughtworks.go.config.security.users.Everyone unless defined? Everyone
  java_import com.thoughtworks.go.config.security.users.NoOne unless defined? NoOne
  java_import com.thoughtworks.go.server.dashboard.GoDashboardPipeline unless defined? GoDashboardPipeline
  java_import com.thoughtworks.go.server.dashboard.TimeStampBasedCounter unless defined? TimeStampBasedCounter
  java_import com.thoughtworks.go.util.SystemTimeClock unless defined? SystemTimeClock
  java_import com.thoughtworks.go.config.FetchTaskAdapter unless defined? FetchTaskAdapter
  java_import com.thoughtworks.go.server.cache.GoCache unless defined? GoCache
  java_import com.thoughtworks.go.server.ui.EnvironmentViewModel unless defined? EnvironmentViewModel
end
