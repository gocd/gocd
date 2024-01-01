#
# Copyright 2024 Thoughtworks, Inc.
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
  java_import java.util.ArrayList unless defined? ArrayList
  java_import com.thoughtworks.go.helper.GoConfigMother unless defined? GoConfigMother
  java_import com.thoughtworks.go.helper.JobInstanceMother unless defined? JobInstanceMother
  java_import com.thoughtworks.go.helper.MaterialsMother unless defined? MaterialsMother
  java_import com.thoughtworks.go.helper.ModificationsMother unless defined? ModificationsMother
  java_import com.thoughtworks.go.helper.PipelineHistoryMother unless defined? PipelineHistoryMother
  java_import com.thoughtworks.go.helper.StageMother unless defined? StageMother
  java_import com.thoughtworks.go.util.LogFixture unless defined? LogFixture
  java_import com.thoughtworks.go.presentation.pipelinehistory.StageHistoryEntry unless defined? StageHistoryEntry
  java_import com.thoughtworks.go.domain.valuestreammap.PipelineRevision unless defined? PipelineRevision
  java_import com.thoughtworks.go.domain.valuestreammap.SCMRevision unless defined? SCMRevision
  java_import com.thoughtworks.go.config.validation.GoConfigValidity unless defined? GoConfigValidity
  java_import com.thoughtworks.go.domain.valuestreammap.UnrunPipelineRevision unless defined? UnrunPipelineRevision
  java_import com.thoughtworks.go.domain.valuestreammap.PipelineDependencyNode unless defined? PipelineDependencyNode
  java_import com.thoughtworks.go.domain.valuestreammap.SCMDependencyNode unless defined? SCMDependencyNode
  java_import com.thoughtworks.go.server.cache.GoCache unless defined? GoCache
  java_import com.thoughtworks.go.domain.buildcause.BuildCause unless defined? BuildCause
  java_import com.thoughtworks.go.domain.ConfigErrors unless defined? ConfigErrors
  java_import com.thoughtworks.go.config.BasicCruiseConfig unless defined? BasicCruiseConfig
  java_import com.thoughtworks.go.domain.JobInstances unless defined? JobInstances

  java_import com.thoughtworks.go.domain.MaterialRevisions unless defined? MaterialRevisions
  java_import com.thoughtworks.go.domain.MaterialRevision unless defined? MaterialRevision
  java_import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision unless defined? DependencyMaterialRevision
  java_import com.thoughtworks.go.domain.materials.Modification unless defined? Modification
  java_import com.thoughtworks.go.domain.materials.ModifiedAction unless defined? ModifiedAction
  java_import com.thoughtworks.go.domain.materials.ModifiedFile unless defined? ModifiedFile

  java_import com.thoughtworks.go.config.materials.svn.SvnMaterial unless defined? SvnMaterial
  java_import com.thoughtworks.go.config.materials.git.GitMaterial unless defined? GitMaterial

  java_import com.thoughtworks.go.config.PipelineConfigs unless defined? PipelineConfigs

  java_import com.thoughtworks.go.domain.valuestreammap.ValueStreamMap unless defined? ValueStreamMap
  java_import com.thoughtworks.go.domain.PipelineIdentifier unless defined? PipelineIdentifier
  java_import com.thoughtworks.go.domain.PipelinePauseInfo unless defined? PipelinePauseInfo

  java_import com.thoughtworks.go.domain.Stages unless defined? Stages
  java_import com.thoughtworks.go.domain.Stage unless defined? Stage
  java_import com.thoughtworks.go.config.TrackingTool unless defined? TrackingTool
  java_import com.thoughtworks.go.presentation.pipelinehistory.JobHistory unless defined? JobHistory
  java_import com.thoughtworks.go.presentation.pipelinehistory.NullStageHistoryItem unless defined? NullStageHistoryItem
  java_import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel unless defined? PipelineInstanceModel
  java_import com.thoughtworks.go.presentation.pipelinehistory.PipelineModel unless defined? PipelineModel
  java_import com.thoughtworks.go.presentation.pipelinehistory.StageHistoryPage unless defined? StageHistoryPage
  java_import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels unless defined? StageInstanceModels
  java_import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel unless defined? StageInstanceModel
  java_import com.thoughtworks.go.server.domain.JobDurationStrategy unless defined? JobDurationStrategy
  java_import com.thoughtworks.go.server.domain.Username unless defined? Username
  java_import com.thoughtworks.go.serverhealth.HealthStateScope unless defined? HealthStateScope
  java_import com.thoughtworks.go.serverhealth.HealthStateType unless defined? HealthStateType
  java_import com.thoughtworks.go.server.ui.JobInstanceModel unless defined? JobInstanceModel
  java_import com.thoughtworks.go.server.ui.StageSummaryModel unless defined? StageSummaryModel
  java_import com.thoughtworks.go.server.ui.StageSummaryModels unless defined? StageSummaryModels
  java_import com.thoughtworks.go.server.util.Pagination unless defined? Pagination
  java_import org.joda.time.Duration unless defined? Duration
  java_import com.thoughtworks.go.domain.GoConfigRevision unless defined? GoConfigRevision
  java_import com.thoughtworks.go.util.TimeProvider unless defined? TimeProvider
  java_import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor unless defined? GoPluginDescriptor
  java_import com.thoughtworks.go.plugin.domain.analytics.SupportedAnalytics unless defined? SupportedAnalytics
  java_import com.thoughtworks.go.plugin.domain.analytics.AnalyticsPluginInfo unless defined? AnalyticsPluginInfo
  java_import com.thoughtworks.go.plugin.domain.common.CombinedPluginInfo unless defined? CombinedPluginInfo

end
