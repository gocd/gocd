#
# Copyright 2019 ThoughtWorks, Inc.
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

module Admin
  module PipelinesHelper
    include JavaImports

    def default_stage_config
      job_configs = JobConfigs.new([JobConfig.new(CaseInsensitiveString.new("defaultJob"), ResourceConfigs.new, ArtifactTypeConfigs.new, com.thoughtworks.go.config.Tasks.new([AntTask.new].to_java(Task)))].to_java(JobConfig))
      StageConfig.new(CaseInsensitiveString.new("defaultStage"), job_configs)
    end

    def use_template?(params)
      "configurationType_template" == params.try(:[], :pipeline_group).try(:[], :pipeline).try(:[], :configurationType)
    end
  end
end
