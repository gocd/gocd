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

describe ApiV1::UsersController do

  describe :index do
    describe :for_admins do
      it 'should render a list of users, for admins' do
        login_as_admin
        john = User.new('jdoe', 'Jon Doe', ['jdoe', 'jdoe@example.com'].to_java(:string), 'jdoe@example.com', true)

        @user_service = double('user service')
        controller.stub(:user_service).and_return(@user_service)
        @user_service.should_receive(:allUsers).and_return([john])

        get_with_api_header :index
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response([john], ApiV1::UsersRepresenter))
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
      @john = User.new('jdoe', 'Jon Doe', ['jdoe', 'jdoe@example.com'].to_java(:string), 'jdoe@example.com', true)

      @user_service = double('user service')
      controller.stub(:user_service).and_return(@user_service)
      @user_service.stub(:findUserByName).with(@john.name).and_return(@john)
    end

    describe :for_admins do
      it 'should render the user' do
        login_as_admin

        get_with_api_header :show, login_name: @john.name
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(@john, ApiV1::UserRepresenter))
      end

      it 'should render 404 when a user does not exist' do
        login_as_admin

        login_name = SecureRandom.hex
        @user_service.stub(:findUserByName).with(login_name).and_return(com.thoughtworks.go.domain.NullUser.new)
        get_with_api_header :show, login_name: login_name
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe :security do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:get, :show, login_name: @john.name)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:get, :show, login_name: @john.name).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:get, :show, login_name: @john.name).with(401, 'You are not authorized to perform this action.')
      end
    end
  end

  describe :destroy do
    before(:each) do
      @john = User.new('jdoe', 'Jon Doe', ['jdoe', 'jdoe@example.com'].to_java(:string), 'jdoe@example.com', true)

      @user_service = double('user service')
      controller.stub(:user_service).and_return(@user_service)
      @user_service.stub(:findUserByName).with(@john.name).and_return(@john)
      @user_service.stub(:deleteUser).with(@john.name, anything()).and_return(@john)
    end

    describe :for_admins do
      it 'should allow deleting users' do
        login_as_admin

        @user_service.should_receive(:deleteUser).with(@john.name, an_instance_of(HttpLocalizedOperationResult)) do |username, result|
          result.setMessage(LocalizedMessage.string("USER_DELETE_SUCCESSFUL", [username].to_java(java.lang.String)))
        end

        delete_with_api_header :destroy, login_name: @john.name
        expect(response).to be_ok
        expect(response).to have_api_message_response(200, "User 'jdoe' was deleted successfully.")
      end

      it 'should render 404 when a user does not exist' do
        login_as_admin

        login_name = SecureRandom.hex
        @user_service.stub(:findUserByName).with(login_name).and_return(com.thoughtworks.go.domain.NullUser.new)
        delete_with_api_header :destroy, login_name: login_name
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe :security do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:delete, :destroy, login_name: @john.name)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:delete, :destroy, login_name: @john.name).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:delete, :destroy, login_name: @john.name).with(401, 'You are not authorized to perform this action.')
      end
    end
  end

  describe :update do
    before(:each) do
      @john = User.new('jdoe', 'Jon Doe', ['jdoe', 'jdoe@example.com'].to_java(:string), 'jdoe@example.com', true)

      @user_service = double('user service')
      controller.stub(:user_service).and_return(@user_service)
      @user_service.stub(:findUserByName).with(@john.name).and_return(@john)
    end

    describe :for_admins do
      it 'should allow patching users' do
        login_as_admin
        @user_service.should_receive(:update).with(@john, TriState.TRUE, TriState.FALSE, 'foo@example.com', 'foo, bar', an_instance_of(HttpLocalizedOperationResult)).and_return(@john)

        patch_with_api_header :update, login_name: @john.name, enabled: true, email_me: false, email: 'foo@example.com', checkin_aliases: 'foo, bar'
        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(@john, ApiV1::UserRepresenter))
      end

      it 'should render 404 when a user does not exist' do
        login_as_admin

        login_name = SecureRandom.hex
        @user_service.stub(:findUserByName).with(login_name).and_return(com.thoughtworks.go.domain.NullUser.new)
        patch_with_api_header :update, login_name: login_name
        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe :security do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:patch, :update, login_name: @john.name)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous
        expect(controller).to disallow_action(:patch, :update, login_name: @john.name).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:patch, :update, login_name: @john.name).with(401, 'You are not authorized to perform this action.')
      end
    end
  end

end
