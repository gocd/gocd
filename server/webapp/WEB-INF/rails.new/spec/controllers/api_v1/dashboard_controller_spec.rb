##########################################################################
# Copyright 2016 ThoughtWorks, Inc.
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

describe ApiV1::DashboardController do

  before do
    @user                  = Username.new(CaseInsensitiveString.new("foo"))
    @pipeline_group_models = java.util.ArrayList.new
    controller.stub(:current_user).and_return(@user)
    controller.stub(:pipeline_history_service).and_return(@pipeline_history_service=double())
    controller.stub(:go_config_service).and_return(@go_config_service=double())
    controller.stub(:populate_config_validity)
  end

  describe :dashboard do
    it 'should get dashboard json' do
      @pipeline_group_models.add(PipelineGroupModel.new("bla"))
      @go_config_service.should_receive(:getSelectedPipelines).with(@selected_pipeline_id, @user_id).and_return(selections=PipelineSelections.new)
      @pipeline_history_service.should_receive(:allActivePipelineInstances).with(@user, selections).and_return(@pipeline_group_models)

      get_with_api_header :dashboard
      expect(response).to be_ok
      expect(actual_response).to eq(expected_response(@pipeline_group_models, ApiV1::Dashboard::PipelineGroupsRepresenter))
    end

    it 'should get empty json when dashboard is empty' do
      @go_config_service.should_receive(:getSelectedPipelines).with(@selected_pipeline_id, @user_id).and_return(selections=PipelineSelections.new)
      @pipeline_history_service.should_receive(:allActivePipelineInstances).with(@user, selections).and_return(@pipeline_group_models)

      get_with_api_header :dashboard
      expect(response).to be_ok
      expect(actual_response).to eq(expected_response(@pipeline_group_models, ApiV1::Dashboard::PipelineGroupsRepresenter))
    end

    describe :route do
      describe :with_header do
        before :each do
          Rack::MockRequest::DEFAULT_ENV["HTTP_ACCEPT"] = "application/vnd.go.cd.v1+json"
        end
        after :each do
          Rack::MockRequest::DEFAULT_ENV = {}
        end

        it 'should route to dashboard action of the dashboard controller' do
          expect(:get => 'api/dashboard').to route_to(action: 'dashboard', controller: 'api_v1/dashboard')
        end
      end
      describe :without_header do
        it 'should not route to dashboard action of dashboard controller without header' do
          expect(:get => 'api/dashboard').to_not route_to(action: 'dashboard', controller: 'api_v1/dashboard')
          expect(:get => 'api/dashboard').to route_to(controller: 'application', action: 'unresolved', url: 'api/dashboard')
        end
      end
    end
  end

end
