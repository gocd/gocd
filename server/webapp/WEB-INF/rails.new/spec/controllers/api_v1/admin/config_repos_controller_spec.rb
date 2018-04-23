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

describe ApiV1::Admin::ConfigReposController do

  include ApiV1::ApiVersionHelper

  before :each do
    @material_config = GitMaterialConfig.new('git://foo', 'master')
    @config_repo_id = SecureRandom.hex
    @plugin_id = 'config-repo-json-plugin'
    @config_repo = ConfigRepoConfig.new(@material_config, @plugin_id, @config_repo_id)
    @md5 = 'some-digest'

    @entity_hashing_service = double('entity-hashing-service')
    allow(controller).to receive(:entity_hashing_service).and_return(@entity_hashing_service)

    @config_repo_service = double('config-repo-service')
    allow(controller).to receive(:config_repo_service).and_return(@config_repo_service)

    allow(@entity_hashing_service).to receive(:md5ForEntity).and_return(@md5)
  end

  describe "show" do
    describe "for_admins" do
      before(:each) do
        enable_security
        login_as_admin
      end

      it 'should render the package repo' do
        allow(@config_repo_service).to receive(:getConfigRepo).with(@config_repo_id).and_return(@config_repo)
        get_with_api_header :show, params: { id: @config_repo_id }
        expect(response.status).to eq(200)
        expect(actual_response).to eq(expected_response(@config_repo, ApiV1::Config::ConfigRepoRepresenter))
      end

      it 'should render 404 when a config repo with specified id does not exist' do
        allow(@config_repo_service).to receive(:getConfigRepo).with('invalid-package-id').and_return(nil)
        get_with_api_header :show, params: { id: 'invalid-package-id' }
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe "security" do
      before :each do
        allow(controller).to receive(:load_config_repo).and_return(nil)
      end

      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:get, :show, params: { id: @config_repo_id })
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:get, :show, params: { id: @config_repo_id }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:get, :show, params: { id: @config_repo_id }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:get, :show)
      end
    end
  end

  describe "index" do
    describe "for_admins" do
      before :each do
        enable_security
        login_as_admin
      end

      it 'should render a list of config_repos, for admins' do
        repos = Arrays.asList(@config_repo)
        expect(@config_repo_service).to receive(:getConfigRepos).and_return(repos)

        get_with_api_header :index
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(repos, ApiV1::Config::ConfigReposRepresenter))
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
    end
  end

  describe "destroy" do
    describe "for_admins" do
      before(:each) do
        enable_security
        login_as_admin
      end

      it 'should allow deleting config repo' do
        allow(@config_repo_service).to receive(:getConfigRepo).with(@config_repo_id).and_return(@config_repo)
        expect(@config_repo_service).to receive(:deleteConfigRepo).with(@config_repo_id, an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult)) do |pkg, user, result|
          result.setMessage(LocalizedMessage.string('RESOURCE_DELETE_SUCCESSFUL', 'config repo', @config_repo_id))
        end

        delete_with_api_header :destroy, params: { id: @config_repo_id }
        expect(response).to have_api_message_response(200, "The config repo '#{@config_repo_id}' was deleted successfully.")
      end

      it 'should render 404 when config repo does not exist' do
        allow(@config_repo_service).to receive(:getConfigRepo).with('invalid-package-id').and_return(nil)
        delete_with_api_header :destroy, params: { id: 'invalid-package-id' }
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe "security" do
      before :each do
        allow(@config_repo_service).to receive(:getConfigRepo).and_return(@config_repo)
      end

      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:delete, :destroy, params: { id: @config_repo_id })
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:delete, :destroy, params: { id: @config_repo_id }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:delete, :destroy, params: { id: @config_repo_id }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:delete, :destroy, params: { id: @config_repo_id })
      end

    end
  end

  describe "create" do
    describe "for_admins" do
      before(:each) do
        enable_security
        login_as_admin
      end

      it 'should render 200 created when config repo is created' do
        allow(@config_repo_service).to receive(:getConfigRepo).and_return(@config_repo)
        expect(@config_repo_service).to receive(:createConfigRepo).with(an_instance_of(ConfigRepoConfig), an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult))
        post_with_api_header :create, params: { :config_repo => get_config_repo_json(@config_repo_id) }

        expect(response.status).to be(200)
        expect(actual_response).to eq(expected_response(@config_repo, ApiV1::Config::ConfigRepoRepresenter))
      end

      it 'should render the error occurred while creating a package' do
        expect(@config_repo_service).to receive(:createConfigRepo).with(an_instance_of(ConfigRepoConfig), an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult)) do |pkg, user, result|
          result.unprocessableEntity(LocalizedMessage::string("SAVE_FAILED_WITH_REASON", "Validation failed"))
        end

        post_with_api_header :create, params: { :config_repo => get_config_repo_json(@config_repo_id) }
        expect(response).to have_api_message_response(422, "Save failed. Validation failed")
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
        expect(controller).to allow_action(:post, :create)
      end
    end
  end

  describe "update" do
    describe "for_admins" do
      before(:each) do
        enable_security
        login_as_admin
      end

      it 'should allow updating config repo' do
        allow(@config_repo_service).to receive(:getConfigRepo).with(@config_repo_id).and_return(@config_repo)
        result = HttpLocalizedOperationResult.new
        expect(@config_repo_service).to receive(:updateConfigRepo).with(@config_repo_id, an_instance_of(ConfigRepoConfig), @md5, an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult)).and_return(result)
        hash = get_config_repo_json(@config_repo_id)

        controller.request.env['HTTP_IF_MATCH'] = "\"#{Digest::MD5.hexdigest(@md5)}\""

        put_with_api_header :update, params: { id: @config_repo_id, :config_repo => hash }
        expect(response.status).to eq(200)
        expect(actual_response).to eq(expected_response(@config_repo, ApiV1::Config::ConfigRepoRepresenter))
      end

      it 'should not update package config if etag passed does not match the one on server' do
        allow(@config_repo_service).to receive(:getConfigRepo).with(@config_repo_id).and_return(@config_repo_id)
        controller.request.env['HTTP_IF_MATCH'] = 'old-etag'
        hash = get_config_repo_json(@config_repo_id)

        put_with_api_header :update, params: { id: @config_repo_id, :config_repo => hash }

        expect(response.status).to eq(412)
        expect(actual_response).to eq({:message => "Someone has modified the configuration for config repo '#{@config_repo_id}'. Please update your copy of the config with the changes."})
      end

      it 'should not update package config if no etag is passed' do
        allow(@config_repo_service).to receive(:getConfigRepo).with(@config_repo_id).and_return(@config_repo_id)
        hash = get_config_repo_json(@config_repo_id)

        put_with_api_header :update, params: { id: @config_repo_id, :config_repo => hash }

        expect(response.status).to eq(412)
        expect(actual_response).to eq({:message => "Someone has modified the configuration for config repo '#{@config_repo_id}'. Please update your copy of the config with the changes."})
      end

      it 'should render 404 when a package does not exist' do
        allow(@config_repo_service).to receive(:getConfigRepo).with('non-existent-package-id').and_return(nil)
        put_with_api_header :update, params: { id: 'non-existent-package-id' }
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end

    end

    describe "security" do
      before(:each) do
        allow(@config_repo_service).to receive(:getConfigRepo).and_return(@config_repo_id)
        allow(controller).to receive(:check_for_stale_request).and_return(nil)
      end

      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:put, :update, params: { id: @config_repo_id })
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:put, :update, params: { id: @config_repo_id }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:put, :update, params: { id: @config_repo_id }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:put, :update, params: { id: @config_repo_id })
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
