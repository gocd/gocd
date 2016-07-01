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
  end

  describe :update do

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
        @environment_config_service.should_receive(:updateEnvironment).with(@environment_name, anything(), anything(), anything()).and_return(result)
        hash = {name: @environment_name, pipelines: [], agents: [], environment_variables: []}

        controller.request.env['HTTP_IF_MATCH'] = '"some-digest"'
        controller.should_receive(:get_etag_for_environment).twice.and_return('some-digest')

        patch_with_api_header :update,name: @environment_name, :environment => hash
        expect(response.status).to eq(200)
        expect(actual_response).to eq(expected_response(@environment_config, ApiV1::Config::EnvironmentConfigRepresenter))
      end

      it 'should not update environment config if etag passed does not match the one on server' do
        login_as_admin
        controller.request.env['HTTP_IF_MATCH'] = 'old-etag'

        patch_with_api_header :update,name: @environment_name, :environment => { name: @environment_name, pipelines: [], agents: [], environment_variables: []}

        expect(response.status).to eq(412)
        expect(actual_response).to eq({ :message => "Someone has modified the configuration for environment 'foo-environment'. Please update your copy of the config with the changes." })
      end

      it 'should not update environment config if no etag is passed' do
        login_as_admin
        patch_with_api_header :update,name: @environment_name, :environment => { name: @environment_name, pipelines: [], agents: [], environment_variables: []}

        expect(response.status).to eq(412)
        expect(response).to have_api_message_response(412, "Someone has modified the configuration for environment 'foo-environment'. Please update your copy of the config with the changes.")
      end

      it 'should render 404 when a environment does not exist' do
        login_as_admin

        @environment_config_service.stub(:getEnvironmentConfig).and_raise(com.thoughtworks.go.config.exceptions.NoSuchEnvironmentException.new(CaseInsensitiveString.new('foo-env')))
        patch_with_api_header :update, name: @environment_name
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe :security do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:patch, :update)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:patch, :update, name: @environment_name).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:patch, :update, name: @environment_name).with(401, 'You are not authorized to perform this action.')
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

        @environment_config_service.should_receive(:deleteEnvironment).with(@environment_name,an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult)) do |name, user, result|
          result.setMessage(LocalizedMessage.string('ENVIRONMENT_DELETE_SUCCESSFUL', [name].to_java(java.lang.String)))
        end
        controller.request.env['HTTP_IF_MATCH'] = "\"#{Digest::MD5.hexdigest('latest-etag')}\""

        delete_with_api_header :destroy, name: @environment_name
        expect(response).to have_api_message_response(200, "Environment 'foo-environment' was deleted successfully.")
      end

      it 'should render 404 when a environment does not exist' do
        login_as_admin

        @environment_config_service.stub(:getEnvironmentConfig).and_raise(com.thoughtworks.go.config.exceptions.NoSuchEnvironmentException.new(CaseInsensitiveString.new('foo-env')))
        delete_with_api_header :destroy, name: @environment_name
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe :security do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:delete, :destroy)
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

        post_with_api_header :create, :environment => { name: @environment_name, pipelines: [], agents: [], environment_variables: []}
        expect(response.status).to be(200)
        expect(actual_response).to eq(expected_response(@environment_config, ApiV1::Config::EnvironmentConfigRepresenter))
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
  end
end
