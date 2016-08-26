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

describe ApiV1::Admin::Internal::PipelinesController do
  before(:each) do
    @pipeline_config_service = double('pipeline_config_service')
    controller.stub('pipeline_config_service').and_return(@pipeline_config_service)
  end

  describe :security do
    describe :index do
      it 'should allow anyone, with security disabled' do
        disable_security

        expect(controller).to allow_action(:get, :index)
      end

      it 'should disallow non-admin user, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:get, :index).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin

        expect(controller).to allow_action(:get, :index)
      end

      it 'should allow group admin users, with security enabled' do
        login_as_group_admin

        expect(controller).to allow_action(:get, :index)
      end
    end
  end

  describe :action do
    before :each do
      enable_security
    end

    describe :index do
      it 'should fetch all the pipelines for the user' do
        login_as_admin
        pipeline_configs = BasicPipelineConfigs.new(PipelineConfigMother.createPipelineConfigWithStages('regression', 'fetch', 'run'))
        pipeline_configs_list = Arrays.asList(pipeline_configs)

        @pipeline_config_service.should_receive(:viewableOrOperatableGroupsFor).with(controller.current_user).and_return(pipeline_configs_list)

        get_with_api_header :index

        expect(response).to be_ok
        expected_response = expected_response(pipeline_configs_list, ApiV1::Config::PipelineConfigsWithMinimalAttributesRepresenter)
        expect(actual_response).to eq(expected_response)
      end
      describe :route do
        describe :with_header do
          before :each do
            Rack::MockRequest::DEFAULT_ENV["HTTP_ACCEPT"] = "application/vnd.go.cd.v1+json"
          end
          after :each do
            Rack::MockRequest::DEFAULT_ENV = {}
          end

          it 'should route to index action of the internal pipelines controller' do
            expect(:get => 'api/admin/internal/pipelines').to route_to(action: 'index', controller: 'api_v1/admin/internal/pipelines')
          end
        end
        describe :without_header do
          it 'should not route to index action of internal pipelines controller without header' do
            expect(:get => 'api/admin/internal/pipelines').to_not route_to(action: 'index', controller: 'api_v1/admin/internal/pipelines')
            expect(:get => 'api/admin/internal/pipelines').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/internal/pipelines')
          end
        end
      end
    end
  end
end
