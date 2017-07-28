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

describe ApiV1::Admin::ConfigReposController do
  include ApiHeaderSetupTeardown, ApiV1::ApiVersionHelper

  before :each do
    @material_config = GitMaterialConfig.new('git://foo', 'master')
    @config_repo_id = SecureRandom.hex
    @plugin_id = 'config-repo-json-plugin'
    @config_repo = ConfigRepoConfig.new(@material_config, @plugin_id, @config_repo_id)
    @md5 = 'some-digest'

    @entity_hashing_service = double('entity-hashing-service')
    controller.stub(:entity_hashing_service).and_return(@entity_hashing_service)

    @config_repo_service = double('config-repo-service')
    controller.stub(:config_repo_service).and_return(@config_repo_service)

    @entity_hashing_service.stub(:md5ForEntity).and_return(@md5)
  end

  describe :show do
    describe :for_admins do
      before(:each) do
        enable_security
        login_as_admin
      end

      it 'should render the package repo' do
        @config_repo_service.stub(:getConfigRepo).with(@config_repo_id).and_return(@config_repo)
        get_with_api_header :show, id: @config_repo_id
        expect(response.status).to eq(200)
        expect(actual_response).to eq(expected_response(@config_repo, ApiV1::Config::ConfigRepoRepresenter))
      end

      it 'should render 404 when a config repo with specified id does not exist' do
        @config_repo_service.stub(:getConfigRepo).with('invalid-package-id').and_return(nil)
        get_with_api_header :show, id: 'invalid-package-id'
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe :security do
      before :each do
        controller.stub(:load_config_repo).and_return(nil)
      end

      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:get, :show, id: @config_repo_id)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:get, :show, id: @config_repo_id).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:get, :show, id: @config_repo_id).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:get, :show)
      end
    end

    describe :route do
      describe :with_header do
        it 'should route to show action of config repo controller for specified repo id' do
          expect(:get => 'api/admin/config_repos/foo').to route_to(action: 'show', controller: 'api_v1/admin/config_repos', id: 'foo')
        end
        it 'should route to show action of config repo controller for config id with dots' do
          expect(:get => 'api/admin/config_repos/foo.bar').to route_to(action: 'show', controller: 'api_v1/admin/config_repos', id: 'foo.bar')
        end
      end

      describe :without_header do
        before :each do
          teardown_header
        end
        it 'should not route to show action of config repo controller without header' do
          expect(:get => 'api/admin/config_repos/foo').to_not route_to(action: 'show', controller: 'api_v1/admin/config_repos')
          expect(:get => 'api/admin/config_repos/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/config_repos/foo')
        end
      end
    end
  end

  describe :index do
    describe :for_admins do
      before :each do
        enable_security
        login_as_admin
      end

      it 'should render a list of config_repos, for admins' do
        repos = Arrays.asList(@config_repo)
        @config_repo_service.should_receive(:getConfigRepos).and_return(repos)

        get_with_api_header :index
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(repos, ApiV1::Config::ConfigReposRepresenter))
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

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:get, :index)
      end
    end

    describe :route do
      describe :with_header do
        it 'should route to index action of config repos controller' do
          expect(:get => 'api/admin/config_repos').to route_to(action: 'index', controller: 'api_v1/admin/config_repos')
        end
      end

      describe :without_header do
        before :each do
          teardown_header
        end

        it 'should not route to index action of config repos controller without header' do
          expect(:get => 'api/admin/config_repos').to_not route_to(action: 'index', controller: 'api_v1/admin/config_repos')
          expect(:get => 'api/admin/config_repos').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/config_repos')
        end
      end
    end
  end

  describe :destroy do
    describe :for_admins do
      before(:each) do
        enable_security
        login_as_admin
      end

      it 'should allow deleting config repo' do
        @config_repo_service.stub(:getConfigRepo).with(@config_repo_id).and_return(@config_repo)
        @config_repo_service.should_receive(:deleteConfigRepo).with(@config_repo_id, an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult)) do |pkg, user, result|
          result.setMessage(LocalizedMessage.string('RESOURCE_DELETE_SUCCESSFUL', 'config repo', @config_repo_id))
        end

        delete_with_api_header :destroy, id: @config_repo_id
        expect(response).to have_api_message_response(200, "The config repo '#{@config_repo_id}' was deleted successfully.")
      end

      it 'should render 404 when config repo does not exist' do
        @config_repo_service.stub(:getConfigRepo).with('invalid-package-id').and_return(nil)
        delete_with_api_header :destroy, id: 'invalid-package-id'
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe :security do
      before :each do
        @config_repo_service.stub(:getConfigRepo).and_return(@config_repo)
      end

      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:delete, :destroy, id: @config_repo_id)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:delete, :destroy, id: @config_repo_id).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:delete, :destroy, id: @config_repo_id).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:delete, :destroy, id: @config_repo_id)
      end

    end

    describe :route do
      describe :with_header do
        it 'should route to destroy action of config repo controller for specified repo id' do
          expect(:delete => 'api/admin/config_repos/foo').to route_to(action: 'destroy', controller: 'api_v1/admin/config_repos', id: 'foo')
        end

        it 'should route to delete action of config repo controller for id with dots' do
          expect(:delete => 'api/admin/config_repos/foo.bar').to route_to(action: 'destroy', controller: 'api_v1/admin/config_repos', id: 'foo.bar')
        end
      end
      describe :without_header do
        before :each do
          teardown_header
        end
        it 'should not route to destroy action of config repos controller without header' do
          expect(:delete => 'api/admin/config_repos/foo').to_not route_to(action: 'destroy', controller: 'api_v1/admin/config_repos')
          expect(:delete => 'api/admin/config_repos/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/config_repos/foo')
        end
      end
    end
  end

  describe :create do
    describe :for_admins do
      before(:each) do
        enable_security
        login_as_admin
      end

      it 'should render 200 created when config repo is created' do
        @config_repo_service.stub(:getConfigRepo).and_return(@config_repo)
        @config_repo_service.should_receive(:createConfigRepo).with(an_instance_of(ConfigRepoConfig), an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult))
        post_with_api_header :create, :config_repo => get_config_repo_json(@config_repo_id)

        expect(response.status).to be(200)
        expect(actual_response).to eq(expected_response(@config_repo, ApiV1::Config::ConfigRepoRepresenter))
      end
      
      it 'should render the error occurred while creating a package' do
        @config_repo_service.should_receive(:createConfigRepo).with(an_instance_of(ConfigRepoConfig), an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult)) do |pkg, user, result|
          result.unprocessableEntity(LocalizedMessage::string("SAVE_FAILED_WITH_REASON", "Validation failed"))
        end

        post_with_api_header :create, :config_repo => get_config_repo_json(@config_repo_id)
        expect(response).to have_api_message_response(422, "Save failed. Validation failed")
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

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:post, :create)
      end
    end

    describe :route do
      describe :with_header do
        it 'should route to create action of config repo controller' do
          expect(:post => 'api/admin/config_repos').to route_to(action: 'create', controller: 'api_v1/admin/config_repos')
        end
      end
      describe :without_header do
        before :each do
          teardown_header
        end
        it 'should not route to create action of config repo controller without header' do
          expect(:post => 'api/admin/config_repos').to_not route_to(action: 'create', controller: 'api_v1/admin/config_repos')
          expect(:post => 'api/admin/config_repos').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/config_repos')
        end
      end
    end
  end

  describe :update do
    describe :for_admins do
      before(:each) do
        enable_security
        login_as_admin
      end

      it 'should allow updating config repo' do
        @config_repo_service.stub(:getConfigRepo).with(@config_repo_id).and_return(@config_repo)
        result = HttpLocalizedOperationResult.new
        @config_repo_service.should_receive(:updateConfigRepo).with(@config_repo_id, an_instance_of(ConfigRepoConfig), @md5, an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult)).and_return(result)
        hash = get_config_repo_json(@config_repo_id)

        controller.request.env['HTTP_IF_MATCH'] = "\"#{Digest::MD5.hexdigest(@md5)}\""

        put_with_api_header :update, id: @config_repo_id, :config_repo => hash
        expect(response.status).to eq(200)
        expect(actual_response).to eq(expected_response(@config_repo, ApiV1::Config::ConfigRepoRepresenter))
      end

      it 'should not update package config if etag passed does not match the one on server' do
        @config_repo_service.stub(:getConfigRepo).with(@config_repo_id).and_return(@config_repo_id)
        controller.request.env['HTTP_IF_MATCH'] = 'old-etag'
        hash = get_config_repo_json(@config_repo_id)

        put_with_api_header :update, id: @config_repo_id, :config_repo => hash

        expect(response.status).to eq(412)
        expect(actual_response).to eq({:message => "Someone has modified the configuration for config repo '#{@config_repo_id}'. Please update your copy of the config with the changes."})
      end

      it 'should not update package config if no etag is passed' do
        @config_repo_service.stub(:getConfigRepo).with(@config_repo_id).and_return(@config_repo_id)
        hash = get_config_repo_json(@config_repo_id)

        put_with_api_header :update, id: @config_repo_id, :config_repo => hash

        expect(response.status).to eq(412)
        expect(actual_response).to eq({:message => "Someone has modified the configuration for config repo '#{@config_repo_id}'. Please update your copy of the config with the changes."})
      end

      it 'should render 404 when a package does not exist' do
        @config_repo_service.stub(:getConfigRepo).with('non-existent-package-id').and_return(nil)
        put_with_api_header :update, id: 'non-existent-package-id'
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
      
    end

    describe :security do
      before(:each) do
        @config_repo_service.stub(:getConfigRepo).and_return(@config_repo_id)
        controller.stub(:check_for_stale_request).and_return(nil)
      end

      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:put, :update, id: @config_repo_id)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:put, :update, id: @config_repo_id).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:put, :update, id: @config_repo_id).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:put, :update, id: @config_repo_id)
      end

    end

    describe :route do
      describe :with_header do
        it 'should route to update action of config repo controller for specified package id' do
          expect(:put => 'api/admin/config_repos/foo123').to route_to(action: 'update', controller: 'api_v1/admin/config_repos', id: 'foo123')
        end
        it 'should route to update action of config repo controller for package_id with dots' do
          expect(:put => 'api/admin/config_repos/foo.bar').to route_to(action: 'update', controller: 'api_v1/admin/config_repos', id: 'foo.bar')
        end
      end
      describe :without_header do
        before :each do
          teardown_header
        end
        it 'should not route to update action of packages controller without header' do
          expect(:put => 'api/admin/config_repos/foo').to_not route_to(put: 'update', controller: 'api_v1/admin/config_repos')
          expect(:put => 'api/admin/config_repos/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/config_repos/foo')
        end
      end
    end
  end

  def get_config_repo_json(repo_id)
    {
      :id => repo_id,
      :plugin => 'config-repo-json-plugin',
      :material => {
        :type => 'git',
        :attributes => {
          :url => 'git://foo',
          :destination => nil,
          :filter => nil,
          :invert_filter => false,
          :name => nil,
          :auto_update => true,
          :branch => 'master',
          :submodule_folder => nil,
          :shallow_clone => false
        }
      },
      :configuration => []
    }
  end
end
