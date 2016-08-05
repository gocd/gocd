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

describe ApiV1::Admin::Internal::ResourcesController do
  before(:each) do
    @go_config_service = double("go_config_service")
    controller.stub("go_config_service").and_return(@go_config_service)
    @go_config_service.stub(:checkConfigFileValid).and_return(GoConfigValidity::valid())
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
      it 'should fetch all the resources' do
        login_as_admin
        resources_list = %w(linux windows)
        @go_config_service.should_receive(:getResourceList).and_return(resources_list)

        get_with_api_header :index

        expect(response).to be_ok
        expect(JSON.parse(response.body)).to eq(resources_list)
      end

      it 'should not recompute the resources list when not modified and etag provided' do
        login_as_admin
        resources_list = %w(linux windows).sort
        @go_config_service.should_receive(:getResourceList).and_return(resources_list)
        controller.request.env['HTTP_IF_NONE_MATCH'] = Digest::MD5.hexdigest(resources_list.join('/'))

        get_with_api_header :index

        expect(response.code).to eq('304')
        expect(response.body).to be_empty
      end

      it 'should recompute the resources list when it is modified and stale etag provided' do
        login_as_admin
        resources_list = %w(linux windows).sort
        @go_config_service.should_receive(:getResourceList).and_return(resources_list)
        controller.request.env['HTTP_IF_NONE_MATCH'] = 'stale-etag'

        get_with_api_header :index

        expect(response).to be_ok
        expect(JSON.parse(response.body)).to eq(resources_list)
      end

      describe :route do
        describe :with_header do
          before :each do
            Rack::MockRequest::DEFAULT_ENV["HTTP_ACCEPT"] = "application/vnd.go.cd.v1+json"
          end
          after :each do
            Rack::MockRequest::DEFAULT_ENV = {}
          end

          it 'should route to index action of the internal resources controller' do
            expect(:get => 'api/admin/internal/resources').to route_to(action: 'index', controller: 'api_v1/admin/internal/resources')
          end
        end
        describe :without_header do
          it 'should not route to index action of internal resources controller without header' do
            expect(:get => 'api/admin/internal/resources').to_not route_to(action: 'index', controller: 'api_v1/admin/internal/resources')
            expect(:get => 'api/admin/internal/resources').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/internal/resources')
          end
        end
      end

    end
  end
end
