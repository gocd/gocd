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

describe ApiV1::Admin::EnvironmentsController do

  describe :index do
    describe :for_admins do
      it 'should render a list of environments, for admins' do
        login_as_admin

        env = BasicEnvironmentConfig.new(CaseInsensitiveString.new('foo-env'))
        environments = java.util.HashSet.new
        environments.add(env)

        @environment_config_service = double('environment-config-service')
        controller.stub(:environment_config_service).and_return(@environment_config_service)
        @environment_config_service.should_receive(:getEnvironments).and_return(environments)

        get_with_api_header :index
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response([env], ApiV1::Config::EnvironmentsConfigRepresenter))
      end
    end

    describe :security do
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
    end

    describe :route do
      describe :with_header do
        before :each do
          Rack::MockRequest::DEFAULT_ENV["HTTP_ACCEPT"] = "application/vnd.go.cd.v1+json"
        end
        after :each do
          Rack::MockRequest::DEFAULT_ENV = {}
        end
        it 'should route to index action of environments controller' do
          expect(:get => 'api/admin/environments').to route_to(action: 'index', controller: 'api_v1/admin/environments')
        end
      end
      describe :without_header do
        it 'should not route to index action of environments controller without header' do
          expect(:get => 'api/admin/environments').to_not route_to(action: 'index', controller: 'api_v1/admin/environments')
          expect(:get => 'api/admin/environments').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/environments')
        end
      end
    end
  end

  describe :show do
    before(:each) do
      @environment_name = 'foo-environment'
      @environment_config = BasicEnvironmentConfig.new(CaseInsensitiveString.new(@environment_name))
      @environment_config_service = double('environment-config-service')
      controller.stub(:environment_config_service).and_return(@environment_config_service)
      @environment_config_service.stub(:getEnvironmentConfig).with(@environment_name).and_return(@environment_config)
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
        @environment_config_service.stub(:getEnvironmentConfig).and_raise(com.thoughtworks.go.config.exceptions.NoSuchEnvironmentException.new(CaseInsensitiveString.new('foo-env')))
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
        before :each do
          Rack::MockRequest::DEFAULT_ENV["HTTP_ACCEPT"] = "application/vnd.go.cd.v1+json"
        end
        after :each do
          Rack::MockRequest::DEFAULT_ENV = {}
        end
        it 'should route to show action of environments controller for alphanumeric environment name' do
          expect(:get => 'api/admin/environments/foo123').to route_to(action: 'show', controller: 'api_v1/admin/environments', name: 'foo123')
        end

        it 'should route to show action of environments controller for environment name with dots' do
          expect(:get => 'api/admin/environments/foo.123').to route_to(action: 'show', controller: 'api_v1/admin/environments', name: 'foo.123')
        end

        it 'should route to show action of environments controller for environment name with hyphen' do
          expect(:get => 'api/admin/environments/foo-123').to route_to(action: 'show', controller: 'api_v1/admin/environments', name: 'foo-123')
        end

        it 'should route to show action of environments controller for environment name with underscore' do
          expect(:get => 'api/admin/environments/foo_123').to route_to(action: 'show', controller: 'api_v1/admin/environments', name: 'foo_123')
        end

        it 'should route to show action of environments controller for capitalized environment name' do
          expect(:get => 'api/admin/environments/FOO').to route_to(action: 'show', controller: 'api_v1/admin/environments', name: 'FOO')
        end
      end
      describe :without_header do
        it 'should not route to show action of environments controller without header' do
          expect(:get => 'api/admin/environments/foo').to_not route_to(action: 'show', controller: 'api_v1/admin/environments')
          expect(:get => 'api/admin/environments/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/environments/foo')
        end
      end
    end
  end

  describe :put do

    before(:each) do
      @environment_name = 'foo-environment'
      @md5 = 'some-digest'
      @environment_config = BasicEnvironmentConfig.new(CaseInsensitiveString.new(@environment_name))
      @environment_config_service = double('environment-config-service')
      @entity_hashing_service = double('entity-hashing-see=rvice')
      controller.stub(:environment_config_service).and_return(@environment_config_service)
      controller.stub(:entity_hashing_service).and_return(@entity_hashing_service)
      @environment_config_service.stub(:getEnvironmentConfig).with(@environment_name).and_return(@environment_config)
      @entity_hashing_service.stub(:md5ForEntity).and_return(@md5)
    end

    describe :for_admins do
      it 'should allow updating environments' do
        login_as_admin
        result = HttpLocalizedOperationResult.new
        @environment_config_service.should_receive(:updateEnvironment).with(@environment_config, anything, anything, @md5, anything).and_return(result)
        hash = {name: @environment_name, pipelines: [], agents: [], environment_variables: []}

        controller.request.env['HTTP_IF_MATCH'] = "\"#{Digest::MD5.hexdigest(@md5)}\""

        put_with_api_header :put, name: @environment_name, :environment => hash
        expect(response.status).to eq(200)
        expect(actual_response).to eq(expected_response(@environment_config, ApiV1::Config::EnvironmentConfigRepresenter))
      end

      it 'should not put environment config if etag passed does not match the one on server' do
        login_as_admin
        controller.request.env['HTTP_IF_MATCH'] = 'old-etag'

        put_with_api_header :put, name: @environment_name, :environment => {name: @environment_name, pipelines: [], agents: [], environment_variables: []}

        expect(response.status).to eq(412)
        expect(actual_response).to eq({:message => "Someone has modified the configuration for environment 'foo-environment'. Please update your copy of the config with the changes."})
      end

      it 'should not put environment config if no etag is passed' do
        login_as_admin
        put_with_api_header :put, name: @environment_name, :environment => {name: @environment_name, pipelines: [], agents: [], environment_variables: []}

        expect(response.status).to eq(412)
        expect(response).to have_api_message_response(412, "Someone has modified the configuration for environment 'foo-environment'. Please update your copy of the config with the changes.")
      end

      it 'should render 404 when a environment does not exist' do
        login_as_admin

        @environment_name = SecureRandom.hex
        @environment_config_service.stub(:getEnvironmentConfig).and_raise(com.thoughtworks.go.config.exceptions.NoSuchEnvironmentException.new(CaseInsensitiveString.new('foo-env')))
        put_with_api_header :put, name: @environment_name
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe :security do
      before(:each) do
        controller.stub(:check_for_stale_request).and_return(nil)
      end

      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:put, :put, name: @environment_name)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:put, :put, name: @environment_name).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:put, :put, name: @environment_name).with(401, 'You are not authorized to perform this action.')
      end
    end

    describe :route do
      describe :with_header do
        before :each do
          Rack::MockRequest::DEFAULT_ENV["HTTP_ACCEPT"] = "application/vnd.go.cd.v1+json"
        end
        after :each do
          Rack::MockRequest::DEFAULT_ENV = {}
        end
        it 'should route to put action of environments controller for alphanumeric environment name' do
          expect(:put => 'api/admin/environments/foo123').to route_to(action: 'put', controller: 'api_v1/admin/environments', name: 'foo123')
        end

        it 'should route to put action of environments controller for environment name with dots' do
          expect(:put => 'api/admin/environments/foo.123').to route_to(action: 'put', controller: 'api_v1/admin/environments', name: 'foo.123')
        end

        it 'should route to put action of environments controller for environment name with hyphen' do
          expect(:put => 'api/admin/environments/foo-123').to route_to(action: 'put', controller: 'api_v1/admin/environments', name: 'foo-123')
        end

        it 'should route to put action of environments controller for environment name with underscore' do
          expect(:put => 'api/admin/environments/foo_123').to route_to(action: 'put', controller: 'api_v1/admin/environments', name: 'foo_123')
        end

        it 'should route to put action of environments controller for capitalized environment name' do
          expect(:put => 'api/admin/environments/FOO').to route_to(action: 'put', controller: 'api_v1/admin/environments', name: 'FOO')
        end
      end
      describe :without_header do
        it 'should not route to put action of environments controller without header' do
          expect(:put => 'api/admin/environments/foo').to_not route_to(action: 'put', controller: 'api_v1/admin/environments')
          expect(:put => 'api/admin/environments/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/environments/foo')
        end
      end
    end
  end

  describe :patch do

    before(:each) do
      @environment_name = 'foo-environment'
      @environment_config = BasicEnvironmentConfig.new(CaseInsensitiveString.new(@environment_name))
      @environment_config_service = double('environment-config-service')
      controller.stub(:environment_config_service).and_return(@environment_config_service)
      @environment_config_service.stub(:getEnvironmentConfig).with(@environment_name).and_return(@environment_config)
    end

    describe :for_admins do
      it 'should allow patching environments' do
        login_as_admin
        result = HttpLocalizedOperationResult.new
        pipelines_to_add = ['foo']
        pipelines_to_remove = ['bar']
        agents_to_add = ['agent1']
        agents_to_remove = ['agent2']
        @environment_config_service.should_receive(:patchEnvironment).with(@environment_config, pipelines_to_add, pipelines_to_remove, agents_to_add, agents_to_remove, anything, result).and_return(result)

        patch_with_api_header :patch, name: @environment_name, :pipelines => {add: pipelines_to_add, remove: pipelines_to_remove}, :agents => {add: agents_to_add, remove: agents_to_remove}
        expect(response.status).to eq(200)
        expect(actual_response).to eq(expected_response(@environment_config, ApiV1::Config::EnvironmentConfigRepresenter))
      end

      it 'should render error when it fails to patch environment' do
        login_as_admin
        result = HttpLocalizedOperationResult.new
        pipelines_to_add = ['foo']
        pipelines_to_remove = ['bar']
        agents_to_add = ['agent1']
        agents_to_remove = ['agent2']
        @environment_config_service.stub(:patchEnvironment).with(@environment_config, pipelines_to_add, pipelines_to_remove, agents_to_add, agents_to_remove, anything, result) do |environment_config, pipelines_to_add, pipelines_to_remove, agents_to_add, agents_to_remove, user, result|
          result.badRequest(LocalizedMessage.string("PIPELINES_WITH_NAMES_NOT_FOUND", pipelines_to_add))
        end

        patch_with_api_header :patch, name: @environment_name, :pipelines => {add: pipelines_to_add, remove: pipelines_to_remove}, :agents => {add: agents_to_add, remove: agents_to_remove}
        expect(response).to have_api_message_response(400, 'Pipelines(s) with name(s) [foo] not found.')
      end

      it 'should render 404 when a environment does not exist' do
        login_as_admin

        @environment_name = SecureRandom.hex
        @environment_config_service.stub(:getEnvironmentConfig).and_raise(com.thoughtworks.go.config.exceptions.NoSuchEnvironmentException.new(CaseInsensitiveString.new('foo-env')))
        patch_with_api_header :patch, name: @environment_name
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe :security do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:patch, :patch, name: @environment_name)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:patch, :patch, name: @environment_name).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:patch, :patch, name: @environment_name).with(401, 'You are not authorized to perform this action.')
      end
    end

    describe :route do
      describe :with_header do
        before :each do
          Rack::MockRequest::DEFAULT_ENV["HTTP_ACCEPT"] = "application/vnd.go.cd.v1+json"
        end
        after :each do
          Rack::MockRequest::DEFAULT_ENV = {}
        end
        it 'should route to patch action of environments controller for alphanumeric environment name' do
          expect(:patch => 'api/admin/environments/foo123').to route_to(action: 'patch', controller: 'api_v1/admin/environments', name: 'foo123')
        end

        it 'should route to patch action of environments controller for environment name with dots' do
          expect(:patch => 'api/admin/environments/foo.123').to route_to(action: 'patch', controller: 'api_v1/admin/environments', name: 'foo.123')
        end

        it 'should route to patch action of environments controller for environment name with hyphen' do
          expect(:patch => 'api/admin/environments/foo-123').to route_to(action: 'patch', controller: 'api_v1/admin/environments', name: 'foo-123')
        end

        it 'should route to patch action of environments controller for environment name with underscore' do
          expect(:patch => 'api/admin/environments/foo_123').to route_to(action: 'patch', controller: 'api_v1/admin/environments', name: 'foo_123')
        end

        it 'should route to patch action of environments controller for capitalized environment name' do
          expect(:patch => 'api/admin/environments/FOO').to route_to(action: 'patch', controller: 'api_v1/admin/environments', name: 'FOO')
        end
      end
      describe :without_header do
        it 'should not route to put action of environments controller without header' do
          expect(:patch => 'api/admin/environments/foo').to_not route_to(action: 'put', controller: 'api_v1/admin/environments')
          expect(:patch => 'api/admin/environments/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/environments/foo')
        end
      end
    end
  end

  describe :destroy do
    before(:each) do
      @environment_name = 'foo-environment'
      @environment_config = BasicEnvironmentConfig.new(CaseInsensitiveString.new(@environment_name))
      @environment_config_service = double('environment-config-service')
      controller.stub(:environment_config_service).and_return(@environment_config_service)
      @environment_config_service.stub(:getEnvironmentConfig).with(@environment_name).and_return(@environment_config)
    end

    describe :for_admins do
      it 'should allow deleting environments' do
        login_as_admin

        @environment_config_service.should_receive(:deleteEnvironment).with(@environment_config, an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult)) do |envConfig, user, result|
          result.setMessage(LocalizedMessage.string('ENVIRONMENT_DELETE_SUCCESSFUL', @environment_config.name))
        end
        controller.request.env['HTTP_IF_MATCH'] = "\"#{Digest::MD5.hexdigest('latest-etag')}\""

        delete_with_api_header :destroy, name: @environment_name
        expect(response).to have_api_message_response(200, "Environment 'foo-environment' was deleted successfully.")
      end

      it 'should render 404 when a environment does not exist' do
        login_as_admin

        @environment_name = SecureRandom.hex
        @environment_config_service.stub(:getEnvironmentConfig).and_raise(com.thoughtworks.go.config.exceptions.NoSuchEnvironmentException.new(CaseInsensitiveString.new('foo-env')))
        delete_with_api_header :destroy, name: @environment_name
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe :security do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:delete, :destroy, name: @environment_name)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:delete, :destroy, name: @environment_name).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:delete, :destroy, name: @environment_name).with(401, 'You are not authorized to perform this action.')
      end
    end

    describe :route do
      describe :with_header do
        before :each do
          Rack::MockRequest::DEFAULT_ENV["HTTP_ACCEPT"] = "application/vnd.go.cd.v1+json"
        end
        after :each do
          Rack::MockRequest::DEFAULT_ENV = {}
        end
        it 'should route to destroy action of environments controller for alphanumeric environment name' do
          expect(:delete => 'api/admin/environments/foo123').to route_to(action: 'destroy', controller: 'api_v1/admin/environments', name: 'foo123')
        end

        it 'should route to destroy action of environments controller for environment name with dots' do
          expect(:delete => 'api/admin/environments/foo.123').to route_to(action: 'destroy', controller: 'api_v1/admin/environments', name: 'foo.123')
        end

        it 'should route to destroy action of environments controller for environment name with hyphen' do
          expect(:delete => 'api/admin/environments/foo-123').to route_to(action: 'destroy', controller: 'api_v1/admin/environments', name: 'foo-123')
        end

        it 'should route to destroy action of environments controller for environment name with underscore' do
          expect(:delete => 'api/admin/environments/foo_123').to route_to(action: 'destroy', controller: 'api_v1/admin/environments', name: 'foo_123')
        end

        it 'should route to destroy action of environments controller for capitalized environment name' do
          expect(:delete => 'api/admin/environments/FOO').to route_to(action: 'destroy', controller: 'api_v1/admin/environments', name: 'FOO')
        end
      end
      describe :without_header do
        it 'should not route to destroy action of environments controller without header' do
          expect(:delete => 'api/admin/environments/foo').to_not route_to(action: 'destroy', controller: 'api_v1/admin/environments')
          expect(:delete => 'api/admin/environments/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/environments/foo')
        end
      end
    end
  end

  describe :create do
    before(:each) do
      @environment_name = 'foo-environment'
      @environment_config = BasicEnvironmentConfig.new(CaseInsensitiveString.new(@environment_name))
      @environment_config_service = double('environment-config-service')
      controller.stub(:environment_config_service).and_return(@environment_config_service)
      @environment_config_service.stub(:getEnvironmentConfig).with(@environment_name).and_return(@environment_config)
    end


    describe :for_admins do
      it 'should render 200 created when environment is created' do
        login_as_admin

        @environment_config_service.should_receive(:createEnvironment)

        post_with_api_header :create, :environment => {name: @environment_name, pipelines: [], agents: [], environment_variables: []}
        expect(response.status).to be(200)
        expect(actual_response).to eq(expected_response(@environment_config, ApiV1::Config::EnvironmentConfigRepresenter))
      end

      it 'should render the error occurred while creating an environment' do
        login_as_admin

        @environment_config_service.should_receive(:createEnvironment).with(@environment_config, an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult)) do |env, user, result|
          result.conflict(LocalizedMessage.string("CANNOT_ADD_ENV_ALREADY_EXISTS", env.name));
        end

        post_with_api_header :create, :environment => {name: @environment_name, pipelines: [], agents: [], environment_variables: []}
        expect(response).to have_api_message_response(409, 'Failed to add environment. Environment \'foo-environment\' already exists.')
      end
    end

    describe :security do
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
    end

    describe :route do
      describe :with_header do
        before :each do
          Rack::MockRequest::DEFAULT_ENV["HTTP_ACCEPT"] = "application/vnd.go.cd.v1+json"
        end
        after :each do
          Rack::MockRequest::DEFAULT_ENV = {}
        end
        it 'should route to create action of environments controller' do
          expect(:post => 'api/admin/environments/').to route_to(action: 'create', controller: 'api_v1/admin/environments')
        end
      end
      describe :without_header do
        it 'should not route to create action of environments controller without header' do
          expect(:post => 'api/admin/environments').to_not route_to(action: 'create', controller: 'api_v1/admin/environments')
          expect(:post => 'api/admin/environments').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/environments')
        end
      end
    end
  end
end
