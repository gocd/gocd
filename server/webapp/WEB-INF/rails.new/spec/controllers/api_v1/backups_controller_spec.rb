##########################################################################
# Copyright 2015 ThoughtWorks, Inc.
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

describe ApiV1::BackupsController do
  describe :create do
    describe :for_admins do
      it 'should create a backup' do
        login_as_admin
        @backup_service = double('backup service')
        john = User.new('jdoe', 'Jon Doe', ['jdoe', 'jdoe@example.com'].to_java(:string), 'jdoe@example.com', true)
        backup = com.thoughtworks.go.server.domain.ServerBackup.new("file_path", java.util.Date.new, "jdoe")

        @user_service = double('user service')
        controller.stub(:user_service).and_return(@user_service)
        @user_service.stub(:findUserByName).and_return(john)

        @backup_service.should_receive(:startBackup).with(@user, instance_of(HttpLocalizedOperationResult)) do |user, result|
          result.setMessage(LocalizedMessage.string("BACKUP_COMPLETED_SUCCESSFULLY"));
          backup
        end

        controller.stub(:backup_service).and_return(@backup_service)
        post_with_api_header :create
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(backup, ApiV1::BackupRepresenter))
      end
    end

    describe :security do
      before(:each) do
        @backup_service = double('backup service')
        john = User.new('jdoe', 'Jon Doe', ['jdoe', 'jdoe@example.com'].to_java(:string), 'jdoe@example.com', true)
        backup = com.thoughtworks.go.server.domain.ServerBackup.new("file_path", java.util.Date.new, "jdoe")

        @user_service = double('user service')
        controller.stub(:user_service).and_return(@user_service)
        @user_service.stub(:findUserByName).and_return(john)

        @backup_service.stub(:startBackup).with(@user, instance_of(HttpLocalizedOperationResult)) do |user, result|
          result.setMessage(LocalizedMessage.string("BACKUP_COMPLETED_SUCCESSFULLY"));
          backup
        end

        controller.stub(:backup_service).and_return(@backup_service)
      end

      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:post, :create)
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

        it 'should route to create action of the backups controller with custom header' do
          expect_any_instance_of(HeaderConstraint).to receive(:matches?).with(any_args).and_return(true)
          expect(:post => 'api/backups').to route_to(action: 'create', controller: 'api_v1/backups')
        end

        it 'should route to errors without custom header' do
          expect_any_instance_of(HeaderConstraint).to receive(:matches?).with(any_args).and_return(false)
          expect(:post => 'api/backups').to route_to(controller: 'api_v1/errors', action: 'not_found', url: 'backups')
        end
      end
      describe :without_header do
        it 'should not route to create action of backups controller without header' do
          expect(:post => 'api/backups').to_not route_to(action: 'backups', controller: 'api_v1/backups')
          expect(:post => 'api/backups').to route_to(controller: 'application', action: 'unresolved', url: 'api/backups')
        end
      end

    end

  end
end
