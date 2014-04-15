##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
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


module JavaSpecImports
  import java.util.HashSet unless defined? HashSet
  import java.util.ArrayList unless defined? ArrayList
  import java.util.Arrays unless defined? Arrays
  import java.util.HashMap unless defined? HashMap
  import com.thoughtworks.go.helper.ConfigFileFixture unless defined? ConfigFileFixture
  import com.thoughtworks.go.config.ConfigMigrator unless defined? ConfigMigrator
  import com.thoughtworks.go.helper.AgentInstanceMother unless defined? AgentInstanceMother
  import com.thoughtworks.go.server.ui.helper.AgentsViewModelMother unless defined? AgentsViewModelMother
  import com.thoughtworks.go.server.ui.AgentsViewModel unless defined? AgentsViewModel
  import com.thoughtworks.go.domain.DiskSpace unless defined? DiskSpace
  import com.thoughtworks.go.helper.GoConfigMother unless defined? GoConfigMother
  import com.thoughtworks.go.helper.JobIdentifierMother unless defined? JobIdentifierMother
  import com.thoughtworks.go.helper.JobInstanceMother  unless defined? JobInstanceMother
  import com.thoughtworks.go.helper.JobInstanceMother unless defined? JobInstanceMother
  import com.thoughtworks.go.helper.MaterialsMother unless defined? MaterialsMother
  import com.thoughtworks.go.helper.MaterialConfigsMother unless defined? MaterialConfigsMother
  import com.thoughtworks.go.helper.ModificationsMother unless defined? ModificationsMother
  import com.thoughtworks.go.helper.PipelineConfigMother unless defined? PipelineConfigMother
  import com.thoughtworks.go.helper.PipelineTemplateConfigMother unless defined? PipelineTemplateConfigMother
  import com.thoughtworks.go.helper.PipelineHistoryMother unless defined? PipelineHistoryMother
  import com.thoughtworks.go.helper.PipelineMother unless defined? PipelineMother
  import com.thoughtworks.go.helper.StageConfigMother unless defined? StageConfigMother
  import com.thoughtworks.go.helper.BuildPlanMother unless defined? BuildPlanMother
  import com.thoughtworks.go.helper.StageMother unless defined? StageMother
  import com.thoughtworks.go.util.LogFixture unless defined? LogFixture
  import com.thoughtworks.go.util.GoConfigFileHelper  unless defined? GoConfigFileHelper
  import com.thoughtworks.go.util.ReflectionUtil unless defined? ReflectionUtil
  import com.thoughtworks.go.server.service.UserService unless defined? UserService
  import com.thoughtworks.go.server.service.AdminService unless defined? AdminService
  import com.thoughtworks.go.presentation.TriStateSelection unless defined? TriStateSelection
  import com.thoughtworks.go.config.materials.Filter unless defined? Filter
  import com.thoughtworks.go.config.materials.IgnoredFiles unless defined? IgnoredFiles
  import com.thoughtworks.go.server.domain.Username unless defined? Username
  import com.thoughtworks.go.config.materials.AbstractMaterial unless defined? AbstractMaterial
  import com.thoughtworks.go.config.materials.AbstractMaterialConfig unless defined? AbstractMaterialConfig
  import com.thoughtworks.go.config.EnvironmentVariableConfig unless defined? EnvironmentVariableConfig
  import com.thoughtworks.go.config.EncryptedVariableValueConfig unless defined? EncryptedVariableValueConfig
  import com.thoughtworks.go.config.VariableValueConfig unless defined? VariableValueConfig
  import com.thoughtworks.go.presentation.pipelinehistory.StageHistoryEntry unless defined? StageHistoryEntry
  import com.thoughtworks.go.config.ExecTask unless defined? ExecTask
  import com.thoughtworks.go.config.AntTask unless defined? AntTask
  import com.thoughtworks.go.config.RakeTask unless defined? RakeTask
  import com.thoughtworks.go.domain.Task unless defined? Task
  import com.thoughtworks.go.config.Argument unless defined? Argument
  import com.thoughtworks.go.domain.config.Arguments unless defined? Arguments
  import com.thoughtworks.go.config.RunIfConfig unless defined? RunIfConfig
  import com.thoughtworks.go.config.AdminUser unless defined? AdminUser
  import com.thoughtworks.go.config.AdminRole unless defined? AdminRole
  import com.thoughtworks.go.config.Authorization unless defined? Authorization
  import com.thoughtworks.go.config.PipelineTemplateConfig unless defined? PipelineTemplateConfig
  import com.thoughtworks.go.config.TemplatesConfig unless defined? TemplatesConfig
  import com.thoughtworks.go.server.presentation.CanDeleteResult unless defined? CanDeleteResult
  import com.thoughtworks.go.i18n.LocalizedMessage unless defined? LocalizedMessage
  import com.thoughtworks.go.security.GoCipher unless defined? GoCipher
  import com.thoughtworks.go.config.update.ConfigUpdateResponse unless defined? ConfigUpdateResponse
  import com.thoughtworks.go.config.materials.perforce.P4MaterialView unless defined? P4MaterialView
  import com.thoughtworks.go.config.materials.perforce.P4MaterialViewConfig unless defined? P4MaterialViewConfig
  import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig unless defined? TfsMaterialConfig
  import com.thoughtworks.go.server.domain.xml.JobPlanXmlViewModel unless defined? JobPlanXmlViewModel
  import com.thoughtworks.go.domain.WaitingJobPlan unless defined? WaitingJobPlan
  import com.thoughtworks.go.domain.feed.Author unless defined? Author
  import com.thoughtworks.go.server.ui.MingleCard unless defined? MingleCard
  import com.thoughtworks.go.domain.buildcause.BuildCause unless defined? BuildCause
  import com.thoughtworks.go.server.web.PipelineRevisionRange unless defined? PipelineRevisionRange
  import com.thoughtworks.go.helper.CommandSnippetMother unless defined? CommandSnippetMother
  import com.thoughtworks.go.domain.valuestreammap.DependencyNodeType unless defined? DependencyNodeType
  import com.thoughtworks.go.server.valuestreammap.DummyNodeCreation unless defined? DummyNodeCreation
  import com.thoughtworks.go.domain.valuestreammap.PipelineRevision unless defined? PipelineRevision
  import com.thoughtworks.go.domain.valuestreammap.SCMRevision unless defined? SCMRevision
  import com.thoughtworks.go.config.validation.GoConfigValidity unless defined? GoConfigValidity
  import com.thoughtworks.go.domain.valuestreammap.UnrunPipelineRevision unless defined? UnrunPipelineRevision
  import com.thoughtworks.go.domain.valuestreammap.PipelineDependencyNode unless defined? PipelineDependencyNode
  import com.thoughtworks.go.domain.valuestreammap.SCMDependencyNode unless defined? SCMDependencyNode
  import com.thoughtworks.go.domain.packagerepository.PackageRepositoryMother unless defined? PackageRepositoryMother
  import com.thoughtworks.go.domain.packagerepository.PackageDefinitionMother unless defined? PackageDefinitionMother
  import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother unless defined? ConfigurationPropertyMother
  import com.thoughtworks.go.domain.config.RepositoryMetadataStoreHelper unless defined? RepositoryMetadataStoreHelper
  import com.thoughtworks.go.config.pluggabletask.PluggableTask unless defined? PluggableTask
  import com.thoughtworks.go.presentation.PluggableTaskViewModel unless defined? PluggableTaskViewModel
  import com.thoughtworks.go.plugin.infra.PluginManager unless defined? PluginManager
  import com.thoughtworks.go.plugin.api.task.TaskView unless defined? TaskView
  import com.thoughtworks.go.plugins.presentation.PluggableTaskViewModelFactory unless defined? PluggableTaskViewModelFactory
  import com.thoughtworks.go.plugin.api.task.TaskView unless defined? TaskView
  import com.thoughtworks.go.domain.TaskViewStub unless defined? TaskViewStub
end