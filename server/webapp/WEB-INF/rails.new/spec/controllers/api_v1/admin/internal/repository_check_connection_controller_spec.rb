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

describe ApiV1::Admin::Internal::RepositoryCheckConnectionController do
  include ApiHeaderSetupTeardown, ApiV1::ApiVersionHelper

  describe 'security' do
    it 'should allow anyone, with security disabled' do
      disable_security
      expect(controller).to allow_action(:post, :check_connection)
    end

    it 'should disallow anonymous users, with security enabled' do
      enable_security
      login_as_anonymous
      expect(controller).to disallow_action(:post, :check_connection).with(401, 'You are not authorized to perform this action.')
    end

    it 'should disallow normal users, with security enabled' do
      login_as_user
      expect(controller).to disallow_action(:post, :check_connection)
    end

    it 'should allow admin users, with security enabled' do
      login_as_admin
      expect(controller).to allow_action(:post, :check_connection)
    end

    it 'should allow pipeline group admin users, with security enabled' do
      login_as_group_admin
      expect(controller).to allow_action(:post, :check_connection)
    end
  end

  describe 'route' do
    describe 'with_header' do

      it 'should route to check_connection action of the repository check connection controller' do
        expect(:post => 'api/admin/internal/repository_check_connection').to route_to(action: 'check_connection', controller: 'api_v1/admin/internal/repository_check_connection')
      end
    end
    describe 'without_header' do
      before :each do
        teardown_header
      end
      it 'should not route to check_connection action of repository check connection controller without header' do
        expect(:post => 'api/admin/internal/repository_check_connection').to_not route_to(action: 'check_connection', controller: 'api_v1/admin/internal/repository_check_connection')
        expect(:post => 'api/admin/internal/repository_check_connection').to route_to(action: 'unresolved', controller: 'application', url: 'api/admin/internal/repository_check_connection')
      end
    end
  end

  describe 'admin' do
    before :each do
      login_as_admin
      @package_repository_service = double('package_repository_service')
      controller.stub(:package_repository_service).and_return(@package_repository_service)
    end

    it 'should return the successful response for a valid package repo' do
      result = HttpLocalizedOperationResult.new
      repository = {
        plugin: 'foo',
        configuration: [
          {
            key: 'REPO_URL',
            value: 'foo'
          }
        ]
      }
      @package_repository_service.should_receive(:checkConnection).with(an_instance_of(PackageRepository), result) do |repository, result|
        result.setMessage(LocalizedMessage::string("CONNECTION_OK"))
      end

      post_with_api_header :check_connection, repository_check_connection: repository

      expect(response).to have_api_message_response(200, "Connection OK. {0}")
    end
  end
end