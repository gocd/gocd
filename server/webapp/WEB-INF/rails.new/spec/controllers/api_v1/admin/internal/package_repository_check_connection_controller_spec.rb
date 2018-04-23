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

describe ApiV1::Admin::Internal::PackageRepositoryCheckConnectionController do

  include ApiV1::ApiVersionHelper

  before :each do
    @package_repository_service = double('package_repository_service')
    allow(controller).to receive(:package_repository_service).and_return(@package_repository_service)

    @package_definition_service = double('package_definition_service')
    allow(controller).to receive(:package_definition_service).and_return(@package_definition_service)
  end

  describe 'repository_check_connection' do
    describe 'security' do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:post, :repository_check_connection)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:post, :repository_check_connection).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:post, :repository_check_connection).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:post, :repository_check_connection)
      end

      it 'should allow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to allow_action(:post, :repository_check_connection)
      end
    end

    describe 'admin' do
      before :each do
        login_as_admin
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
        expect(@package_repository_service).to receive(:checkConnection).with(an_instance_of(PackageRepository), result) do |repository, result|
          result.setMessage(LocalizedMessage::string("CONNECTION_OK"))
        end

        post_with_api_header :repository_check_connection, params: { pacakge_repository_check_connection: repository }

        expect(response).to have_api_message_response(200, "Connection OK. {0}")
      end
    end
  end

  describe 'package_check_connection' do
    describe 'security' do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:post, :package_check_connection)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:post, :package_check_connection).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:post, :package_check_connection)
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:post, :package_check_connection)
      end

      it 'should allow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to allow_action(:post, :package_check_connection)
      end
    end

    describe 'admin' do
      before :each do
        @package_definition = {
          repo_id: 'foo',
          configuration: [
            {
              key: 'PACKAGE_SPEC',
              value: 'go-server'
            }
          ]
        }

        login_as_admin
      end

      it 'should return the successful response for a valid package repo' do
        package_repository = PackageRepository.new
        package_repository.setPluginConfiguration(PluginConfiguration.new("yum", nil))
        expect(@package_repository_service).to receive(:getPackageRepository).with(anything).and_return(package_repository)
        result = HttpLocalizedOperationResult.new
        expect(@package_definition_service).to receive(:checkConnection).with(an_instance_of(PackageDefinition), result) do |package_definition, result|
          result.setMessage(LocalizedMessage::string("CONNECTION_OK"))
        end

        post_with_api_header :package_check_connection, params: { package_repository_check_connection: @package_definition }

        expect(response).to have_api_message_response(200, "Connection OK. {0}")
      end

      it 'should error out if repository is not found' do

        expect(@package_repository_service).to receive(:getPackageRepository).with(anything).and_return(nil)

        post_with_api_header :package_check_connection, params: { package_repository_check_connection: @package_definition }
        expect(response).to have_api_message_response(404, "Either the resource you requested was not found, or you are not authorized to perform this action.")
      end
    end
  end
end