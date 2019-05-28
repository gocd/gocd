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

require 'rails_helper'

describe PipelineConfigAPIModel do
  include APIModelMother

  describe "should initialize correctly" do
    it "should populate correct data" do
      @pipeline_config_view_model = create_pipeline_config_model
      pipeline_config_api_model = PipelineConfigAPIModel.new(@pipeline_config_view_model)

      expect(pipeline_config_api_model.name).to eq('pipeline name')
      expect(pipeline_config_api_model.label).to eq('label')

      material_config_api_model = pipeline_config_api_model.materials[0]
      expect(material_config_api_model.fingerprint).to eq('fingerprint')
      expect(material_config_api_model.type).to eq('git')
      expect(material_config_api_model.description).to eq('URL: http://test.com Branch: master')

      stage_config_api_model = pipeline_config_api_model.stages[0]
      expect(stage_config_api_model.name).to eq('stage name')
    end
  end
end
