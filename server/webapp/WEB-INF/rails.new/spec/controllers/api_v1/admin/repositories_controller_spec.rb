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

describe ApiV1::Admin::RepositoriesController do

  include ApiV1::ApiVersionHelper

  before :each do
    @repo_id = 'npm'
    @package_repository_service = double('package-repository-service')
    allow(controller).to receive(:package_repository_service).and_return(@package_repository_service)
  end

  describe "index" do
    describe "for_admins" do
      it 'should render a list of package repositories, for admins' do
        login_as_admin
        pkg_repo = PackageRepository.new()
        pkg_repo.setId('npm')
        all_repos = PackageRepositories.new(pkg_repo)

        expect(@package_repository_service).to receive(:getPackageRepositories).and_return(all_repos)

        get_with_api_header :index
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(all_repos, ApiV1::Config::PackageRepositoriesRepresenter))
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

      it 'should allow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to allow_action(:get, :index)
      end
    end
  end

  describe "show" do
    describe "for_admins" do
      before :each do
        login_as_admin
        @package_repo = PackageRepository.new()
        @package_repo.setId(@repo_id)
      end

      it 'should render the package repository' do
        allow(@package_repository_service).to receive(:getPackageRepository).with(@repo_id).and_return(@package_repo)
        get_with_api_header :show, params: { repo_id: @repo_id }
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(@package_repo, ApiV1::Config::PackageRepositoryRepresenter))
      end

      it 'should render 404 when a package repository does not exist' do
        allow(@package_repository_service).to receive(:getPackageRepository).and_return(nil)

        get_with_api_header :show, params: { repo_id: 'invalid-package-repo-id' }
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe "security" do
      before :each do
        allow(controller).to receive(:load_package_repository).and_return(nil)
      end
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:get, :show, params: { repo_id: @repo_id })
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:get, :show, params: { repo_id: @repo_id }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:get, :show, params: { repo_id: @repo_id }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:get, :show)
      end

      it 'should allow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to allow_action(:get, :show)
      end
    end
  end

  describe "destroy" do
    describe "for_admins" do
      before :each do
        login_as_admin
        @package_repo = PackageRepository.new()
        @package_repo.setId(@repo_id)
      end

      it 'should allow deleting package repositories' do
        allow(@package_repository_service).to receive(:getPackageRepository).with(@repo_id).and_return(@package_repo)
        expect(@package_repository_service).to receive(:deleteRepository).with(an_instance_of(Username), @package_repo, an_instance_of(HttpLocalizedOperationResult)) do |user, repo, result|
          result.setMessage(LocalizedMessage.string("RESOURCE_DELETE_SUCCESSFUL", 'package repository', @package_repo.getId()))
        end

        delete_with_api_header :destroy, params: { repo_id: @repo_id }
        expect(response).to have_api_message_response(200, "The package repository 'npm' was deleted successfully.")
      end

      it 'should render 404 when a package repository does not exist' do
        allow(@package_repository_service).to receive(:getPackageRepository).with(@repo_id).and_return(nil)

        delete_with_api_header :destroy, params: { repo_id: @repo_id }
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe "security" do
      before :each do
        allow(controller).to receive(:load_package_repository).and_return(nil)
      end

      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:delete, :destroy, params: { repo_id: @repo_id })
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:delete, :destroy, params: { repo_id: @repo_id }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:delete, :destroy, params: { repo_id: @repo_id }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:delete, :destroy)
      end

      it 'should allow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to allow_action(:delete, :destroy)
      end
    end
  end

  describe "create" do
    describe "for_admins" do
      before :each do
        login_as_admin
        @package_repo = PackageRepository.new(@repo_id, @repo_id, PluginConfiguration.new('npm', '1'), Configuration.new())
      end
      it 'should render 200 created when package repository is created' do
        expect(@package_repository_service).to receive(:createPackageRepository).with(an_instance_of(PackageRepository), an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult))
        allow(@package_repository_service).to receive(:getPackageRepository).and_return(@package_repo)

        post_with_api_header :create, params: { :repository => {repo_id: @repo_id, name: @repo_id, plugin_metadata: {id: 'npm', version: '1'}, configuration: []} }
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(@package_repo, ApiV1::Config::PackageRepositoryRepresenter))
      end

      it 'should generate id if id is not provided by user' do
        expect(@package_repository_service).to receive(:createPackageRepository).with(an_instance_of(PackageRepository), an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult))
        allow(@package_repository_service).to receive(:getPackageRepository).and_return(@package_repo)

        post_with_api_header :create, params: { :repository => {name: @repo_id, plugin_metadata: {id: 'npm', version: '1'}, configuration: []} }
        expect(actual_response).to have_key(:repo_id)
      end

      it 'should render the error occurred while creating a package repository' do
        expect(@package_repository_service).to receive(:createPackageRepository).with(an_instance_of(PackageRepository), an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult)) do |repo, user, result|
          result.unprocessableEntity(LocalizedMessage.string("PLUGIN_ID_INVALID", 'invalid_id'));
        end

        post_with_api_header :create, params: { :repository => {name: @repo_id, type: 'invalid_id', configuration: []} }
        expect(response).to have_api_message_response(422, 'Invalid plugin id')
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

      it 'should allow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to allow_action(:post, :create)
      end
    end
  end

  describe "update" do
    describe "for_admins" do
      before :each do
        login_as_admin
        @entity_hashing_service = double('entity-hashing-service')
        allow(controller).to receive(:entity_hashing_service).and_return(@entity_hashing_service)
        @package_repo = PackageRepository.new(@repo_id, @repo_id, PluginConfiguration.new('some-id', '1'), Configuration.new())
        @md5 = 'some-digest'
      end
      it 'should allow updating package repository' do
        result = HttpLocalizedOperationResult.new
        allow(@package_repository_service).to receive(:getPackageRepository).exactly(2).times.and_return(@package_repo)
        allow(controller).to receive(:check_for_stale_request).and_return(nil)
        allow(@entity_hashing_service).to receive(:md5ForEntity).and_return('md5')

        expect(@package_repository_service).to receive(:updatePackageRepository).with(an_instance_of(PackageRepository), anything, anything, result, anything)
        hash = {repo_id: @repo_id, name: @repo_id, plugin_metadata: {id: 'some-id', version: '1'}, configuration: []}

        put_with_api_header :update, params: { :repo_id => @repo_id, :repository => hash }
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(@package_repo, ApiV1::Config::PackageRepositoryRepresenter))
      end

      it 'should not update package repository if etag passed does not match the one on server' do
        allow(controller).to receive(:load_package_repository).and_return(@package_repo)

        controller.request.env['HTTP_IF_MATCH'] = 'old-etag'
        allow(@entity_hashing_service).to receive(:md5ForEntity).and_return(@md5)

        hash = {repo_id: @repo_id, name: "foo", plugin_metadata: {id: 'npm', version: '1'}, configuration: [{key: 'REPO_URL', value: 'https://foo.bar'}]}
        put_with_api_header :update, params: { :repo_id => @repo_id, :repository => hash }

        expect(response).to have_api_message_response(412, "Someone has modified the configuration for Package Repository 'npm'. Please update your copy of the config with the changes.")
      end

      it 'should not update package repository if no etag is passed' do
        allow(controller).to receive(:load_package_repository).and_return(@package_repo)
        allow(@entity_hashing_service).to receive(:md5ForEntity).and_return(@md5)

        hash = {repo_id: @repo_id, name: "foo", plugin_metadata: {id: 'npm', version: '1'}, configuration: [{key: 'REPO_URL', value: 'https://foo.bar'}]}
        put_with_api_header :update, params: { :repo_id => @repo_id, :repository => hash }

        expect(response).to have_api_message_response(412, "Someone has modified the configuration for Package Repository 'npm'. Please update your copy of the config with the changes.")
      end

      it 'should render 404 when a package repository does not exist' do
        allow(controller).to receive(:check_for_stale_request).and_return(nil)
        allow(@package_repository_service).to receive(:getPackageRepository).with('non-existing-repo-id').and_return(nil)
        hash = {repo_id: 'non-existing-repo-id', name: "foo", plugin_metadata: {id: 'npm', version: '1'}, configuration: [{key: 'REPO_URL', value: 'https://foo.bar'}]}

        put_with_api_header :update, params: { :repo_id => 'non-existing-repo-id', :repository => hash }

        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe "security" do
      before(:each) do
        allow(controller).to receive(:load_package_repository).and_return(nil)
        allow(controller).to receive(:check_for_stale_request).and_return(nil)
      end

      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:put, :update, params: { repo_id: @repo_id })
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:put, :update, params: { repo_id: @repo_id }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:put, :update, params: { repo_id: @repo_id }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:put, :update)
      end

      it 'should allow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to allow_action(:put, :update)
      end
    end
  end
end
