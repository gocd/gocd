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

describe ApiV1::Admin::PluginSettingsController do
  include ApiHeaderSetupTeardown, ApiV1::ApiVersionHelper

  before :each do
    @plugin_settings = PluginSettings.new("plugin.id.1")
    @plugin_settings.setPluginSettingsProperties([ConfigurationProperty.new(ConfigurationKey.new('username'), ConfigurationValue.new('admin'))])
    @plugin_service = double('plugin_service')
    @entity_hashing_service = double('entity_hashing_service')
    controller.stub(:plugin_service).and_return(@plugin_service)
    controller.stub(:entity_hashing_service).and_return(@entity_hashing_service)
  end

  describe 'show' do
    describe 'authorization_check' do
      before :each do
        controller.stub(:load_plugin_settings).and_return(nil)
      end

      it 'should allow all with security disabled' do
        disable_security

        expect(controller).to allow_action(:get, :show)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:get, :show, plugin_id: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:get, :show, plugin_id: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow group admin users, with security enabled' do
        enable_security
        login_as_group_admin

        expect(controller).to disallow_action(:get, :show, plugin_id: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow template users, with security enabled' do
        enable_security
        login_as_template_admin

        expect(controller).to disallow_action(:get, :show, plugin_id: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin, with security enabled' do
        enable_security
        login_as_admin

        expect(controller).to allow_action(:get, :show)
      end

    end

    describe 'admin' do

      before(:each) do
        enable_security
        login_as_admin
      end

      it 'should render the plugin settings for a specified plugin id' do
        @plugin_service.should_receive(:getPluginSettings).with('plugin.id.1').and_return(@plugin_settings)
        @entity_hashing_service.should_receive(:md5ForEntity).with(@plugin_settings).and_return('md5')

        get_with_api_header :show, plugin_id: 'plugin.id.1'

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response({plugin_settings: @plugin_settings}, ApiV1::Config::PluginSettingsRepresenter))
      end

      it 'should render 404 for a non existent plugin' do
        @plugin_service.should_receive(:getPluginSettings).with('plugin.id.2').and_return(nil)

        get_with_api_header :show, plugin_id: 'plugin.id.2'

        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end

    end

    describe :route do
      describe :with_header do
        it 'should route to show action of plugin_settings controller for plugin id with dots' do
          expect(:get => 'api/admin/plugin_settings/foo.bar').to route_to(action: 'show', controller: 'api_v1/admin/plugin_settings', plugin_id: 'foo.bar')
        end
      end
      describe :without_header do
        before :each do
          teardown_header
        end
        it 'should not route to show action of plugin_settings controller without header' do
          expect(:get => 'api/admin/plugin_settings/foo').to_not route_to(action: 'show', controller: 'api_v1/admin/plugin_settings')
          expect(:get => 'api/admin/plugin_settings/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/plugin_settings/foo')
        end
      end
    end
  end

  describe 'create' do
    describe 'authorization_check' do
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

      it 'should disallow group admin users, with security enabled' do
        enable_security
        login_as_group_admin

        expect(controller).to disallow_action(:post, :create).with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow template users, with security enabled' do
        enable_security
        login_as_template_admin

        expect(controller).to disallow_action(:post, :create).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin, with security enabled' do
        enable_security
        login_as_admin

        expect(controller).to allow_action(:post, :create)
      end
    end

    describe 'admin' do
      before(:each) do
        enable_security
        login_as_admin
        @default_plugin_info_finder = double('default_plugin_info_finder')
        controller.stub(:default_plugin_info_finder).and_return(@default_plugin_info_finder)
        @default_plugin_info_finder.should_receive(:pluginInfoFor).with('plugin.id.2').exactly(2).times.and_return(com.thoughtworks.go.plugin.domain.configrepo.ConfigRepoPluginInfo.new(nil, com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('url', nil), com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('password', nil)])))
      end

      it 'should deserialize plugin settings from given object' do
        @entity_hashing_service.stub(:md5ForEntity).and_return("some-md5")
        hash = {plugin_id: 'plugin.id.2',configuration: [{"key" => 'url', "value" => 'git@github.com:foo/bar.git'}, {"key" => 'password', "value" => "some-value"}]}
        @plugin_service.should_receive(:createPluginSettings).with(anything, anything, an_instance_of(PluginSettings))

        post_with_api_header :create, plugin_setting: hash

        expect(response).to be_ok
        real_response = actual_response
        real_response.delete(:_links)
        expect(real_response).to eq(hash)

      end

      it 'should fail save if validation has failed' do
        hash = {plugin_id: 'plugin.id.2'}
        result = double('HttpLocalizedOperationResult')
        HttpLocalizedOperationResult.stub(:new).and_return(result)
        result.stub(:isSuccessful).and_return(false)
        result.stub(:message).with(anything).and_return("Save failed")
        result.stub(:httpCode).and_return(422)
        @plugin_service.should_receive(:createPluginSettings).with(anything, result, an_instance_of(PluginSettings))

        post_with_api_header :create, plugin_setting: hash

        expect(response).to have_api_message_response(422, "Save failed")
      end

    end

    describe :route do
      describe :with_header do
        it 'should route to show action of plugin_settings controller for plugin id with dots' do
          expect(:post => 'api/admin/plugin_settings').to route_to(action: 'create', controller: 'api_v1/admin/plugin_settings')
        end
      end
      describe :without_header do
        before :each do
          teardown_header
        end
        it 'should not route to show action of plugin_settings controller without header' do
          expect(:post => 'api/admin/plugin_settings').to_not route_to(action: 'create', controller: 'api_v1/admin/plugin_settings')
          expect(:post => 'api/admin/plugin_settings').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/plugin_settings')
        end
      end
    end
  end

  describe 'update' do
    describe 'authorization_check' do
      before :each do
        controller.stub(:load_plugin_settings).and_return(nil)
        controller.stub(:check_for_stale_request).and_return(nil)
      end
      it 'should allow all with security disabled' do
        disable_security

        expect(controller).to allow_action(:put, :update)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:put, :update, plugin_id: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:put, :update, plugin_id: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow group admin users, with security enabled' do
        enable_security
        login_as_group_admin

        expect(controller).to disallow_action(:put, :update, plugin_id: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should disallow template users, with security enabled' do
        enable_security
        login_as_template_admin

        expect(controller).to disallow_action(:put, :update, plugin_id: 'foo').with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin, with security enabled' do
        enable_security
        login_as_admin

        expect(controller).to allow_action(:put, :update)
      end
    end

    describe 'admin' do
      before :each do
        enable_security
        login_as_admin
      end

      it 'should not proceed with update if validation has failed' do
        @default_plugin_info_finder = double('default_plugin_info_finder')
        controller.stub(:default_plugin_info_finder).and_return(@default_plugin_info_finder)
        @default_plugin_info_finder.should_receive(:pluginInfoFor).with('plugin.id.1').exactly(2).times.and_return(com.thoughtworks.go.plugin.domain.configrepo.ConfigRepoPluginInfo.new(nil, com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('url', nil), com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('password', nil)])))
        controller.stub(:check_for_stale_request)
        hash = {plugin_id: 'plugin.id.1',configuration: [{"key" => 'url', "value" => 'git@github.com:foo/bagdgr.git'}, {"key" => 'password', "value" => "some-value"}]}

        result = double('HttpLocalizedOperationResult')
        HttpLocalizedOperationResult.stub(:new).and_return(result)
        result.stub(:isSuccessful).and_return(false)
        result.stub(:message).with(anything).and_return("Save failed")
        result.stub(:httpCode).and_return(422)

        @entity_hashing_service.should_receive(:md5ForEntity).with(an_instance_of(PluginSettings)).and_return('md5')
        @plugin_service.should_receive(:getPluginSettings).with('plugin.id.1').and_return(@plugin_settings)
        @plugin_service.should_receive(:updatePluginSettings).with(anything, result, an_instance_of(PluginSettings), "md5")

        put_with_api_header :update, plugin_id: 'plugin.id.1', plugin_setting: hash

        expect(response).to have_api_message_response(422, "Save failed")
      end

      it 'should fail update if etag does not match' do
        controller.request.env['HTTP_IF_MATCH'] = "some-etag"
        hash = {plugin_id: 'plugin.id.1',configuration: [{"key" => 'url', "value" => 'git@github.com:foo/bagdgr.git'}, {"key" => 'password', "value" => "some-value"}]}
        @entity_hashing_service.should_receive(:md5ForEntity).with(an_instance_of(PluginSettings)).and_return('another-etag')
        @plugin_service.should_receive(:getPluginSettings).with('plugin.id.1').and_return(@plugin_settings)

        put_with_api_header :update, plugin_id: 'plugin.id.1', plugin_setting: hash

        expect(response).to have_api_message_response(412, "Someone has modified the configuration for Plugin Settings 'plugin.id.1'. Please update your copy of the config with the changes." )
      end

      it 'should proceed with update if etag matches.' do
        @default_plugin_info_finder = double('default_plugin_info_finder')
        controller.stub(:default_plugin_info_finder).and_return(@default_plugin_info_finder)
        @default_plugin_info_finder.should_receive(:pluginInfoFor).with('plugin.id.1').exactly(2).times.and_return(com.thoughtworks.go.plugin.domain.configrepo.ConfigRepoPluginInfo.new(nil, com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('url', nil), com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('password', nil)])))
        controller.request.env['HTTP_IF_MATCH'] = "\"#{Digest::MD5.hexdigest("md5")}\""
        hash = {plugin_id: 'plugin.id.1',configuration: [{"key" => 'url', "value" => 'git@github.com:foo/bar.git'}, {"key" => 'password', "value" => "some-value"}]}

        @entity_hashing_service.should_receive(:md5ForEntity).with(an_instance_of(PluginSettings)).exactly(3).times.and_return('md5')
        @plugin_service.should_receive(:getPluginSettings).with('plugin.id.1').and_return(@plugin_settings)
        @plugin_service.should_receive(:updatePluginSettings).with(anything, anything, an_instance_of(PluginSettings), "md5")

        put_with_api_header :update, plugin_id: 'plugin.id.1', plugin_setting: hash

        expect(response).to be_ok
        real_response = actual_response
        real_response.delete(:_links)
        expect(real_response).to eq(hash)
      end
    end

    describe :route do
      describe :with_header do
        it 'should route to show action of plugin_settings controller for plugin id with dots' do
          expect(:put => 'api/admin/plugin_settings/foo.bar').to route_to(action: 'update', controller: 'api_v1/admin/plugin_settings', plugin_id: 'foo.bar')
        end
      end
      describe :without_header do
        before :each do
          teardown_header
        end
        it 'should not route to show action of plugin_settings controller without header' do
          expect(:put => 'api/admin/plugin_settings/foo').to_not route_to(action: 'update', controller: 'api_v1/admin/plugin_settings')
          expect(:put => 'api/admin/plugin_settings/foo').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/plugin_settings/foo')
        end
      end
    end
  end

end