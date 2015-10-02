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

def schedule_options(specified_revisions, variables, secure_variables = {})
  ScheduleOptions.new(HashMap.new(specified_revisions), HashMap.new(variables), HashMap.new(secure_variables))
end

describe Api::PipelineGroupsController do
  include APIModelMother

  describe :list_pipeline_group_configs do
    before :each do
      controller.stub(:pipeline_configs_service).and_return(@pipeline_configs_service = double('pipeline_configs_service'))
    end

    it "should resolve" do
      expect(:get => "/api/config/pipeline_groups").to route_to(:controller => "api/pipeline_groups", :action => "list_configs", :no_layout=>true)
    end

    it "should render pipeline group list json" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      controller.should_receive(:current_user).and_return(loser)
      @pipeline_configs_service.should_receive(:getGroupsForUser).with("loser").and_return([create_pipeline_group_config_model])

      get :list_configs, :no_layout => true

      expect(response.body).to eq([PipelineGroupConfigAPIModel.new(create_pipeline_group_config_model)].to_json)
    end
  end
end
