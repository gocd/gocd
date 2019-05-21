##########################################################################
# Copyright 2018 ThoughtWorks, Inc.
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

require 'rails_helper'

describe ApiV1::Admin::Internal::ResourcesController do
  include ApiHeaderSetupForRouting
  include ApiV1::ApiVersionHelper

  before(:each) do
    @go_config_service = double("go_config_service")
    allow(controller).to receive("go_config_service").and_return(@go_config_service)
    allow(@go_config_service).to receive(:checkConfigFileValid).and_return(GoConfigValidity::valid())
  end

  describe "security" do
    describe "index" do

      it 'should allow anyone, with security disabled' do
        disable_security

        expect(controller).to allow_action(:get, :index)
      end

      it 'should disallow non-admin user, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:get, :index).with(403, 'You are not authorized to perform this action.')
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin

        expect(controller).to allow_action(:get, :index)
      end

      it 'should not allow group admin users, with security enabled' do
        login_as_group_admin

        expect(controller).to disallow_action(:get, :index)
      end

    end
  end

  describe "action" do
    before :each do
      enable_security
    end

    describe "index" do
      it 'should fetch all the resources' do
        login_as_admin
        resources_list = %w(linux windows)
        expect(@go_config_service).to receive(:getResourceList).and_return(resources_list)

        get_with_api_header :index

        expect(response).to be_ok
        expect(response.headers["ETag"]).not_to include('W/')
        expect(JSON.parse(response.body)).to eq(resources_list)
      end

      it 'should not recompute the resources list when not modified and etag provided' do
        login_as_admin
        resources_list = %w(linux windows).sort
        expect(@go_config_service).to receive(:getResourceList).twice.and_return(resources_list)
        controller.request.env['HTTP_IF_NONE_MATCH'] = controller.send(:generate_weak_etag, resources_list)

        get_with_api_header :index

        etag = response.headers['ETag']

        request.if_none_match = etag

        get_with_api_header :index

        expect(response.code).to eq('304')
        expect(response.body).to be_empty
      end

      it 'should recompute the resources list when it is modified and stale etag provided' do
        login_as_admin
        resources_list = %w(linux windows).sort
        expect(@go_config_service).to receive(:getResourceList).and_return(resources_list)
        controller.request.env['HTTP_IF_NONE_MATCH'] = 'stale-etag'

        get_with_api_header :index

        expect(response).to be_ok
        expect(JSON.parse(response.body)).to eq(resources_list)
      end


    end
  end
end
