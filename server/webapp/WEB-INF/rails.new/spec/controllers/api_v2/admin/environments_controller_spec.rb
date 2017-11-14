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

describe ApiV2::Admin::EnvironmentsController do
  describe "index" do
    describe "for_admins" do
      it 'should render a list of environments, for admins' do
        login_as_admin

        env = BasicEnvironmentConfig.new(CaseInsensitiveString.new('foo-env'))
        environments = java.util.HashSet.new
        environments.add(env)

        @environment_config_service = double('environment-config-service')
        allow(controller).to receive(:environment_config_service).and_return(@environment_config_service)
        expect(@environment_config_service).to receive(:getEnvironments).and_return(environments)

        get_with_api_header :index
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response([env], ApiV2::Config::EnvironmentsConfigRepresenter))
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
      allow(@environment_config_service).to receive(:getEnvironmentForEdit).with(@environment_name).and_return(@environment_config)
    end

    describe "for_admins" do
      it 'should render the environment' do
        login_as_admin

        get_with_api_header :show, params: { name: @environment_name }
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(@environment_config, ApiV2::Config::EnvironmentConfigRepresenter))
      end

      it 'should render 404 when a environment does not exist' do
        login_as_admin

        @environment_name = SecureRandom.hex
        allow(@environment_config_service).to receive(:getEnvironmentForEdit).and_return(nil)
        get_with_api_header :show, params: { name: @environment_name }
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe "security" do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:get, :show, params: { name: @environment_name })
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:get, :show, params: { name: @environment_name }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:get, :show, params: { name: @environment_name }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:get, :show, params: { name: @environment_name })
      end

      it 'should disallow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to disallow_action(:get, :show, params: { name: @environment_name }).with(401, 'You are not authorized to perform this action.')
      end
    end
  end

  describe "put" do

    before(:each) do
      @environment_name = 'foo-environment'
      @md5 = 'some-digest'
      @environment_config = BasicEnvironmentConfig.new(CaseInsensitiveString.new(@environment_name))
      @environment_config_service = double('environment-config-service')
      @entity_hashing_service = double('entity-hashing-see=rvice')
      allow(controller).to receive(:environment_config_service).and_return(@environment_config_service)
      allow(controller).to receive(:entity_hashing_service).and_return(@entity_hashing_service)
      allow(@environment_config_service).to receive(:getEnvironmentForEdit).with(@environment_name).and_return(@environment_config)
      allow(@entity_hashing_service).to receive(:md5ForEntity).and_return(@md5)
    end

    describe "for_admins" do
      it 'should allow updating environments' do
        login_as_admin
        result = HttpLocalizedOperationResult.new
        expect(@environment_config_service).to receive(:updateEnvironment).with(@environment_name, anything, anything, @md5, anything).and_return(result)
        hash = {name: @environment_name, pipelines: [], agents: [], environment_variables: []}

        controller.request.env['HTTP_IF_MATCH'] = "\"#{Digest::MD5.hexdigest(@md5)}\""

        put_with_api_header :put, params: { name: @environment_name, :environment => hash }
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(@environment_config, ApiV2::Config::EnvironmentConfigRepresenter))
      end

      it 'should not put environment config if etag passed does not match the one on server' do
        login_as_admin
        controller.request.env['HTTP_IF_MATCH'] = 'old-etag'

        put_with_api_header :put, params: { name: @environment_name, :environment => {name: @environment_name, pipelines: [], agents: [], environment_variables: []} }

        expect(response).to have_api_message_response(412, "Someone has modified the configuration for environment 'foo-environment'. Please update your copy of the config with the changes.")
      end

      it 'should not put environment config if no etag is passed' do
        login_as_admin
        put_with_api_header :put, params: { name: @environment_name, :environment => {name: @environment_name, pipelines: [], agents: [], environment_variables: []} }

        expect(response).to have_api_message_response(412, "Someone has modified the configuration for environment 'foo-environment'. Please update your copy of the config with the changes.")
      end

      it 'should render 404 when a environment does not exist' do
        login_as_admin

        @environment_name = SecureRandom.hex
        allow(@environment_config_service).to receive(:getEnvironmentForEdit).and_return(nil)
        put_with_api_header :put, params: { name: @environment_name }
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe "security" do
      before(:each) do
        allow(controller).to receive(:check_for_stale_request).and_return(nil)
      end

      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:put, :put, params: { name: @environment_name })
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:put, :put, params: { name: @environment_name }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:put, :put, params: { name: @environment_name }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:put, :put, params: { name: @environment_name })
      end

      it 'should disallow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to disallow_action(:put, :put, params: { name: @environment_name }).with(401, 'You are not authorized to perform this action.')
      end
    end
  end

  describe "patch" do

    before(:each) do
      @environment_name = 'foo-environment'
      @environment_config = BasicEnvironmentConfig.new(CaseInsensitiveString.new(@environment_name))
      @environment_config_service = double('environment-config-service')
      allow(controller).to receive(:environment_config_service).and_return(@environment_config_service)
      allow(@environment_config_service).to receive(:getEnvironmentForEdit).with(@environment_name).and_return(@environment_config)
      @result = HttpLocalizedOperationResult.new
      @pipelines_to_add = ['foo']
      @pipelines_to_remove = ['bar']
      @agents_to_add = ['agent1']
      @agents_to_remove = ['agent2']
      @env_vars_to_add = []
      @env_vars_to_remove = []

      login_as_admin
    end

    describe "for_admins" do
      it 'should allow patching environments' do
        expect(@environment_config_service).to receive(:patchEnvironment).with(@environment_config, @pipelines_to_add, @pipelines_to_remove, @agents_to_add, @agents_to_remove, @env_vars_to_add, @env_vars_to_remove, anything, @result).and_return(@result)

        patch_with_api_header :patch, params: { name: @environment_name, :pipelines => {add: @pipelines_to_add, remove: @pipelines_to_remove}, :agents => {add: @agents_to_add, remove: @agents_to_remove}, :environment_variables => {add: @env_vars_to_add, remove: @env_vars_to_remove} }
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(@environment_config, ApiV2::Config::EnvironmentConfigRepresenter))
      end

      it 'should render error when it fails to patch environment' do
        allow(@environment_config_service).to receive(:patchEnvironment).with(@environment_config, @pipelines_to_add, @pipelines_to_remove, @agents_to_add, @agents_to_remove, @env_vars_to_add, @env_vars_to_remove, anything, @result) do |environment_config, pipelines_to_add, pipelines_to_remove, agents_to_add, agents_to_remove, env_vars_to_add, env_vars_to_remove, user, result|
          result.badRequest(LocalizedMessage.string("PIPELINES_WITH_NAMES_NOT_FOUND", pipelines_to_add))
        end

        patch_with_api_header :patch, params: { name: @environment_name, :pipelines => {add: @pipelines_to_add, remove: @pipelines_to_remove}, :agents => {add: @agents_to_add, remove: @agents_to_remove}, :environment_variables => {add: @env_vars_to_add, remove: @env_vars_to_remove} }
        expect(response).to have_api_message_response(400, 'Pipelines(s) with name(s) [foo] not found.')
      end

      it 'should render 404 when a environment does not exist' do
        @environment_name = SecureRandom.hex
        allow(@environment_config_service).to receive(:getEnvironmentForEdit).and_return(nil)
        patch_with_api_header :patch, params: { name: @environment_name }
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe "security" do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:patch, :patch, params: { name: @environment_name })
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:patch, :patch, params: { name: @environment_name }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:patch, :patch, params: { name: @environment_name }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:patch, :patch, params: { name: @environment_name })
      end

      it 'should disallow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to disallow_action(:patch, :patch, params: { name: @environment_name }).with(401, 'You are not authorized to perform this action.')
      end

    end
  end

  describe "destroy" do
    before(:each) do
      @environment_name = 'foo-environment'
      @environment_config = BasicEnvironmentConfig.new(CaseInsensitiveString.new(@environment_name))
      @environment_config_service = double('environment-config-service')
      allow(controller).to receive(:environment_config_service).and_return(@environment_config_service)
      allow(@environment_config_service).to receive(:getEnvironmentForEdit).with(@environment_name).and_return(@environment_config)
      login_as_admin
    end

    describe "for_admins" do
      it 'should allow deleting environments' do
        expect(@environment_config_service).to receive(:deleteEnvironment).with(@environment_config, an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult)) do |envConfig, user, result|
          result.setMessage(LocalizedMessage.string('RESOURCE_DELETE_SUCCESSFUL', 'environment', @environment_config.name))
        end

        delete_with_api_header :destroy, params: { name: @environment_name }
        expect(response).to have_api_message_response(200, "The environment 'foo-environment' was deleted successfully.")
      end

      it 'should render 404 when a environment does not exist' do
        @environment_name = SecureRandom.hex
        allow(@environment_config_service).to receive(:getEnvironmentForEdit).and_return(nil)
        delete_with_api_header :destroy, params: { name: @environment_name }
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe "security" do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:delete, :destroy, params: { name: @environment_name })
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:delete, :destroy, params: { name: @environment_name }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:delete, :destroy, params: { name: @environment_name }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:delete, :destroy, params: { name: @environment_name })
      end

      it 'should disallow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to disallow_action(:delete, :destroy, params: { name: @environment_name }).with(401, 'You are not authorized to perform this action.')
      end

    end
  end

  describe "create" do
    before(:each) do
      @environment_name = 'foo-environment'
      @environment_config = BasicEnvironmentConfig.new(CaseInsensitiveString.new(@environment_name))
      @environment_config_service = double('environment-config-service')
      allow(controller).to receive(:environment_config_service).and_return(@environment_config_service)
      allow(@environment_config_service).to receive(:getEnvironmentForEdit).with(@environment_name).and_return(@environment_config)

      login_as_admin
    end

    describe "for_admins" do
      it 'should render 200 created when environment is created' do
        expect(@environment_config_service).to receive(:createEnvironment)

        post_with_api_header :create, params: { :environment => {name: @environment_name, pipelines: [], agents: [], environment_variables: []} }
        expect(response.status).to be(200)
        expect(actual_response).to eq(expected_response(@environment_config, ApiV2::Config::EnvironmentConfigRepresenter))
      end

      it 'should render the error occurred while creating an environment' do
        expect(@environment_config_service).to receive(:createEnvironment).with(@environment_config, an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult)) do |env, user, result|
          result.conflict(LocalizedMessage.string("RESOURCE_ALREADY_EXISTS", 'environment', env.name));
        end

        post_with_api_header :create, params: { :environment => {name: @environment_name, pipelines: [], agents: [], environment_variables: []} }
        expect(response).to have_api_message_response(409, "Failed to add environment. The environment 'foo-environment' already exists.")
      end
    end

    describe "security" do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:create, :create)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:post, :create).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:post, :create).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:create, :create)
      end

      it 'should disallow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to disallow_action(:post, :create).with(401, 'You are not authorized to perform this action.')
      end

    end
  end
end
