#
# Copyright 2023 Thoughtworks, Inc.
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
end
