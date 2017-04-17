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

require 'spec_helper'

describe ApiV1::Admin::Security::AuthConfigsController do
  include ApiHeaderSetupTeardown, ApiV1::ApiVersionHelper

  before :each do
    @security_auth_config_service = double('security_auth_config_service')
    @entity_hashing_service = double('entity_hashing_service')
    controller.stub(:entity_hashing_service).and_return(@entity_hashing_service)
    controller.stub(:security_auth_config_service).and_return(@security_auth_config_service)
  end

  describe :index do
    describe :security do
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
        @security_auth_config_service.should_receive(:listAll).and_return({'foo' => auth_config})

        get_with_api_header :index

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response([auth_config], ApiV1::Security::AuthConfigsRepresenter))
      end
    end

    describe :route do
      describe :with_header do

        it 'should route to index action of controller' do
          expect(:get => 'api/admin/security/auth_configs').to route_to(action: 'index', controller: 'api_v1/admin/security/auth_configs')
        end
      end

      describe :without_header do

        before :each do
          teardown_header
        end

        it 'should not route to index action of controller without header' do
          expect(:get => 'api/admin/security/auth_configs').to_not route_to(action: 'index', controller: 'api_v1/admin/security/auth_configs')
          expect(:get => 'api/admin/security/auth_configs').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/security/auth_configs')
        end
      end
    end
  end

  describe :show do
    describe :security do
      before :each do
        controller.stub(:load_entity_from_config).and_return(nil)
      end

      it 'should allow all with security disabled' do
        disable_security

        expect(controller).to allow_action(:get, :show, auth_config_id: 'foo')
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:get, :show, auth_config_id: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:get, :show, auth_config_id: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin, with security enabled' do
        enable_security
        login_as_admin

        expect(controller).to allow_action(:get, :show, auth_config_id: 'foo')
      end

      it 'should disallow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to disallow_action(:get, :show, auth_config_id: 'foo')
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
        @entity_hashing_service.should_receive(:md5ForEntity).with(an_instance_of(SecurityAuthConfig)).and_return('md5')
        @security_auth_config_service.should_receive(:findProfile).with('ldap').and_return(auth_config)

        get_with_api_header :show, auth_config_id: 'ldap'

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(auth_config, ApiV1::Security::AuthConfigRepresenter))
      end

      it 'should return 404 if the security auth config does not exist' do
        @security_auth_config_service.should_receive(:findProfile).with('non-existent-security-auth-config').and_return(nil)

        get_with_api_header :show, auth_config_id: 'non-existent-security-auth-config'

        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe :route do
      describe :with_header do

        it 'should route to show action of controller for alphanumeric identifier' do
          expect(:get => 'api/admin/security/auth_configs/foo123').to route_to(action: 'show', controller: 'api_v1/admin/security/auth_configs', auth_config_id: 'foo123')
        end

        it 'should route to show action of controller for identifier with dots' do
          expect(:get => 'api/admin/security/auth_configs/foo.123').to route_to(action: 'show', controller: 'api_v1/admin/security/auth_configs', auth_config_id: 'foo.123')
        end

        it 'should route to show action of controller for identifier with hyphen' do
          expect(:get => 'api/admin/security/auth_configs/foo-123').to route_to(action: 'show', controller: 'api_v1/admin/security/auth_configs', auth_config_id: 'foo-123')
        end

        it 'should route to show action of controller for identifier with underscore' do
          expect(:get => 'api/admin/security/auth_configs/foo_123').to route_to(action: 'show', controller: 'api_v1/admin/security/auth_configs', auth_config_id: 'foo_123')
        end

        it 'should route to show action of controller for capitalized identifier' do
          expect(:get => 'api/admin/security/auth_configs/FOO').to route_to(action: 'show', controller: 'api_v1/admin/security/auth_configs', auth_config_id: 'FOO')
        end
      end
      describe :without_header do
        before :each do
          teardown_header
        end
        it 'should not route to show action of controller without header' do
          expect(:get => 'api/admin/security/auth_configs/foo').to_not route_to(action: 'show', controller: 'api_v1/admin/security/auth_configs')
          expect(:get => 'api/admin/security/auth_configs/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/security/auth_configs/foo')
        end
      end
    end

  end

  describe :create do
    describe :security do
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
        controller.stub(:etag_for).and_return('some-md5')
        @security_auth_config_service.should_receive(:create).with(anything, an_instance_of(SecurityAuthConfig), an_instance_of(HttpLocalizedOperationResult))
        post_with_api_header :create, auth_config: auth_config_hash

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(auth_config, ApiV1::Security::AuthConfigRepresenter))
      end

      it 'should fail to save if there are validation errors' do
        result = double('HttpLocalizedOperationResult')
        HttpLocalizedOperationResult.stub(:new).and_return(result)
        result.stub(:isSuccessful).and_return(false)
        result.stub(:message).with(anything()).and_return('Save failed')
        result.stub(:httpCode).and_return(422)
        @security_auth_config_service.should_receive(:create).with(anything, an_instance_of(SecurityAuthConfig), result)

        post_with_api_header :create, auth_config: auth_config_hash

        expect(response).to have_api_message_response(422, 'Save failed')
      end
    end
    describe :route do
      describe :with_header do
        it 'should route to create action of controller' do
          expect(:post => 'api/admin/security/auth_configs').to route_to(action: 'create', controller: 'api_v1/admin/security/auth_configs')
        end
      end
      describe :without_header do
        before :each do
          teardown_header
        end
        it 'should not route to create action of controller without header' do
          expect(:post => 'api/admin/security/auth_configs').to_not route_to(action: 'create', controller: 'api_v1/admin/security/auth_configs')
          expect(:post => 'api/admin/security/auth_configs').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/security/auth_configs')
        end
      end
    end

  end

  describe :update do
    describe :security do
      before :each do
        controller.stub(:load_entity_from_config).and_return(nil)
        controller.stub(:check_for_stale_request).and_return(nil)
        controller.stub(:check_for_attempted_rename).and_return(nil)
      end
      it 'should allow all with security disabled' do
        disable_security
        expect(controller).to allow_action(:put, :update, auth_config_id: 'foo')
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:put, :update, auth_config_id: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:put, :update, auth_config_id: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:put, :update, auth_config_id: 'foo')
      end

      it 'should disallow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to disallow_action(:put, :update, auth_config_id: 'foo')
      end
    end
    describe 'admin' do
      before(:each) do
        enable_security
        login_as_admin
      end

      it 'should not allow rename of auth config id' do
        auth_config = SecurityAuthConfig.new('ldap', 'cd.go.ldap')
        controller.stub(:load_entity_from_config).and_return(auth_config)
        controller.stub(:check_for_stale_request).and_return(nil)

        put_with_api_header :update, auth_config_id: 'foo', auth_config: auth_config_hash

        expect(response).to have_api_message_response(422, 'Renaming of security auth config IDs is not supported by this API.')
      end

      it 'should fail update if etag does not match' do
        auth_config = SecurityAuthConfig.new('ldap', 'cd.go.ldap')
        controller.stub(:load_entity_from_config).and_return(auth_config)
        controller.stub(:etag_for).and_return('another-etag')
        controller.request.env['HTTP_IF_MATCH'] = 'some-etag'

        put_with_api_header :update, auth_config_id: 'ldap', auth_config: auth_config_hash

        expect(response).to have_api_message_response(412, "Someone has modified the configuration for Security auth config 'ldap'. Please update your copy of the config with the changes.")
      end

      it 'should proceed with update if etag matches' do
        controller.request.env['HTTP_IF_MATCH'] = %Q{"#{Digest::MD5.hexdigest('md5')}"}
        auth_config = SecurityAuthConfig.new('ldap', 'cd.go.ldap')
        controller.stub(:load_entity_from_config).twice.and_return(auth_config)

        @entity_hashing_service.should_receive(:md5ForEntity).with(an_instance_of(SecurityAuthConfig)).exactly(3).times.and_return('md5')
        @security_auth_config_service.should_receive(:update).with(anything, 'md5', an_instance_of(SecurityAuthConfig), anything)

        put_with_api_header :update, auth_config_id: 'ldap', auth_config: auth_config_hash

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(auth_config, ApiV1::Security::AuthConfigRepresenter))
      end
    end

    describe :route do
      describe :with_header do
        it 'should route to update action of controller for alphanumeric identifier' do
          expect(:put => 'api/admin/security/auth_configs/foo123').to route_to(action: 'update', controller: 'api_v1/admin/security/auth_configs', auth_config_id: 'foo123')
        end

        it 'should route to update action of controller for identifier with dots' do
          expect(:put => 'api/admin/security/auth_configs/foo.123').to route_to(action: 'update', controller: 'api_v1/admin/security/auth_configs', auth_config_id: 'foo.123')
        end

        it 'should route to update action of controller for identifier with hyphen' do
          expect(:put => 'api/admin/security/auth_configs/foo-123').to route_to(action: 'update', controller: 'api_v1/admin/security/auth_configs', auth_config_id: 'foo-123')
        end

        it 'should route to update action of controller for identifier with underscore' do
          expect(:put => 'api/admin/security/auth_configs/foo_123').to route_to(action: 'update', controller: 'api_v1/admin/security/auth_configs', auth_config_id: 'foo_123')
        end

        it 'should route to update action of controller for capitalized identifier' do
          expect(:put => 'api/admin/security/auth_configs/FOO').to route_to(action: 'update', controller: 'api_v1/admin/security/auth_configs', auth_config_id: 'FOO')
        end
      end
      describe :without_header do
        before :each do
          teardown_header
        end
        it 'should not route to update action of controller without header' do
          expect(:put => 'api/admin/security/auth_configs/foo').to_not route_to(action: 'update', controller: 'api_v1/admin/security/auth_configs')
          expect(:put => 'api/admin/security/auth_configs/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/security/auth_configs/foo')
        end
      end
    end
  end

  describe :destroy do
    describe :security do
      before :each do
        controller.stub(:load_entity_from_config).and_return(nil)
      end
      it 'should allow all with security disabled' do
        disable_security
        expect(controller).to allow_action(:delete, :destroy, auth_config_id: 'foo')
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:delete, :destroy, auth_config_id: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:delete, :destroy, auth_config_id: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin, with security enabled' do
        enable_security
        login_as_admin
        expect(controller).to allow_action(:delete, :destroy, auth_config_id: 'foo')
      end

      it 'should disallow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to disallow_action(:delete, :destroy, auth_config_id: 'foo').with(401, 'You are not authorized to perform this action.')
      end
    end
    describe 'admin' do
      before(:each) do
        enable_security
        login_as_admin
      end

      it 'should raise an error if auth_config is not found' do
        @security_auth_config_service.should_receive(:findProfile).and_return(nil)

        delete_with_api_header :destroy, auth_config_id: 'foo'

        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end

      it 'should render the success message on deleting a auth_config' do
        auth_config = SecurityAuthConfig.new('foo', 'cd.go.ldap')
        @security_auth_config_service.should_receive(:findProfile).and_return(auth_config)
        result = HttpLocalizedOperationResult.new
        @security_auth_config_service.stub(:delete).with(anything, an_instance_of(SecurityAuthConfig), result) do |user, auth_config, result|
          result.setMessage(LocalizedMessage::string('RESOURCE_DELETE_SUCCESSFUL', 'auth_config', 'foo'))
        end
        delete_with_api_header :destroy, auth_config_id: 'foo'

        expect(response).to have_api_message_response(200, "The auth_config 'foo' was deleted successfully.")
      end

      it 'should render the validation errors on failure to delete' do
        auth_config = SecurityAuthConfig.new('foo', 'cd.go.ldap')
        @security_auth_config_service.should_receive(:findProfile).and_return(auth_config)
        result = HttpLocalizedOperationResult.new
        @security_auth_config_service.stub(:delete).with(anything, an_instance_of(SecurityAuthConfig), result) do |user, auth_config, result|
          result.unprocessableEntity(LocalizedMessage::string('SAVE_FAILED_WITH_REASON', 'Validation failed'))
        end
        delete_with_api_header :destroy, auth_config_id: 'foo'

        expect(response).to have_api_message_response(422, 'Save failed. Validation failed')
      end
    end

    describe :route do
      describe :with_header do

        it 'should route to destroy action of controller for alphanumeric identifier' do
          expect(:delete => 'api/admin/security/auth_configs/foo123').to route_to(action: 'destroy', controller: 'api_v1/admin/security/auth_configs', auth_config_id: 'foo123')
        end

        it 'should route to destroy action of controller for identifier with dots' do
          expect(:delete => 'api/admin/security/auth_configs/foo.123').to route_to(action: 'destroy', controller: 'api_v1/admin/security/auth_configs', auth_config_id: 'foo.123')
        end

        it 'should route to destroy action of controller for identifier with hyphen' do
          expect(:delete => 'api/admin/security/auth_configs/foo-123').to route_to(action: 'destroy', controller: 'api_v1/admin/security/auth_configs', auth_config_id: 'foo-123')
        end

        it 'should route to destroy action of controller for identifier with underscore' do
          expect(:delete => 'api/admin/security/auth_configs/foo_123').to route_to(action: 'destroy', controller: 'api_v1/admin/security/auth_configs', auth_config_id: 'foo_123')
        end

        it 'should route to destroy action of controller for capitalized identifier' do
          expect(:delete => 'api/admin/security/auth_configs/FOO').to route_to(action: 'destroy', controller: 'api_v1/admin/security/auth_configs', auth_config_id: 'FOO')
        end
      end

      describe :without_header do
        before :each do
          teardown_header
        end
        it 'should not route to destroy action of controller without header' do
          expect(:delete => 'api/admin/security/auth_configs/foo').to_not route_to(action: 'destroy', controller: 'api_v1/admin/security/auth_configs')
          expect(:delete => 'api/admin/security/auth_configs/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/security/auth_configs/foo')
        end
      end
    end
  end

  describe :verify_connection do
    describe :security do
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

    describe :route do
      describe :with_header do
        it 'should route to verify_connection action of controller' do
          expect(:post => 'api/admin/internal/security/auth_configs/verify_connection').to route_to(action: 'verify_connection', controller: 'api_v1/admin/security/auth_configs')
        end
      end

      describe :without_header do
        before :each do
          teardown_header
        end

        it 'should not route to verify action of controller without header' do
          expect(:post => 'api/admin/internal/security/auth_configs/verify_connection').to_not route_to(action: 'verify_connection', controller: 'api_v1/admin/security/auth_configs')
          expect(:post => 'api/admin/internal/security/auth_configs/verify_connection').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/internal/security/auth_configs/verify_connection')
        end
      end
    end

    describe :as_admin do
      before(:each) do
        enable_security
        login_as_admin
      end

      it 'should verify connection for the given auth_config' do
        auth_config = SecurityAuthConfig.new('ldap', 'cd.go.ldap')
        verify_response = VerifyConnectionResponse.new('success', 'Connection check passed', nil)

        @security_auth_config_service.should_receive(:verify_connection).with(auth_config).and_return(verify_response)
        post_with_api_header :verify_connection, {auth_config: auth_config_hash}

        expect(response).to be_ok
        expect(actual_response).to eq({
                                        :status => 'success',
                                        :message => 'Connection check passed',
                                        :auth_config => expected_response(auth_config, ApiV1::Security::AuthConfigRepresenter)})
      end

      it 'should respond with errors if verify_connection fails' do
        auth_config = SecurityAuthConfig.new('ldap', 'cd.go.ldap')
        verify_response = VerifyConnectionResponse.new('failure', 'Connection check failed', nil)

        @security_auth_config_service.should_receive(:verify_connection).with(auth_config).and_return(verify_response)
        post_with_api_header :verify_connection, {auth_config: auth_config_hash}

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