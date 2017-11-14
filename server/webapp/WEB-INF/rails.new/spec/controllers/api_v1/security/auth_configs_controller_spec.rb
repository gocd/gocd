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

describe ApiV1::Admin::Security::AuthConfigsController do

  include ApiV1::ApiVersionHelper

  before :each do
    @security_auth_config_service = double('security_auth_config_service')
    @entity_hashing_service = double('entity_hashing_service')
    allow(controller).to receive(:entity_hashing_service).and_return(@entity_hashing_service)
    allow(controller).to receive(:security_auth_config_service).and_return(@security_auth_config_service)
  end

  describe "index" do
    describe "security" do
      it 'should allow all with security disabled' do
        disable_security
        expect(controller).to allow_action(:get, :index)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:get, :index).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:get, :index).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin, with security enabled' do
        enable_security
        login_as_admin

        expect(controller).to allow_action(:get, :index)
      end

      it 'should disallow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to disallow_action(:get, :index)
      end
    end

    describe 'admin' do
      it 'should list all security auth configs' do
        enable_security
        login_as_admin

        auth_config = SecurityAuthConfig.new('foo', 'cd.go.ldap')
        expect(@security_auth_config_service).to receive(:listAll).and_return({'foo' => auth_config})

        get_with_api_header :index

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response([auth_config], ApiV1::Security::AuthConfigsRepresenter))
      end
    end
  end

  describe "show" do
    describe "security" do
      before :each do
        allow(controller).to receive(:load_entity_from_config).and_return(nil)
      end

      it 'should allow all with security disabled' do
        disable_security

        expect(controller).to allow_action(:get, :show, params: { auth_config_id: 'foo' })
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:get, :show, params: { auth_config_id: 'foo' }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:get, :show, params: { auth_config_id: 'foo' }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin, with security enabled' do
        enable_security
        login_as_admin

        expect(controller).to allow_action(:get, :show, params: { auth_config_id: 'foo' })
      end

      it 'should disallow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to disallow_action(:get, :show, params: {auth_config_id: 'foo'})
      end
    end

    describe 'admin' do
      before(:each) do
        enable_security
        login_as_admin
        @result = HttpLocalizedOperationResult.new
      end

      it 'should render the security auth config of specified name' do
        auth_config = SecurityAuthConfig.new('ldap', 'cd.go.ldap')
        expect(@entity_hashing_service).to receive(:md5ForEntity).with(an_instance_of(SecurityAuthConfig)).and_return('md5')
        expect(@security_auth_config_service).to receive(:findProfile).with('ldap').and_return(auth_config)

        get_with_api_header :show, params: { auth_config_id: 'ldap' }

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(auth_config, ApiV1::Security::AuthConfigRepresenter))
      end

      it 'should return 404 if the security auth config does not exist' do
        expect(@security_auth_config_service).to receive(:findProfile).with('non-existent-security-auth-config').and_return(nil)

        get_with_api_header :show, params: { auth_config_id: 'non-existent-security-auth-config' }

        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

  end

  describe "create" do
    describe "security" do
      it 'should allow all with security disabled' do
        disable_security

        expect(controller).to allow_action(:post, :create)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:post, :create).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:post, :create).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin, with security enabled' do
        enable_security
        login_as_admin

        expect(controller).to allow_action(:post, :create)
      end

      it 'should disallow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to disallow_action(:post, :create)
      end
    end
    describe 'admin' do
      before(:each) do
        enable_security
        login_as_admin
      end

      it 'should deserialize auth config from given parameters' do
        auth_config = SecurityAuthConfig.new('ldap', 'cd.go.ldap')
        allow(controller).to receive(:etag_for).and_return('some-md5')
        expect(@security_auth_config_service).to receive(:create).with(anything, an_instance_of(SecurityAuthConfig), an_instance_of(HttpLocalizedOperationResult))
        post_with_api_header :create, params: { auth_config: auth_config_hash }

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(auth_config, ApiV1::Security::AuthConfigRepresenter))
      end

      it 'should fail to save if there are validation errors' do
        result = double('HttpLocalizedOperationResult')
        allow(HttpLocalizedOperationResult).to receive(:new).and_return(result)
        allow(result).to receive(:isSuccessful).and_return(false)
        allow(result).to receive(:message).with(anything()).and_return('Save failed')
        allow(result).to receive(:httpCode).and_return(422)
        expect(@security_auth_config_service).to receive(:create).with(anything, an_instance_of(SecurityAuthConfig), result)

        post_with_api_header :create, params: { auth_config: auth_config_hash }

        expect(response).to have_api_message_response(422, 'Save failed')
      end
    end

  end

  describe "update" do
    describe "security" do
      before :each do
        allow(controller).to receive(:load_entity_from_config).and_return(nil)
        allow(controller).to receive(:check_for_stale_request).and_return(nil)
        allow(controller).to receive(:check_for_attempted_rename).and_return(nil)
      end
      it 'should allow all with security disabled' do
        disable_security
        expect(controller).to allow_action(:put, :update, params: { auth_config_id: 'foo' })
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:put, :update, params: { auth_config_id: 'foo' }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:put, :update, params: { auth_config_id: 'foo' }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:put, :update, params: { auth_config_id: 'foo' })
      end

      it 'should disallow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to disallow_action(:put, :update, params: {auth_config_id: 'foo'})
      end
    end
    describe 'admin' do
      before(:each) do
        enable_security
        login_as_admin
      end

      it 'should not allow rename of auth config id' do
        auth_config = SecurityAuthConfig.new('ldap', 'cd.go.ldap')
        allow(controller).to receive(:load_entity_from_config).and_return(auth_config)
        allow(controller).to receive(:check_for_stale_request).and_return(nil)

        put_with_api_header :update, params: { auth_config_id: 'foo', auth_config: auth_config_hash }

        expect(response).to have_api_message_response(422, 'Renaming of security auth config IDs is not supported by this API.')
      end

      it 'should fail update if etag does not match' do
        auth_config = SecurityAuthConfig.new('ldap', 'cd.go.ldap')
        allow(controller).to receive(:load_entity_from_config).and_return(auth_config)
        allow(controller).to receive(:etag_for).and_return('another-etag')
        controller.request.env['HTTP_IF_MATCH'] = 'some-etag'

        put_with_api_header :update, params: { auth_config_id: 'ldap', auth_config: auth_config_hash }

        expect(response).to have_api_message_response(412, "Someone has modified the configuration for Security auth config 'ldap'. Please update your copy of the config with the changes.")
      end

      it 'should proceed with update if etag matches' do
        controller.request.env['HTTP_IF_MATCH'] = %Q{"#{Digest::MD5.hexdigest('md5')}"}
        auth_config = SecurityAuthConfig.new('ldap', 'cd.go.ldap')
        allow(controller).to receive(:load_entity_from_config).twice.and_return(auth_config)

        expect(@entity_hashing_service).to receive(:md5ForEntity).with(an_instance_of(SecurityAuthConfig)).exactly(3).times.and_return('md5')
        expect(@security_auth_config_service).to receive(:update).with(anything, 'md5', an_instance_of(SecurityAuthConfig), anything)

        put_with_api_header :update, params: { auth_config_id: 'ldap', auth_config: auth_config_hash }

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(auth_config, ApiV1::Security::AuthConfigRepresenter))
      end
    end
  end

  describe "destroy" do
    describe "security" do
      before :each do
        allow(controller).to receive(:load_entity_from_config).and_return(nil)
      end
      it 'should allow all with security disabled' do
        disable_security
        expect(controller).to allow_action(:delete, :destroy, params: { auth_config_id: 'foo' })
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:delete, :destroy, params: { auth_config_id: 'foo' }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:delete, :destroy, params: { auth_config_id: 'foo' }).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin, with security enabled' do
        enable_security
        login_as_admin
        expect(controller).to allow_action(:delete, :destroy, params: { auth_config_id: 'foo' })
      end

      it 'should disallow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to disallow_action(:delete, :destroy, params: { auth_config_id: 'foo' }).with(401, 'You are not authorized to perform this action.')
      end
    end
    describe 'admin' do
      before(:each) do
        enable_security
        login_as_admin
      end

      it 'should raise an error if auth_config is not found' do
        expect(@security_auth_config_service).to receive(:findProfile).and_return(nil)

        delete_with_api_header :destroy, params: { auth_config_id: 'foo' }

        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end

      it 'should render the success message on deleting a auth_config' do
        auth_config = SecurityAuthConfig.new('foo', 'cd.go.ldap')
        expect(@security_auth_config_service).to receive(:findProfile).and_return(auth_config)
        result = HttpLocalizedOperationResult.new
        allow(@security_auth_config_service).to receive(:delete).with(anything, an_instance_of(SecurityAuthConfig), result) do |user, auth_config, result|
          result.setMessage(LocalizedMessage::string('RESOURCE_DELETE_SUCCESSFUL', 'auth_config', 'foo'))
        end
        delete_with_api_header :destroy, params: { auth_config_id: 'foo' }

        expect(response).to have_api_message_response(200, "The auth_config 'foo' was deleted successfully.")
      end

      it 'should render the validation errors on failure to delete' do
        auth_config = SecurityAuthConfig.new('foo', 'cd.go.ldap')
        expect(@security_auth_config_service).to receive(:findProfile).and_return(auth_config)
        result = HttpLocalizedOperationResult.new
        allow(@security_auth_config_service).to receive(:delete).with(anything, an_instance_of(SecurityAuthConfig), result) do |user, auth_config, result|
          result.unprocessableEntity(LocalizedMessage::string('SAVE_FAILED_WITH_REASON', 'Validation failed'))
        end
        delete_with_api_header :destroy, params: { auth_config_id: 'foo' }

        expect(response).to have_api_message_response(422, 'Save failed. Validation failed')
      end
    end
  end

  describe "verify_connection" do
    describe "security" do
      it 'should allow all with security disabled' do
        disable_security

        expect(controller).to allow_action(:post, :verify_connection)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:post, :verify_connection).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:post, :verify_connection).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin, with security enabled' do
        enable_security
        login_as_admin

        expect(controller).to allow_action(:post, :verify_connection)
      end

      it 'should disallow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to disallow_action(:post, :verify_connection)
      end
    end

    describe "as_admin" do
      before(:each) do
        enable_security
        login_as_admin
      end

      it 'should verify connection for the given auth_config' do
        auth_config = SecurityAuthConfig.new('ldap', 'cd.go.ldap')
        verify_response = VerifyConnectionResponse.new('success', 'Connection check passed', nil)

        expect(@security_auth_config_service).to receive(:verify_connection).with(auth_config).and_return(verify_response)
        post_with_api_header :verify_connection, params: { auth_config: auth_config_hash }

        expect(response).to be_ok
        expect(actual_response).to eq({
                                        :status => 'success',
                                        :message => 'Connection check passed',
                                        :auth_config => expected_response(auth_config, ApiV1::Security::AuthConfigRepresenter)})
      end

      it 'should respond with errors if verify_connection fails' do
        auth_config = SecurityAuthConfig.new('ldap', 'cd.go.ldap')
        verify_response = VerifyConnectionResponse.new('failure', 'Connection check failed', nil)

        expect(@security_auth_config_service).to receive(:verify_connection).with(auth_config).and_return(verify_response)
        post_with_api_header :verify_connection, params: { auth_config: auth_config_hash }

        expect(response.code).to eq('422')
        expect(actual_response).to eq({
                                        :status => 'failure',
                                        :message => 'Connection check failed',
                                        :auth_config => expected_response(auth_config, ApiV1::Security::AuthConfigRepresenter)})
      end
    end
  end

  def auth_config_hash
    {
      id: 'ldap',
      plugin_id: 'cd.go.ldap'
    }
  end
end