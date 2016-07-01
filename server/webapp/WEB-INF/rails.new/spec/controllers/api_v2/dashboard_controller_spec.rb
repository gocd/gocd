##########################################################################
# Copyright 2017 ThoughtWorks, Inc.
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
##########################################################################

require 'spec_helper'

describe ApiV2::DashboardController do
  include GoDashboardPipelineMother

  before do
    @user = Username.new(CaseInsensitiveString.new("foo"))

    controller.stub(:current_user).and_return(@user)
    controller.stub(:populate_config_validity)

    @go_dashboard_service = stub_service(:go_dashboard_service)
  end

  describe :dashboard do
    it 'should get dashboard json' do
      all_pipelines = [dashboard_pipeline("pipeline1"), dashboard_pipeline("pipeline2")]
      @go_dashboard_service.should_receive(:allPipelinesForDashboard).and_return(all_pipelines)

      get_with_api_header :dashboard
      expect(response).to be_ok
      expect(actual_response).to eq(expected_response(all_pipelines, ApiV2::Dashboard::PipelineGroupsRepresenter))
    end

    it 'should get empty json when dashboard is empty' do
      no_pipelines = []
      @go_dashboard_service.should_receive(:allPipelinesForDashboard).and_return(no_pipelines)

      get_with_api_header :dashboard
      expect(response).to be_ok
      expect(actual_response).to eq(expected_response(no_pipelines, ApiV2::Dashboard::PipelineGroupsRepresenter))
    end

    it 'should not output any pipelines which the current user does not have permission to view' do
      permissions = Permissions.new(NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE)

      pipeline_which_user_can_see = dashboard_pipeline("pipeline1", "group1")
      pipeline_which_user_cannot_see = dashboard_pipeline("pipeline2", "group1", permissions)

      all_pipelines = [pipeline_which_user_can_see, pipeline_which_user_cannot_see]
      expected_pipelines_in_output = [pipeline_which_user_can_see]
      @go_dashboard_service.should_receive(:allPipelinesForDashboard).and_return(all_pipelines)

      get_with_api_header :dashboard
      expect(response).to be_ok
      expect(actual_response).to eq(expected_response(expected_pipelines_in_output, ApiV2::Dashboard::PipelineGroupsRepresenter))
    end
  end
end
