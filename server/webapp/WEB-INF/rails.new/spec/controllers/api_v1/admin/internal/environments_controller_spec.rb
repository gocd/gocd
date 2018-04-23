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

require 'rails_helper'

describe ApiV1::Admin::Internal::EnvironmentsController do

  include ApiV1::ApiVersionHelper

  before(:each) do
    @environment_config_service = double("environment_config_service")
    allow(controller).to receive("environment_config_service").and_return(@environment_config_service)
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

        expect(controller).to disallow_action(:get, :index).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin

        expect(controller).to allow_action(:get, :index)
      end
    end
  end

  describe "action" do
    before :each do
      enable_security
    end

    describe "index" do
      it 'should fetch all the environments' do
        login_as_admin
        environments_list = %w(dev production)
        expect(@environment_config_service).to receive(:environmentNames).and_return(environments_list)

        get_with_api_header :index

        expect(response).to be_ok
        expect(JSON.parse(response.body)).to eq(environments_list)
      end

      it 'should not recompute the environments list when not modified and etag provided' do
        login_as_admin
        environments_list = %w(dev production).sort
        expect(@environment_config_service).to receive(:environmentNames).and_return(environments_list)
        controller.request.env['HTTP_IF_NONE_MATCH'] = %Q{"#{Digest::MD5.hexdigest(environments_list.join('/'))}"}

        get_with_api_header :index

        expect(response.code).to eq('304')
        expect(response.body).to be_empty
      end

      it 'should recompute the environments list when it is modified and stale etag provided' do
        login_as_admin
        environments_list = %w(dev production)
        expect(@environment_config_service).to receive(:environmentNames).and_return(environments_list)

        controller.request.env['HTTP_IF_NONE_MATCH'] = 'stale-etag'

        get_with_api_header :index

        expect(response).to be_ok
        expect(JSON.parse(response.body)).to eq(environments_list)
      end
    end
  end
end
