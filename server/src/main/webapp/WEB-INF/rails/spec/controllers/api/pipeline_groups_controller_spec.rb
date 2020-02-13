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

def schedule_options(specified_revisions, variables, secure_variables = {})
  ScheduleOptions.new(HashMap.new(specified_revisions), HashMap.new(variables), HashMap.new(secure_variables))
end

describe Api::PipelineGroupsController do
  include APIModelMother

  describe "list_pipeline_group_configs" do
    before :each do
      allow(controller).to receive(:pipeline_configs_service).and_return(@pipeline_configs_service = double('pipeline_configs_service'))
      allow(controller).to receive(:feature_toggle_service).and_return(@feature_toggle_service = double('feature_toggle_service'))

      allow(@feature_toggle_service).to receive(:isToggleOn).with("enable_pipeline_group_config_listing_api").and_return(true)
    end

    it "should resolve" do
      expect(:get => "/api/config/pipeline_groups").to route_to(:controller => "api/pipeline_groups", :action => "list_configs", :no_layout=>true)
    end

    it "should add deprecation API headers" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      expect(controller).to receive(:current_user).and_return(loser)
      expect(@pipeline_configs_service).to receive(:getGroupsForUser).with("loser").and_return([create_pipeline_group_config_model])

      get :list_configs, params:{:no_layout => true}

      expect(response).to be_ok
      expect(response.headers["X-GoCD-API-Deprecated-In"]).to eq('v19.12.0')
      expect(response.headers["X-GoCD-API-Removal-In"]).to eq('v20.3.0')
      expect(response.headers["X-GoCD-API-Deprecation-Info"]).to eq("https://api.gocd.org/19.12.0/#api-changelog")
      expect(response.headers["Link"]).to eq('<http://test.host/go/api/admin/pipeline_groups>; Accept="application/vnd.go.cd.v1+json"; rel="successor-version"')
      expect(response.headers["Warning"]).to eq('299 GoCD/v19.12.0 "The Pipeline Groups Config Listing unversioned API has been deprecated in GoCD Release v19.12.0. This version will be removed in GoCD Release v20.3.0. Version v1 of the API is available, and users are encouraged to use it"')
    end

    it "should render pipeline group list json" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      expect(controller).to receive(:current_user).and_return(loser)
      expect(@pipeline_configs_service).to receive(:getGroupsForUser).with("loser").and_return([create_pipeline_group_config_model])

      get :list_configs, params:{:no_layout => true}

      expect(response.body).to eq([PipelineGroupConfigAPIModel.new(create_pipeline_group_config_model)].to_json)
    end

    it "should render not found error when API toggle is turned off" do
      allow(@feature_toggle_service).to receive(:isToggleOn).with("enable_pipeline_group_config_listing_api").and_return(false)

      get :list_configs, params:{:no_layout => true}

      expect(response.body).to eq({message: 'Either the resource you requested was not found, or you are not authorized to perform this action.'}.to_json)
    end
  end
end
