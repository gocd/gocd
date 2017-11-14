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

describe ApiV2::UsersController do

  include ApiV2::ApiVersionHelper

  before :each do
    @user_service = double('user service')
    allow(controller).to receive(:user_service).and_return(@user_service)
  end

  describe "index" do
    describe "for_admins" do
      it 'should render a list of users, for admins' do
        login_as_admin
        john = User.new('jdoe', 'Jon Doe', ['jdoe', 'jdoe@example.com'].to_java(:string), 'jdoe@example.com', true)

        expect(@user_service).to receive(:allUsers).and_return([john])

        get_with_api_header :index
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response([john], ApiV2::UsersRepresenter))
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
    end
  end

  describe "show" do
    before(:each) do
      @john = User.new('jdoe', 'Jon Doe', ['jdoe', 'jdoe@example.com'].to_java(:string), 'jdoe@example.com', true)

      @user_service = double('user service')
      allow(controller).to receive(:user_service).and_return(@user_service)
      allow(@user_service).to receive(:findUserByName).with(@john.name).and_return(@john)
    end

    describe "for_admins" do
      it 'should render the user' do
        login_as_admin

        get_with_api_header :show, params: { login_name: @john.name }
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(@john, ApiV2::UserRepresenter))
      end

      it 'should render 404 when a user does not exist' do
        login_as_admin

        login_name = SecureRandom.hex
        allow(@user_service).to receive(:findUserByName).with(login_name).and_return(com.thoughtworks.go.domain.NullUser.new)
        get_with_api_header :show, params: { login_name: login_name }
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe "security" do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:get, :show, params: { login_name: @john.name })
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:get, :show, params: { login_name: @john.name }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:get, :show, params: { login_name: @john.name }).with(401, 'You are not authorized to perform this action.')
      end
    end
  end

  describe "destroy" do
    before(:each) do
      @john = User.new('jdoe', 'Jon Doe', ['jdoe', 'jdoe@example.com'].to_java(:string), 'jdoe@example.com', true)

      @user_service = double('user service')
      allow(controller).to receive(:user_service).and_return(@user_service)
      allow(@user_service).to receive(:findUserByName).with(@john.name).and_return(@john)
      allow(@user_service).to receive(:deleteUser).with(@john.name, anything()).and_return(@john)
    end

    describe "for_admins" do
      it 'should allow deleting users' do
        login_as_admin

        expect(@user_service).to receive(:deleteUser).with(@john.name, an_instance_of(HttpLocalizedOperationResult)) do |username, result|
          result.setMessage(LocalizedMessage.string("RESOURCE_DELETE_SUCCESSFUL", "user", username))
        end

        delete_with_api_header :destroy, params: { login_name: @john.name }
        expect(response).to be_ok
        expect(response).to have_api_message_response(200, "The user 'jdoe' was deleted successfully.")
      end

      it 'should render 404 when a user does not exist' do
        login_as_admin

        login_name = SecureRandom.hex
        allow(@user_service).to receive(:findUserByName).with(login_name).and_return(com.thoughtworks.go.domain.NullUser.new)
        delete_with_api_header :destroy, params: { login_name: login_name }
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end


    describe "security" do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:delete, :destroy, params: { login_name: @john.name })
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:delete, :destroy, params: { login_name: @john.name }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:delete, :destroy, params: { login_name: @john.name }).with(401, 'You are not authorized to perform this action.')
      end
    end
  end

  describe 'delete' do
    before(:each) do
      @john = User.new('jdoe', 'Jon Doe', ['jdoe', 'jdoe@example.com'].to_java(:string), 'jdoe@example.com', true)
      @joanne = User.new('jrowling', 'Joanne Rowling', ['jrowling'].to_java(:string), 'jrowling@example.com', false)
    end

    describe 'for_admins' do
      it 'should delete users' do
        login_as_admin

        @user_service = double('user_service')
        allow(controller).to receive(:user_service).and_return(@user_service)
        expect(@user_service).to receive(:deleteUsers).with([@john.name, @joanne.name], an_instance_of(HttpLocalizedOperationResult)) do |users, result|
          result.setMessage(LocalizedMessage.string("RESOURCE_DELETE_SUCCESSFUL", "users", users))
        end

        delete_with_api_header :bulk_delete, params: { users: [@john.name, @joanne.name] }

        expect(response).to have_api_message_response(200, "The users '[\"jdoe\", \"jrowling\"]' was deleted successfully.")
      end

      it 'should throw an error if delete users was unsuccessful when some users are enabled or non existent' do
        login_as_admin

        @user_service = double('user_service')
        allow(controller).to receive(:user_service).and_return(@user_service)
        non_existent_users = java.util.ArrayList.new
        non_existent_users.add('jane')
        non_existent_users.add('joe')

        enabled_users = java.util.ArrayList.new
        enabled_users.add('john')

        bulkDeletionFailureResult = BulkDeletionFailureResult.new(non_existent_users, enabled_users)
        expect(@user_service).to receive(:deleteUsers).with([@john.name, @joanne.name], an_instance_of(HttpLocalizedOperationResult)) do |users, result|
          result.unprocessableEntity(LocalizedMessage.string("USER_ENABLED_OR_NOT_FOUND", [users[0]], [users[1]]))
          bulkDeletionFailureResult
        end

        delete_with_api_header :bulk_delete, params: { users: [@john.name, @joanne.name] }

        expect(response).to have_api_message_response(422, "Deletion failed because some users were either enabled or do not exist.")
      end

      it 'should throw an error if delete users was unsuccessful when no users are provided' do
        login_as_admin

        @user_service = double('user_service')
        allow(controller).to receive(:user_service).and_return(@user_service)

        expect(@user_service).to receive(:deleteUsers).with([], an_instance_of(HttpLocalizedOperationResult)) do |users, result|
          result.badRequest(LocalizedMessage.string("NO_USERS_SELECTED"))
          BulkDeletionFailureResult.new()
        end

        delete_with_api_header :bulk_delete, params: { users: [] }

        expect(response).to have_api_message_response(400, "No users selected.")
      end
    end

    describe 'security' do
      it 'should allow admins to bulk delete users' do
        login_as_admin
        expect(controller).to allow_action(:delete, 'bulk_delete')
      end

      it 'should not allow non-admins to bulk delete users' do
        login_as_user
        expect(controller).to disallow_action(:delete, 'bulk_delete')
      end

      it 'should allow anybody to bulk delete users if security is disabled' do
        disable_security
        expect(controller).to allow_action(:delete, 'bulk_delete')
      end
    end
  end

  describe "update" do
    before(:each) do
      @john = User.new('jdoe', 'Jon Doe', ['jdoe', 'jdoe@example.com'].to_java(:string), 'jdoe@example.com', true)

      @user_service = double('user service')
      allow(controller).to receive(:user_service).and_return(@user_service)
      allow(@user_service).to receive(:findUserByName).with(@john.name).and_return(@john)
    end

    describe "for_admins" do
      it 'should allow patching users' do
        login_as_admin
        expect(@user_service).to receive(:save).with(@john, TriState.TRUE, TriState.FALSE, 'foo@example.com', 'foo, bar', an_instance_of(HttpLocalizedOperationResult)).and_return(@john)

        patch_with_api_header :update, params: { login_name: @john.name, enabled: true, email_me: false, email: 'foo@example.com', checkin_aliases: 'foo, bar' }
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(@john, ApiV2::UserRepresenter))
      end

      it 'should render 404 when a user does not exist' do
        login_as_admin

        login_name = SecureRandom.hex
        allow(@user_service).to receive(:findUserByName).with(login_name).and_return(com.thoughtworks.go.domain.NullUser.new)
        patch_with_api_header :update, params: { login_name: login_name }
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe "security" do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:patch, :update, params: { login_name: @john.name })
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:patch, :update, params: { login_name: @john.name }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:patch, :update, params: { login_name: @john.name }).with(401, 'You are not authorized to perform this action.')
      end
    end
  end

  describe "create" do
    before(:each) do
      @john = User.new('jdoe')

      @user_service = double('user service')
      allow(controller).to receive(:user_service).and_return(@user_service)
      allow(@user_service).to receive(:findUserByName).with(anything()).and_return(com.thoughtworks.go.domain.NullUser.new)
    end

    describe "for_admins" do
      it 'should render 201 created when user is created' do
        login_as_admin

        expect(@user_service).to receive(:withEnableUserMutex).and_yield

        expect(@user_service).to receive(:save).with(@john, TriState.TRUE, TriState.FALSE, 'foo@example.com', 'foo, bar', an_instance_of(HttpLocalizedOperationResult)).and_return(@john)

        post_with_api_header :create, params: { login_name: @john.name, enabled: true, email_me: false, email: 'foo@example.com', checkin_aliases: 'foo, bar' }
        expect(response.status).to be(201)
        expect(actual_response).to eq(expected_response(@john, ApiV2::UserRepresenter))
      end

      it 'should render 409 conflict when a user already exists' do
        login_as_admin

        login_name = SecureRandom.hex
        expect(@user_service).to receive(:withEnableUserMutex).and_yield
        allow(@user_service).to receive(:findUserByName).with(login_name).and_return(User.new(login_name))
        post_with_api_header :create, params: { login_name: login_name }
        expect(response).to have_api_message_response(409, "The user `#{login_name}` already exists.")
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
    end
  end
end
