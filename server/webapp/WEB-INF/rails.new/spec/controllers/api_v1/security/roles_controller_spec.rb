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

describe ApiV1::Admin::Security::RolesController do
  include ApiHeaderSetupTeardown, ApiV1::ApiVersionHelper

  before :each do
    @service = double('role_config_service')
    @entity_hashing_service = double('entity_hashing_service')
    controller.stub(:entity_hashing_service).and_return(@entity_hashing_service)
    controller.stub(:role_config_service).and_return(@service)
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
        enable_security
        login_as_group_admin
        expect(controller).to disallow_action(:get, :index)
      end
    end

    describe 'admin' do
      before :each do
        enable_security
        login_as_admin
      end

      it 'should list all security auth configs' do
        role = PluginRoleConfig.new('foo', 'ldap')
        @service.should_receive(:listAll).and_return([role])

        get_with_api_header :index

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response([role], ApiV1::Security::RolesConfigRepresenter))
      end

      it 'should list roles by type' do
        plugin_role_config = PluginRoleConfig.new('foo', 'ldap')
        gocd_role_config = RoleConfig.new(CaseInsensitiveString.new('bar'))

        @service.should_receive(:listAll).and_return([plugin_role_config, gocd_role_config])

        get_with_api_header :index, type: 'plugin'

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response([plugin_role_config], ApiV1::Security::RolesConfigRepresenter))
      end

      it 'should error out if listing roles by a wrong type' do
        get_with_api_header :index, type: 'invalid'

        expect(response).to have_api_message_response(400, 'Bad role type `invalid`. Valid values are `gocd` and `plugin`')
      end
    end

    describe :route do
      describe :with_header do

        it 'should route to index action of controller' do
          expect(:get => 'api/admin/security/roles').to route_to(action: 'index', controller: 'api_v1/admin/security/roles')
        end
      end

      describe :without_header do

        before :each do
          teardown_header
        end

        it 'should not route to index action of controller without header' do
          expect(:get => 'api/admin/security/roles').to_not route_to(action: 'index', controller: 'api_v1/admin/security/roles')
          expect(:get => 'api/admin/security/roles').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/security/roles')
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

        expect(controller).to allow_action(:get, :show, role_name: 'foo')
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:get, :show, role_name: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:get, :show, role_name: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin, with security enabled' do
        enable_security
        login_as_admin

        expect(controller).to allow_action(:get, :show, role_name: 'foo')
      end

      it 'should disallow pipeline group admin users, with security enabled' do
        enable_security
        login_as_group_admin
        expect(controller).to disallow_action(:get, :show, role_name: 'foo')
      end
    end

    describe 'admin' do
      before(:each) do
        enable_security
        login_as_admin
        @result = HttpLocalizedOperationResult.new
      end

      it 'should render the security auth config of specified name' do
        role = PluginRoleConfig.new('blackbird', 'ldap')
        @entity_hashing_service.should_receive(:md5ForEntity).with(an_instance_of(PluginRoleConfig)).and_return('md5')
        @service.should_receive(:findRole).with('blackbird').and_return(role)

        get_with_api_header :show, role_name: 'blackbird'

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(role, ApiV1::Security::RoleConfigRepresenter))
      end

      it 'should return 404 if the security auth config does not exist' do
        @service.should_receive(:findRole).with('non-existent-security-auth-config').and_return(nil)

        get_with_api_header :show, role_name: 'non-existent-security-auth-config'

        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end
    end

    describe :route do
      describe :with_header do

        it 'should route to show action of controller for alphanumeric identifier' do
          expect(:get => 'api/admin/security/roles/foo123').to route_to(action: 'show', controller: 'api_v1/admin/security/roles', role_name: 'foo123')
        end

        it 'should route to show action of controller for identifier with dots' do
          expect(:get => 'api/admin/security/roles/foo.123').to route_to(action: 'show', controller: 'api_v1/admin/security/roles', role_name: 'foo.123')
        end

        it 'should route to show action of controller for identifier with hyphen' do
          expect(:get => 'api/admin/security/roles/foo-123').to route_to(action: 'show', controller: 'api_v1/admin/security/roles', role_name: 'foo-123')
        end

        it 'should route to show action of controller for identifier with underscore' do
          expect(:get => 'api/admin/security/roles/foo_123').to route_to(action: 'show', controller: 'api_v1/admin/security/roles', role_name: 'foo_123')
        end

        it 'should route to show action of controller for capitalized identifier' do
          expect(:get => 'api/admin/security/roles/FOO').to route_to(action: 'show', controller: 'api_v1/admin/security/roles', role_name: 'FOO')
        end
      end
      describe :without_header do
        before :each do
          teardown_header
        end
        it 'should not route to show action of controller without header' do
          expect(:get => 'api/admin/security/roles/foo').to_not route_to(action: 'show', controller: 'api_v1/admin/security/roles')
          expect(:get => 'api/admin/security/roles/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/security/roles/foo')
        end
      end
    end

  end

  describe :create do
    describe :security do
      before :each do
        @service.stub(:findRole).with(anything).and_return(nil)
      end

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
        enable_security
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
        role = PluginRoleConfig.new('blackbird', 'blackbird', ConfigurationPropertyMother.create('foo', false, 'bar'))
        controller.stub(:etag_for).and_return('some-md5')
        @service.should_receive(:findRole).with(anything).and_return(nil)
        @service.should_receive(:create).with(anything, an_instance_of(PluginRoleConfig), an_instance_of(HttpLocalizedOperationResult))
        post_with_api_header :create, role: plugin_role_hash

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(role, ApiV1::Security::RoleConfigRepresenter))
      end

      it 'should fail to save if there are validation errors' do
        result = double('HttpLocalizedOperationResult')
        HttpLocalizedOperationResult.stub(:new).and_return(result)
        result.stub(:isSuccessful).and_return(false)
        result.stub(:message).with(anything()).and_return('Save failed')
        result.stub(:httpCode).and_return(422)

        @service.should_receive(:findRole).with(anything).and_return(nil)
        @service.should_receive(:create).with(anything, an_instance_of(RoleConfig), result)

        post_with_api_header :create, role: role_hash

        expect(response).to have_api_message_response(422, 'Save failed')
      end

      it 'should check for existence of role with same name' do
        @service.should_receive(:findRole).with('blackbird').and_return(PluginRoleConfig.new)

        post_with_api_header :create, role: role_hash

        expected_role = RoleConfig.new(CaseInsensitiveString.new('blackbird'), RoleUser.new('bob'), RoleUser.new('alice'))
        expected_role.addError('name', 'Role names should be unique. Role with the same name exists.')

        expect(@service).not_to receive(:create)
        expect(actual_response[:data]).to eq(expected_response(expected_role, ApiV1::Security::RoleConfigRepresenter))
        expect(response).to have_api_message_response(422, "Failed to add role. The role 'blackbird' already exists.")
      end
    end

    describe :route do
      describe :with_header do
        it 'should route to create action of controller' do
          expect(:post => 'api/admin/security/roles').to route_to(action: 'create', controller: 'api_v1/admin/security/roles')
        end
      end
      describe :without_header do
        before :each do
          teardown_header
        end
        it 'should not route to create action of controller without header' do
          expect(:post => 'api/admin/security/roles').to_not route_to(action: 'create', controller: 'api_v1/admin/security/roles')
          expect(:post => 'api/admin/security/roles').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/security/roles')
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
        expect(controller).to allow_action(:put, :update, role_name: 'foo')
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:put, :update, role_name: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:put, :update, role_name: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:put, :update, role_name: 'foo')
      end

      it 'should disallow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to disallow_action(:put, :update, role_name: 'foo').with(401, 'You are not authorized to perform this action.')
      end
    end
    describe 'admin' do
      before(:each) do
        login_as_admin
      end

      it 'should not allow rename of auth config id' do
        role = PluginRoleConfig.new('blackbird', 'ldap')
        controller.stub(:load_entity_from_config).and_return(role)
        controller.stub(:check_for_stale_request).and_return(nil)

        put_with_api_header :update, role_name: 'foo', role: plugin_role_hash

        expect(response).to have_api_message_response(422, 'Renaming of roles is not supported by this API.')
      end

      it 'should fail update if etag does not match' do
        role = PluginRoleConfig.new('blackbird', 'ldap')
        controller.stub(:load_entity_from_config).and_return(role)
        controller.stub(:etag_for).and_return('another-etag')
        controller.request.env['HTTP_IF_MATCH'] = 'some-etag'

        put_with_api_header :update, role_name: 'blackbird', role: plugin_role_hash

        expect(response).to have_api_message_response(412, "Someone has modified the configuration for role 'blackbird'. Please update your copy of the config with the changes.")
      end

      it 'should proceed with update if etag matches' do
        controller.request.env['HTTP_IF_MATCH'] = %Q{"#{Digest::MD5.hexdigest('md5')}"}
        role = PluginRoleConfig.new('blackbird', 'blackbird', ConfigurationPropertyMother.create('foo', false, 'bar'))
        controller.stub(:load_entity_from_config).twice.and_return(role)

        @entity_hashing_service.should_receive(:md5ForEntity).with(an_instance_of(PluginRoleConfig)).exactly(3).times.and_return('md5')
        @service.should_receive(:update).with(anything, 'md5', an_instance_of(PluginRoleConfig), anything)

        put_with_api_header :update, role_name: 'blackbird', role: plugin_role_hash

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response(role, ApiV1::Security::RoleConfigRepresenter))
      end
    end

    describe :route do
      describe :with_header do
        it 'should route to update action of controller for alphanumeric identifier' do
          expect(:put => 'api/admin/security/roles/foo123').to route_to(action: 'update', controller: 'api_v1/admin/security/roles', role_name: 'foo123')
        end

        it 'should route to update action of controller for identifier with dots' do
          expect(:put => 'api/admin/security/roles/foo.123').to route_to(action: 'update', controller: 'api_v1/admin/security/roles', role_name: 'foo.123')
        end

        it 'should route to update action of controller for identifier with hyphen' do
          expect(:put => 'api/admin/security/roles/foo-123').to route_to(action: 'update', controller: 'api_v1/admin/security/roles', role_name: 'foo-123')
        end

        it 'should route to update action of controller for identifier with underscore' do
          expect(:put => 'api/admin/security/roles/foo_123').to route_to(action: 'update', controller: 'api_v1/admin/security/roles', role_name: 'foo_123')
        end

        it 'should route to update action of controller for capitalized identifier' do
          expect(:put => 'api/admin/security/roles/FOO').to route_to(action: 'update', controller: 'api_v1/admin/security/roles', role_name: 'FOO')
        end
      end
      describe :without_header do
        before :each do
          teardown_header
        end
        it 'should not route to update action of controller without header' do
          expect(:put => 'api/admin/security/roles/foo').to_not route_to(action: 'update', controller: 'api_v1/admin/security/roles')
          expect(:put => 'api/admin/security/roles/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/security/roles/foo')
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
        expect(controller).to allow_action(:delete, :destroy)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:delete, :destroy, role_name: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        login_as_user
        expect(controller).to disallow_action(:delete, :destroy, role_name: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:delete, :destroy, role_name: 'foo')
      end

      it 'should disallow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to disallow_action(:delete, :destroy, role_name: 'foo').with(401, 'You are not authorized to perform this action.')
      end
    end
    describe 'admin' do
      before(:each) do
        enable_security
        login_as_admin
      end

      it 'should raise an error if role is not found' do
        @service.should_receive(:findRole).and_return(nil)

        delete_with_api_header :destroy, role_name: 'foo'

        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end

      it 'should render the success message on deleting a role' do
        role = PluginRoleConfig.new('foo', 'ldap')
        @service.should_receive(:findRole).and_return(role)
        result = HttpLocalizedOperationResult.new
        @service.stub(:delete).with(anything, an_instance_of(PluginRoleConfig), result) do |user, role, result|
          result.setMessage(LocalizedMessage::string('RESOURCE_DELETE_SUCCESSFUL', 'role', 'foo'))
        end
        delete_with_api_header :destroy, role_name: 'foo'

        expect(response).to have_api_message_response(200, "The role 'foo' was deleted successfully.")
      end

      it 'should render the validation errors on failure to delete' do
        role = PluginRoleConfig.new('foo', 'ldap')
        @service.should_receive(:findRole).and_return(role)
        result = HttpLocalizedOperationResult.new
        @service.stub(:delete).with(anything, an_instance_of(PluginRoleConfig), result) do |user, role, result|
          result.unprocessableEntity(LocalizedMessage::string('SAVE_FAILED_WITH_REASON', 'Validation failed'))
        end
        delete_with_api_header :destroy, role_name: 'foo'

        expect(response).to have_api_message_response(422, 'Save failed. Validation failed')
      end
    end

    describe :route do
      describe :with_header do

        it 'should route to destroy action of controller for alphanumeric identifier' do
          expect(:delete => 'api/admin/security/roles/foo123').to route_to(action: 'destroy', controller: 'api_v1/admin/security/roles', role_name: 'foo123')
        end

        it 'should route to destroy action of controller for identifier with dots' do
          expect(:delete => 'api/admin/security/roles/foo.123').to route_to(action: 'destroy', controller: 'api_v1/admin/security/roles', role_name: 'foo.123')
        end

        it 'should route to destroy action of controller for identifier with hyphen' do
          expect(:delete => 'api/admin/security/roles/foo-123').to route_to(action: 'destroy', controller: 'api_v1/admin/security/roles', role_name: 'foo-123')
        end

        it 'should route to destroy action of controller for identifier with underscore' do
          expect(:delete => 'api/admin/security/roles/foo_123').to route_to(action: 'destroy', controller: 'api_v1/admin/security/roles', role_name: 'foo_123')
        end

        it 'should route to destroy action of controller for capitalized identifier' do
          expect(:delete => 'api/admin/security/roles/FOO').to route_to(action: 'destroy', controller: 'api_v1/admin/security/roles', role_name: 'FOO')
        end
      end

      describe :without_header do
        before :each do
          teardown_header
        end
        it 'should not route to destroy action of controller without header' do
          expect(:delete => 'api/admin/security/roles/foo').to_not route_to(action: 'destroy', controller: 'api_v1/admin/security/roles')
          expect(:delete => 'api/admin/security/roles/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/security/roles/foo')
        end
      end
    end
  end

  def role_hash
    {
      name: 'blackbird',
      type: 'gocd',
      attributes: {
        users: %w(bob alice)
      }
    }
  end

  def plugin_role_hash
    {
      name: 'blackbird',
      type: 'plugin',
      attributes: {
        auth_config_id: 'blackbird',
        properties: [{key: 'foo', value: 'bar'}]
      }
    }
  end
end