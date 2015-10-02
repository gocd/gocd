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

require 'spec_helper'

describe PipelineConfigAPIModel do
  include APIModelMother

  describe "should initialize correctly" do
    it "should populate correct data" do
      @pipeline_config_view_model = create_pipeline_config_model
      pipeline_config_api_model = PipelineConfigAPIModel.new(@pipeline_config_view_model)

      pipeline_config_api_model.name.should == 'pipeline name'
      pipeline_config_api_model.label.should == 'label'

      material_config_api_model = pipeline_config_api_model.materials[0]
      material_config_api_model.fingerprint.should == 'fingerprint'
      material_config_api_model.type.should == 'git'
      material_config_api_model.description.should == 'URL: http://test.com Branch: master'

      stage_config_api_model = pipeline_config_api_model.stages[0]
      stage_config_api_model.name.should == 'stage name'
    end
  end
end
