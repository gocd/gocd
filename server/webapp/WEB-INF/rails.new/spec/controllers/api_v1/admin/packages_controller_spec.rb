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

describe ApiV1::Admin::PackagesController do

  include ApiV1::ApiVersionHelper

  before :each do
    @package_id = SecureRandom.hex
    @package_name = "prettyjson"
    config_property = ConfigurationProperty.new(ConfigurationKey.new("PACKAGE_ID"), ConfigurationValue.new("prettyjson"))
    @package_configuration = Configuration.new(config_property)
    @package = PackageDefinition.new(@package_id, @package_name, @package_configuration)
    @package_repo_id = 'repo-id'
    @package_repository = PackageRepository.new(@package_repo_id, @package_name, PluginConfiguration.new('id', 'version'), nil)
    @package.setRepository(@package_repository)

    @package_definition_service = double('package-definition-service')
    allow(controller).to receive(:package_definition_service).and_return(@package_definition_service)
  end

  describe "index" do
    describe "for_admins" do
      before :each do
        enable_security
        login_as_admin
      end

      it 'should render a list of packages, for admins' do
        pkg1 = PackageDefinition.new("1", "prettyjson", Configuration.new)
        pkg2 = PackageDefinition.new("2", "express", Configuration.new)
        packages = Packages.new(pkg1, pkg2)

        expect(@package_definition_service).to receive(:getPackages).and_return(packages)

        get_with_api_header :index
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(packages, ApiV1::Config::PackagesRepresenter))
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
    before(:each) do
      enable_security
      login_as_admin
    end

    describe "for_admins" do
      it 'should render the package' do
        allow(@package_definition_service).to receive(:find).with(@package_id).and_return(@package)
        get_with_api_header :show, params: { package_id: @package_id }
        expect(response.status).to eq(200)
        expect(actual_response).to eq(expected_response({package: @package}, ApiV1::Config::PackageRepresenter))
      end

      it 'should render 404 when a package with specified id does not exist' do
        allow(@package_definition_service).to receive(:find).with('invalid-package-id').and_return(nil)
        get_with_api_header :show, params: { package_id: 'invalid-package-id' }
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe "security" do
      before :each do
        allow(controller).to receive(:load_package).and_return(nil)
      end

      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:get, :show, params: { package_id: @package_id })
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:get, :show, params: { package_id: @package_id }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:get, :show, params: { package_id: @package_id }).with(401, 'You are not authorized to perform this action.')
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
    before(:each) do
      enable_security
      login_as_admin
    end

    describe "for_admins" do
      it 'should allow deleting package' do
        allow(@package_definition_service).to receive(:find).with(@package_id).and_return(@package)
        expect(@package_definition_service).to receive(:deletePackage).with(@package, an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult)) do |pkg, user, result|
          result.setMessage(LocalizedMessage.string('RESOURCE_DELETE_SUCCESSFUL', 'package definition', @package.getId))
        end

        delete_with_api_header :destroy, params: { package_id: @package_id }
        expect(response).to have_api_message_response(200, "The package definition '#{@package_id}' was deleted successfully.")
      end

      it 'should render 404 when package does not exist' do
        allow(@package_definition_service).to receive(:find).with('invalid-package-id').and_return(nil)
        delete_with_api_header :destroy, params: { package_id: 'invalid-package-id' }
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe "security" do
      before :each do
        allow(controller).to receive(:load_package).and_return(nil)
      end

      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:delete, :destroy, params: { package_id: @package_id })
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:delete, :destroy, params: { package_id: @package_id }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:delete, :destroy, params: { package_id: @package_id }).with(401, 'You are not authorized to perform this action.')
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
    before(:each) do
      enable_security
      login_as_admin
      @package_repository_service = double('package-repository-service')
      allow(controller).to receive(:package_repository_service).and_return(@package_repository_service)

    end


    describe "for_admins" do
      it 'should render 200 created when package is created' do
        expect(@package_repository_service).to receive(:getPackageRepository).with(@package_repo_id).and_return(@package_repository)
        expect(@package_definition_service).to receive(:createPackage).with(an_instance_of(PackageDefinition), anything, an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult))
        post_with_api_header :create, params: { :package => {id: @package_id, name: @package_name, auto_update: true, package_repo: {id: @package_repo_id}, configuration: [{key: "PACKAGE_ID", value: "prettyjson"}]} }

        expect(response.status).to be(200)
        expect(actual_response).to eq(expected_response({package: @package}, ApiV1::Config::PackageRepresenter))
      end

      it 'should generate id if id is not provided by user' do
        expect(@package_repository_service).to receive(:getPackageRepository).with(@package_repo_id).and_return(@package_repository)
        expect(@package_definition_service).to receive(:createPackage).with(an_instance_of(PackageDefinition), anything, an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult))
        post_with_api_header :create, params: { :package => {name: @package_name, auto_update: true, package_repo: {id: @package_repo_id}, configuration: [{key: "PACKAGE_ID", value: "prettyjson"}]} }

        expect(actual_response).to have_key(:id)
      end

      it 'should render the error occurred while creating a package' do

        expect(@package_repository_service).to receive(:getPackageRepository).with(@package_repo_id).and_return(@package_repository)
        expect(@package_definition_service).to receive(:createPackage).with(an_instance_of(PackageDefinition), anything, an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult)) do |pkg, repo_id, user, result|
          result.unprocessableEntity(LocalizedMessage::string("SAVE_FAILED_WITH_REASON", "Validation failed"))
        end

        post_with_api_header :create, params: { :package => {id: @package_id, name: @package_name, auto_update: true, package_repo: {id: @package_repo_id}, configuration: [{key: "PACKAGE_ID", value: "prettyjson"}]} }
        expect(response).to have_api_message_response(422, "Save failed. Validation failed")
      end

      it 'should render error when the repository to which the package belongs is not found' do
        expect(@package_repository_service).to receive(:getPackageRepository).with(@package_repo_id).and_return(nil)
        post_with_api_header :create, params: { :package => {id: @package_id, name: @package_name, auto_update: true, package_repo: {id: @package_repo_id}, configuration: [{key: "PACKAGE_ID", value: "prettyjson"}]} }
        expect(response).to have_api_message_response(422, "Could not find the repository with id '#{@package_repo_id}'. It might have been deleted.")
      end
    end

    describe "security" do
      before :each do
        allow(controller).to receive(:check_for_repository).and_return(nil)
      end
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
    before(:each) do
      enable_security
      login_as_admin
      @md5 = 'some-digest'
      allow(@package_definition_service).to receive(:find).with(@package_id).and_return(@package)

      @package_repository_service = double('package-repository-service')
      allow(controller).to receive(:package_repository_service).and_return(@package_repository_service)

      @entity_hashing_service = double('entity-hashing-service')
      allow(controller).to receive(:entity_hashing_service).and_return(@entity_hashing_service)
      allow(@entity_hashing_service).to receive(:md5ForEntity).and_return(@md5)
    end

    describe "for_admins" do
      it 'should allow updating package' do
        allow(@package_definition_service).to receive(:find).with(@package_id).and_return(@package)
        result = HttpLocalizedOperationResult.new
        expect(@package_repository_service).to receive(:getPackageRepository).with(@package_repo_id).and_return(@package_repository)
        expect(@package_definition_service).to receive(:updatePackage).with(@package_id, an_instance_of(PackageDefinition), @md5, an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult)).and_return(result)
        hash = {id: @package_id, name: @package_name, auto_update: true, package_repo: {id: @package_repo_id}, configuration: [{key: "PACKAGE_ID", value: "prettyjson"}]}

        controller.request.env['HTTP_IF_MATCH'] = "\"#{Digest::MD5.hexdigest(@md5)}\""

        put_with_api_header :update, params: { package_id: @package_id, :package => hash }
        expect(response.status).to eq(200)
        expect(actual_response).to eq(expected_response({package: @package}, ApiV1::Config::PackageRepresenter))
      end

      it 'should not update package config if etag passed does not match the one on server' do
        allow(controller).to receive(:load_package).and_return(nil)
        allow(controller).to receive(:check_for_repository).and_return(nil)
        controller.request.env['HTTP_IF_MATCH'] = 'old-etag'
        hash = {name: @package_name, configuration:[ {key: "PACKAGE_ID", value:"prettyjson"}]}

        put_with_api_header :update, params: { package_id: @package_id, :package => hash }

        expect(response.status).to eq(412)
        expect(actual_response).to eq({:message => "Someone has modified the configuration for package '#{@package_id}'. Please update your copy of the config with the changes."})
      end

      it 'should not update package config if no etag is passed' do
        allow(controller).to receive(:load_package).and_return(nil)
        allow(controller).to receive(:check_for_repository).and_return(nil)
        hash = {name: @package_name, configuration:[ {key: "PACKAGE_ID", value:"prettyjson"}]}

        put_with_api_header :update, params: { package_id: @package_id, :package => hash }

        expect(response.status).to eq(412)
        expect(actual_response).to eq({:message => "Someone has modified the configuration for package '#{@package_id}'. Please update your copy of the config with the changes."})
      end

      it 'should render 404 when a package does not exist' do
        allow(@package_definition_service).to receive(:find).with('non-existent-package-id').and_return(nil)
        put_with_api_header :update, params: { package_id: 'non-existent-package-id' }
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end

      it 'should render error when the repository to which the package belongs is not found' do
        allow(@package_definition_service).to receive(:find).with(@package_id).and_return(@package)
        allow(controller).to receive(:check_for_stale_request).and_return(nil)
        expect(@package_repository_service).to receive(:getPackageRepository).with(@package_repo_id).and_return(nil)
        put_with_api_header :update, params: { package_id: @package_id, :package => {id: @package_id, name: @package_name, auto_update: true, package_repo: {id: @package_repo_id}, configuration: [{key: "PACKAGE_ID", value: "prettyjson"}]} }
        expect(response).to have_api_message_response(422, "Could not find the repository with id '#{@package_repo_id}'. It might have been deleted.")
      end
    end

    describe "security" do
      before(:each) do
        allow(controller).to receive(:check_for_stale_request).and_return(nil)
        allow(controller).to receive(:check_for_repository).and_return(nil)
      end

      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:put, :update, params: { package_id: @package_id })
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:put, :update, params: { package_id: @package_id }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:put, :update, params: { package_id: @package_id }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:put, :update, params: { package_id: @package_id })
      end

      it 'should allow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to allow_action(:put, :update, params: { package_id: @package_id })
      end
    end
  end
end
