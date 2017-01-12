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

describe ApiV1::Admin::EnvironmentsWithRemoteController do
  include ApiHeaderSetupTeardown, ApiV1::ApiVersionHelper

  describe :show do
    before(:each) do
      @environment_name = 'foo-environment'
      local = BasicEnvironmentConfig.new(CaseInsensitiveString.new(@environment_name))
      remote = BasicEnvironmentConfig.new(CaseInsensitiveString.new(@environment_name))
      @environment_config = MergeEnvironmentConfig.new(local, remote)
      @environment_config_service = double('environment-config-service')
      controller.stub(:environment_config_service).and_return(@environment_config_service)
      display_element = com.thoughtworks.go.domain.ConfigElementForEdit.new(@environment_config, 'md5')
      @environment_config_service.stub(:forDisplay).and_return(display_element)
      @environment_config_service.stub(:forEdit).and_return(local)
    end

    describe :for_admins do
      it 'should render the environment' do
        login_as_admin

        get_with_api_header :show, name: @environment_name
        expect(response.status).to eq(200)
        expect(actual_response).to eq(expected_response(@environment_config, ApiV1::Config::EnvironmentConfigRepresenter))
      end

      it 'should render 404 when a environment does not exist' do
        login_as_admin

        @environment_name = SecureRandom.hex
        @environment_config_service.stub(:forDisplay).and_raise(com.thoughtworks.go.config.exceptions.NoSuchEnvironmentException.new(CaseInsensitiveString.new('foo-env')))
        get_with_api_header :show, name: @environment_name
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe :security do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:get, :show, name: @environment_name)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:get, :show, name: @environment_name).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:get, :show, name: @environment_name).with(401, 'You are not authorized to perform this action.')
      end
    end

    describe :route do
      describe :with_header do
        it 'should route to show action of environments controller for alphanumeric environment name' do
          expect(:get => 'api/admin/environments/foo123/withremote').to route_to(action: 'show', controller: 'api_v1/admin/environments_with_remote', name: 'foo123')
        end

        it 'should route to show action of environments controller for environment name with dots' do
          expect(:get => 'api/admin/environments/foo.123/withremote').to route_to(action: 'show', controller: 'api_v1/admin/environments_with_remote', name: 'foo.123')
        end

        it 'should route to show action of environments controller for environment name with hyphen' do
          expect(:get => 'api/admin/environments/foo-123/withremote').to route_to(action: 'show', controller: 'api_v1/admin/environments_with_remote', name: 'foo-123')
        end

        it 'should route to show action of environments controller for environment name with underscore' do
          expect(:get => 'api/admin/environments/foo_123/withremote').to route_to(action: 'show', controller: 'api_v1/admin/environments_with_remote', name: 'foo_123')
        end

        it 'should route to show action of environments controller for capitalized environment name' do
          expect(:get => 'api/admin/environments/FOO/withremote').to route_to(action: 'show', controller: 'api_v1/admin/environments_with_remote', name: 'FOO')
        end
      end
      describe :without_header do
        before :each do
          teardown_header
        end
        it 'should not route to show action of environments controller without header' do
          expect(:get => 'api/admin/environments/foo/withremote').to_not route_to(action: 'show', controller: 'api_v1/admin/environments_with_remote')
          expect(:get => 'api/admin/environments/foo/withremote').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/environments/foo/withremote')
        end
      end
    end

  end
end
