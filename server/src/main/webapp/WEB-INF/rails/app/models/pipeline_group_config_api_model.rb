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

class PipelineGroupConfigAPIModel
  attr_reader :name, :pipelines

  def initialize(pipeline_group_config_model)
    @name = pipeline_group_config_model.getGroup()
    @pipelines = pipeline_group_config_model.getPipelines().collect do |pipeline_config_model|
      PipelineConfigAPIModel.new(pipeline_config_model)
    end
  end
end