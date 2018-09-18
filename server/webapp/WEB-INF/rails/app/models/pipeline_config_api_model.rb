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

class PipelineConfigAPIModel
  attr_reader :name, :label, :materials, :stages

  def initialize(pipeline_config_model)
    @name = pipeline_config_model.name().to_s unless pipeline_config_model.name() == nil
    @label = pipeline_config_model.getLabelTemplate()
    @materials = pipeline_config_model.materialConfigs().collect do |material_config_model|
      MaterialConfigAPIModel.new(material_config_model)
    end
    @stages = pipeline_config_model.getStages().collect do |stage_config_model|
      StageConfigAPIModel.new(stage_config_model)
    end
  end
end