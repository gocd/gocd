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

require 'rails_helper'

describe ApiV1::Admin::MergedEnvironmentsController do

  include ApiV1::ApiVersionHelper

  describe "index" do
    before(:each) do
      environment_name = 'foo-environment'
      @environment_config = BasicEnvironmentConfig.new(CaseInsensitiveString.new(environment_name))
      @environment_config_service = double('environment-config-service')
      allow(controller).to receive(:environment_config_service).and_return(@environment_config_service)
      allow(@environment_config_service).to receive(:getAllMergedEnvironments).and_return([@environment_config])
    end

    describe "for_admins" do
      it 'should render the environment' do
        login_as_admin

        get_with_api_header :index
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response([@environment_config], ApiV1::Admin::MergedEnvironments::MergedEnvironmentsConfigRepresenter))
      end
    end

    describe "security" do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:get, :index)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:get, :index).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:get, :index).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:get, :index)
      end

      it 'should disallow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to disallow_action(:get, :index).with(401, 'You are not authorized to perform this action.')
      end
    end
  end

  describe "show" do
    before(:each) do
      @environment_name = 'foo-environment'
      @environment_config = BasicEnvironmentConfig.new(CaseInsensitiveString.new(@environment_name))
      @environment_config_service = double('environment-config-service')
      allow(controller).to receive(:environment_config_service).and_return(@environment_config_service)
      environment_config_element = com.thoughtworks.go.domain.ConfigElementForEdit.new(@environment_config, "md5")
      allow(@environment_config_service).to receive(:getMergedEnvironmentforDisplay).and_return(environment_config_element)
      allow(@environment_config_service).to receive(:getEnvironmentForEdit).with(@environment_name).and_return(@environment_config)
    end

    describe "for_admins" do
      it 'should render the environment' do
        login_as_admin

        get_with_api_header :show, params: { environment_name: @environment_name }
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(@environment_config, ApiV1::Admin::MergedEnvironments::MergedEnvironmentConfigRepresenter))
      end

      it 'should render 404 when a environment does not exist' do
        login_as_admin

        @environment_name = SecureRandom.hex
        allow(@environment_config_service).to receive(:getMergedEnvironmentforDisplay).and_return(nil)
        get_with_api_header :show, params: { environment_name: @environment_name, withconfigrepo: 'true' }
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe "security" do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:get, :show, params: { environment_name: @environment_name })
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:get, :show, params: { environment_name: @environment_name }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:get, :show, params: { environment_name: @environment_name }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:get, :show, params: { environment_name: @environment_name })
      end

      it 'should disallow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to disallow_action(:get, :show, params: { environment_name: @environment_name }).with(401, 'You are not authorized to perform this action.')
      end
    end
  end
end
