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

describe ApiV1::Admin::RepositoriesController do

  describe :index do
    describe :for_admins do
      it 'should render a list of package repositories, for admins' do
        login_as_admin
        pkg_repo = PackageRepository.new()
        pkg_repo.setId('npm')
        all_repos = PackageRepositories.new(pkg_repo)

        @package_repository_service = double('package-repository-service')
        controller.stub(:package_repository_service).and_return(@package_repository_service)
        @package_repository_service.should_receive(:getPackageRepositories).and_return(all_repos)

        get_with_api_header :index
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(all_repos, ApiV1::Config::PackageRepositoriesRepresenter))
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
          expect(:get => 'api/admin/repositories').to route_to(action: 'index', controller: 'api_v1/admin/repositories')
        end
      end
      describe :without_header do
        it 'should not route to index action of environments controller without header' do
          expect(:get => 'api/admin/repositories').to_not route_to(action: 'index', controller: 'api_v1/admin/repositories')
          expect(:get => 'api/admin/repositories').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/repositories')
        end
      end
    end
  end

  describe :show do
    before(:each) do
      @repo_id = 'npm'
      @package_repo = PackageRepository.new()
      @package_repo.setId(@repo_id)
      @package_repository_service = double('package-repository-service')
      controller.stub(:package_repository_service).and_return(@package_repository_service)
      @package_repository_service.stub(:getPackageRepository).with(@repo_id).and_return(@package_repo)
    end

    describe :for_admins do
      it 'should render the package repository' do
        login_as_admin

        get_with_api_header :show, repo_id: @repo_id
        expect(response.status).to eq(200)
        expect(actual_response).to eq(expected_response(@package_repo, ApiV1::Config::PackageRepositoryRepresenter))
      end

      it 'should render 404 when a package repository does not exist' do
        login_as_admin
        @environment_config_service.stub(:getEnvironmentConfig).and_raise(com.thoughtworks.go.config.exceptions.NoSuchEnvironmentException.new(CaseInsensitiveString.new('foo-env')))
        @package_repository_service.stub(:getPackageRepository).and_return(nil)

        get_with_api_header :show, repo_id: 'invalid-package-repo-name'
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe :security do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:get, :show, repo_id: @repo_id)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:get, :show, repo_id: @repo_id).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:get, :show, repo_id: @repo_id).with(401, 'You are not authorized to perform this action.')
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
        it 'should route to show action of package repositories controller for specified package repository id' do
          expect(:get => 'api/admin/repositories/foo123').to route_to(action: 'show', controller: 'api_v1/admin/repositories', repo_id: 'foo123')
        end
      end
      describe :without_header do
        it 'should not route to show action of package repositories controller without header' do
          expect(:get => 'api/admin/repositories/foo').to_not route_to(action: 'show', controller: 'api_v1/admin/repositories')
          expect(:get => 'api/admin/repositories/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/repositories/foo')
        end
      end
    end
  end

  describe :destroy do
    before(:each) do
      @repo_id = 'npm'
      @package_repo = PackageRepository.new()
      @package_repo.setId(@repo_id)
      @package_repository_service = double('package-repository-service')
      controller.stub(:package_repository_service).and_return(@package_repository_service)
      @package_repository_service.stub(:getPackageRepository).with(@repo_id).and_return(@package_repo)
    end

    describe :for_admins do
      it 'should allow deleting package repositories' do
        login_as_admin

        @package_repository_service.should_receive(:deleteRepository).with(an_instance_of(Username), @package_repo, an_instance_of(HttpLocalizedOperationResult)) do |user, repo, result|
          result.setMessage(LocalizedMessage.string("PACKAGE_REPOSITORY_DELETE_SUCCESSFUL", @package_repo.getId()))
        end

        delete_with_api_header :destroy, repo_id: @repo_id
        expect(response).to have_api_message_response(200, "Package Repository with id 'npm' was deleted successfully.")
      end

      it 'should render 404 when a package repository does not exist' do
        login_as_admin

        @package_repository_service.stub(:getPackageRepository).with(@repo_id).and_return(nil)

        delete_with_api_header :destroy, repo_id: @repo_id
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe :security do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:delete, :destroy, repo_id: @repo_id)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:delete, :destroy, repo_id: @repo_id).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:delete, :destroy, repo_id: @repo_id).with(401, 'You are not authorized to perform this action.')
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
        it 'should route to destroy action of package repository controller for specified package repository id' do
          expect(:delete => 'api/admin/repositories/foo123').to route_to(action: 'destroy', controller: 'api_v1/admin/repositories', repo_id: 'foo123')
        end
      end
      describe :without_header do
        it 'should not route to destroy action of package repositories controller without header' do
          expect(:delete => 'api_v1/admin/repositories/foo').to_not route_to(action: 'destroy', controller: 'api_v1/admin/repositories')
          expect(:delete => 'api_v1/admin/repositories/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api_v1/admin/repositories/foo')
        end
      end
    end
  end

  describe :create do
    before(:each) do
      @repo_id = 'npmOrg'
      @repo_name = 'npmOrg'
      @plugin_id = 'npm'
      @package_repo = PackageRepository.new()
      @package_repo.setId(@repo_id)
      @package_repo.setName(@repo_name)
      @package_repo.setPluginConfiguration(PluginConfiguration.new(@plugin_id, '1'))
      @package_repository_service = double('package-repository-service')
      controller.stub(:package_repository_service).and_return(@package_repository_service)
      @package_repository_service.stub(:getPackageRepository).and_return(@package_repo)
    end


    describe :for_admins do
      it 'should render 200 created when package repository is created' do
        login_as_admin

        @package_repository_service.should_receive(:createPackageRepository).with(an_instance_of(PackageRepository), an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult))

        post_with_api_header :create, :repository => {name: @repo_name, type: @plugin_id, configuration:[]}
        expect(response.status).to be(200)
        expect(actual_response).to eq(expected_response(@package_repo, ApiV1::Config::PackageRepositoryRepresenter))
      end

      it 'should render the error occurred while creating a package repository' do
        @plugin_id = 'invalid_id'
        login_as_admin

        @package_repository_service.should_receive(:createPackageRepository).with(an_instance_of(PackageRepository), an_instance_of(Username), an_instance_of(HttpLocalizedOperationResult)) do |repo, user, result|
          result.unprocessableEntity(LocalizedMessage.string("INVALID_PLUGIN_TYPE", @plugin_id));
        end

        post_with_api_header :create, :repository => {name: @repo_name, type: @plugin_id, configuration:[]}
        expect(response).to have_api_message_response(422, 'Invalid plugin type \'invalid_id\'.')
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
        it 'should route to create action of package repositories controller' do
          expect(:post => 'api/admin/repositories/').to route_to(action: 'create', controller: 'api_v1/admin/repositories')
        end
      end
      describe :without_header do
        it 'should not route to create action of environments controller without header' do
          expect(:post => 'api/admin/repositories').to_not route_to(action: 'create', controller: 'api_v1/admin/repositories')
          expect(:post => 'api/admin/environments').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/environments')
        end
      end
    end
  end

  describe :update do
    before(:each) do
      @repo_id = 'npm'
      @package_repo = PackageRepository.new()
      @package_repo.setId(@repo_id)
      @md5 = 'some-digest'
      @package_repository_service = double('package-repository-service')
      @entity_hashing_service = double('entity-hashing-see=rvice')
      controller.stub(:package_repository_service).and_return(@package_repository_service)
      controller.stub(:entity_hashing_service).and_return(@entity_hashing_service)
      @package_repository_service.stub(:getPackageRepository).and_return(@package_repo)
      @entity_hashing_service.stub(:md5ForEntity).and_return(@md5)
    end

    describe :for_admins do
      it 'should allow updating package repository' do
        login_as_admin
        result = HttpLocalizedOperationResult.new

        @package_repository_service.should_receive(:updatePackageRepository).with(@package_repo, an_instance_of(PackageRepository), an_instance_of(Username), @md5, an_instance_of(HttpLocalizedOperationResult)).and_return(result)
        hash = {name: "foo", plugin_configuration: {id: 'npm', version: '1'}, configuration: [ {key: 'REPO_URL', value:'https://foo.bar'}]}

        controller.request.env['HTTP_IF_MATCH'] = "\"#{Digest::MD5.hexdigest(@md5)}\""

        put_with_api_header :update, :repo_id => @repo_id, :repository => hash
        expect(response.status).to eq(200)
        expect(actual_response).to eq(expected_response(@package_repo, ApiV1::Config::PackageRepositoryRepresenter))
      end

      it 'should not update package repository if etag passed does not match the one on server' do
        login_as_admin
        controller.request.env['HTTP_IF_MATCH'] = 'old-etag'

        hash = {name: "foo", plugin_configuration: {id: 'npm', version: '1'}, configuration: [ {key: 'REPO_URL', value:'https://foo.bar'}]}
        put_with_api_header :update, :repo_id => @repo_id, :repository => hash

        expect(response.status).to eq(412)
        expect(actual_response).to eq({:message => "Someone has modified the configuration for Package Repository 'npm'. Please update your copy of the config with the changes."})
      end

      it 'should not update package repository if no etag is passed' do
        login_as_admin
        hash = {name: "foo", plugin_configuration: {id: 'npm', version: '1'}, configuration: [ {key: 'REPO_URL', value:'https://foo.bar'}]}
        put_with_api_header :update, :repo_id => @repo_id, :repository => hash

        expect(response.status).to eq(412)
        expect(response).to have_api_message_response(412, "Someone has modified the configuration for Package Repository 'npm'. Please update your copy of the config with the changes.")
      end

      it 'should render 404 when a package repository does not exist' do
        login_as_admin
        @package_repository_service.stub(:getPackageRepository).with('non-existing-repo-id').and_return(nil)
        controller.request.env['HTTP_IF_MATCH'] = "\"#{Digest::MD5.hexdigest(@md5)}\""
        hash = {name: "foo", plugin_configuration: {id: 'npm', version: '1'}, configuration: [ {key: 'REPO_URL', value:'https://foo.bar'}]}
        put_with_api_header :update, :repo_id => 'non-existing-repo-id', :repository => hash
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe :security do
      before(:each) do
        controller.stub(:check_for_stale_request).and_return(nil)
      end

      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:put, :update, repo_id: @repo_id)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:put, :update, repo_id: @repo_id).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:put, :update, repo_id: @repo_id).with(401, 'You are not authorized to perform this action.')
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
        it 'should route to update action of repositories controller for specified package repository id' do
          expect(:put => 'api/admin/repositories/foo123').to route_to(action: 'update', controller: 'api_v1/admin/repositories', repo_id: 'foo123')
        end
      end
      describe :without_header do
        it 'should not route to put action of repositories controller without header' do
          expect(:put => 'api_v1/admin/repositories/foo').to_not route_to(action: 'update', controller: 'api_v1/admin/repositories')
          expect(:put => 'api_v1/admin/repositories/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api_v1/admin/repositories/foo')
        end
      end
    end
  end
end
