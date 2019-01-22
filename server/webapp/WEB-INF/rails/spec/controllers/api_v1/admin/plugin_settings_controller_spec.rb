##########################################################################
# Copyright 2018 ThoughtWorks, Inc.
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

describe ApiV1::Admin::PluginSettingsController do
  include ApiHeaderSetupForRouting
  include ApiV1::ApiVersionHelper

  before :each do
    @plugin_info = com.thoughtworks.go.plugin.domain.configrepo.ConfigRepoPluginInfo.new(nil, nil, com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('username', nil)]))
    @plugin_settings = PluginSettings.new("plugin.id.1")
    @plugin_settings.addConfigurations(@plugin_info, [ConfigurationProperty.new(ConfigurationKey.new('username'), ConfigurationValue.new('admin'))])
    @plugin_service = double('plugin_service')
    @entity_hashing_service = double('entity_hashing_service')
    allow(controller).to receive(:plugin_service).and_return(@plugin_service)
    allow(controller).to receive(:entity_hashing_service).and_return(@entity_hashing_service)
  end

  describe 'show' do
    describe 'authorization_check' do
      before :each do
        allow(controller).to receive(:load_plugin_settings).and_return(nil)
      end

      it 'should allow all with security disabled' do
        disable_security

        expect(controller).to allow_action(:get, :show)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:get, :show, params: {plugin_id: 'foo'}).with(403, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:get, :show, params: {plugin_id: 'foo'}).with(403, 'You are not authorized to perform this action.')
      end

      it 'should disallow group admin users, with security enabled' do
        enable_security
        login_as_group_admin

        expect(controller).to disallow_action(:get, :show, params: {plugin_id: 'foo'}).with(403, 'You are not authorized to perform this action.')
      end

      it 'should disallow template users, with security enabled' do
        enable_security
        login_as_template_admin

        expect(controller).to disallow_action(:get, :show, params: {plugin_id: 'foo'}).with(403, 'You are not authorized to perform this action.')
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
        expect(@plugin_service).to receive(:getPluginSettings).with('plugin.id.1').and_return(@plugin_settings)
        expect(@plugin_service).to receive(:isPluginLoaded).with('plugin.id.1').and_return(true)
        expect(@plugin_service).to receive(:pluginInfoForExtensionThatHandlesPluginSettings).with('plugin.id.1').and_return(@plugin_info)
        expect(@entity_hashing_service).to receive(:md5ForEntity).with(@plugin_settings).and_return('md5')

        get_with_api_header :show, params:{plugin_id: 'plugin.id.1'}

        expect(response).to be_ok
        expect(actual_response).to eq(expected_response({plugin_settings: @plugin_settings, plugin_info: @plugin_info}, ApiV1::Config::PluginSettingsRepresenter))
      end

      it 'should render 404 for non existent plugin settings' do
        expect(@plugin_service).to receive(:getPluginSettings).with('plugin.id.2').and_return(nil)

        get_with_api_header :show, params:{plugin_id: 'plugin.id.2'}

        expect(response).to have_api_message_response(404, 'Either the resource you requested was not found, or you are not authorized to perform this action.')
      end

      it 'should render 424 for a non existent plugin' do
        expect(@plugin_service).to receive(:isPluginLoaded).with('plugin.id.1').and_return(false)
        expect(@plugin_service).to receive(:getPluginSettings).with('plugin.id.1').and_return(@plugin_settings)
        get_with_api_header :show, params:{plugin_id: 'plugin.id.1'}

        expect(response).to have_api_message_response(424, 'Your request could not be processed. The plugin with id \'plugin.id.1\' is not loaded.')
      end
    end

    xdescribe 'route' do
      describe "with_header" do
        it 'should route to show action of plugin_settings controller for plugin id with dots' do
          expect(:get => 'api/admin/plugin_settings/foo.bar').to route_to(action: 'show', controller: 'api_v1/admin/plugin_settings', plugin_id: 'foo.bar')
        end
      end
      describe "without_header" do
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

        expect(controller).to disallow_action(:post, :create).with(403, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:post, :create).with(403, 'You are not authorized to perform this action.')
      end

      it 'should disallow group admin users, with security enabled' do
        enable_security
        login_as_group_admin

        expect(controller).to disallow_action(:post, :create).with(403, 'You are not authorized to perform this action.')
      end

      it 'should disallow template users, with security enabled' do
        enable_security
        login_as_template_admin

        expect(controller).to disallow_action(:post, :create).with(403, 'You are not authorized to perform this action.')
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
        @plugin_service = double('plugin_service')
        allow(controller).to receive(:plugin_service).and_return(@plugin_service)
        allow(@plugin_service).to receive(:pluginInfoForExtensionThatHandlesPluginSettings).with('plugin.id.2').exactly(2).times.and_return(com.thoughtworks.go.plugin.domain.configrepo.ConfigRepoPluginInfo.new(nil, nil, com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new(
          [
            com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('url', nil),
            com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('password', nil)
          ])))
      end

      it 'should deserialize plugin settings from given object' do
        allow(@entity_hashing_service).to receive(:md5ForEntity).and_return("some-md5")
        hash = {plugin_id: 'plugin.id.2', configuration: [{:key => 'url', :value => 'git@github.com:foo/bar.git'}, {:key => 'password', :value => "some-value"}]}
        expect(@plugin_service).to receive(:isPluginLoaded).with('plugin.id.2').and_return(true)
        expect(@plugin_service).to receive(:isPluginLoaded).with('plugin.id.2').and_return(true)
        expect(@plugin_service).to receive(:createPluginSettings).with(an_instance_of(PluginSettings), anything, anything)

        post_with_api_header :create, params:{plugin_setting: hash}

        expect(response).to be_ok
        real_response = actual_response
        real_response.delete(:_links)
        expect(real_response).to eq(hash)

      end

      it 'should fail save if validation has failed' do
        hash = {plugin_id: 'plugin.id.2'}
        result = double('HttpLocalizedOperationResult')
        allow(HttpLocalizedOperationResult).to receive(:new).and_return(result)
        allow(result).to receive(:isSuccessful).and_return(false)
        allow(result).to receive(:message).and_return("Save failed")
        allow(result).to receive(:httpCode).and_return(422)
        expect(@plugin_service).to receive(:isPluginLoaded).with('plugin.id.2').and_return(true)
        expect(@plugin_service).to receive(:isPluginLoaded).with('plugin.id.2').and_return(true)
        expect(@plugin_service).to receive(:createPluginSettings).with(an_instance_of(PluginSettings), anything, result)

        post_with_api_header :create, params:{plugin_setting: hash}

        expect(response).to have_api_message_response(422, "Save failed")
      end

      it 'should render 424 for a non existent plugin' do
        hash = {plugin_id: 'plugin.id.1'}
        expect(@plugin_service).to receive(:isPluginLoaded).with('plugin.id.1').and_return(false)

        post_with_api_header :create, params:{plugin_setting: hash}

        expect(response).to have_api_message_response(424, 'Your request could not be processed. The plugin with id \'plugin.id.1\' is not loaded.')
      end
    end

    xdescribe 'route' do
      describe "with_header" do
        it 'should route to show action of plugin_settings controller for plugin id with dots' do
          expect(:post => 'api/admin/plugin_settings').to route_to(action: 'create', controller: 'api_v1/admin/plugin_settings')
        end
      end
      describe "without_header" do
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
        allow(controller).to receive(:load_plugin_settings).and_return(nil)
        allow(controller).to receive(:check_for_stale_request).and_return(nil)
      end
      it 'should allow all with security disabled' do
        disable_security

        expect(controller).to allow_action(:put, :update)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:put, :update, params: {plugin_id: 'foo'}).with(403, 'You are not authorized to perform this action.')
      end

      it 'should disallow normal users, with security enabled' do
        enable_security
        login_as_user

        expect(controller).to disallow_action(:put, :update, params: {plugin_id: 'foo'}).with(403, 'You are not authorized to perform this action.')
      end

      it 'should disallow group admin users, with security enabled' do
        enable_security
        login_as_group_admin

        expect(controller).to disallow_action(:put, :update, params: {plugin_id: 'foo'}).with(403, 'You are not authorized to perform this action.')
      end

      it 'should disallow template users, with security enabled' do
        enable_security
        login_as_template_admin

        expect(controller).to disallow_action(:put, :update, params: {plugin_id: 'foo'}).with(403, 'You are not authorized to perform this action.')
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
        @plugin_service = double('plugin_service')
        allow(controller).to receive(:plugin_service).and_return(@plugin_service)

        expect(@plugin_service).to receive(:pluginInfoForExtensionThatHandlesPluginSettings).with('plugin.id.1').exactly(2).times.and_return(
          com.thoughtworks.go.plugin.domain.configrepo.ConfigRepoPluginInfo.new(nil, nil, com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new(
            [com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('url', nil), com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('password', nil)])))
        allow(controller).to receive(:check_for_stale_request)
        hash = {plugin_id: 'plugin.id.1', configuration: [{"key" => 'url', "value" => 'git@github.com:foo/bagdgr.git'}, {"key" => 'password', "value" => "some-value"}]}

        result = double('HttpLocalizedOperationResult')
        allow(HttpLocalizedOperationResult).to receive(:new).and_return(result)
        allow(result).to receive(:isSuccessful).and_return(false)
        allow(result).to receive(:message).and_return("Save failed")
        allow(result).to receive(:httpCode).and_return(422)

        expect(@plugin_service).to receive(:isPluginLoaded).with('plugin.id.1').and_return(true)
        expect(@plugin_service).to receive(:isPluginLoaded).with('plugin.id.1').and_return(true)
        expect(@entity_hashing_service).to receive(:md5ForEntity).with(an_instance_of(PluginSettings)).and_return('md5')
        expect(@plugin_service).to receive(:getPluginSettings).with('plugin.id.1').and_return(@plugin_settings)
        expect(@plugin_service).to receive(:updatePluginSettings).with(an_instance_of(PluginSettings), anything, result, "md5")

        put_with_api_header :update, params:{plugin_id: 'plugin.id.1', plugin_setting: hash}

        expect(response).to have_api_message_response(422, "Save failed")
      end

      it 'should fail update if etag does not match' do
        controller.request.env['HTTP_IF_MATCH'] = "some-etag"
        hash = {plugin_id: 'plugin.id.1', configuration: [{"key" => 'url', "value" => 'git@github.com:foo/bagdgr.git'}, {"key" => 'password', "value" => "some-value"}]}
        expect(@entity_hashing_service).to receive(:md5ForEntity).with(an_instance_of(PluginSettings)).and_return('another-etag')
        expect(@plugin_service).to receive(:getPluginSettings).with('plugin.id.1').and_return(@plugin_settings)

        put_with_api_header :update, params:{plugin_id: 'plugin.id.1', plugin_setting: hash}

        expect(response).to have_api_message_response(412, "Someone has modified the configuration for Plugin Settings 'plugin.id.1'. Please update your copy of the config with the changes.")
      end

      it 'should proceed with update if etag matches.' do
        @plugin_service = double('plugin_service')
        allow(controller).to receive(:plugin_service).and_return(@plugin_service)
        expect(@plugin_service).to receive(:pluginInfoForExtensionThatHandlesPluginSettings).with('plugin.id.1').exactly(2).times.and_return(
          com.thoughtworks.go.plugin.domain.configrepo.ConfigRepoPluginInfo.new(nil, nil, com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new(
            [com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('url', nil), com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('password', nil)])))
        controller.request.env['HTTP_IF_MATCH'] = controller.send(:generate_strong_etag, 'md5')
        hash = {plugin_id: 'plugin.id.1', configuration: [{key: 'url', value: 'git@github.com:foo/bar.git'}, {key: 'password', value: "some-value"}]}

        expect(@plugin_service).to receive(:isPluginLoaded).with('plugin.id.1').and_return(true)
        expect(@plugin_service).to receive(:isPluginLoaded).with('plugin.id.1').and_return(true)
        expect(@entity_hashing_service).to receive(:md5ForEntity).with(an_instance_of(PluginSettings)).exactly(3).times.and_return('md5')
        expect(@plugin_service).to receive(:getPluginSettings).with('plugin.id.1').and_return(@plugin_settings)
        expect(@plugin_service).to receive(:updatePluginSettings).with(an_instance_of(PluginSettings), anything, anything, "md5")

        put_with_api_header :update, params:{plugin_id: 'plugin.id.1', plugin_setting: hash}

        expect(response).to be_ok
        real_response = actual_response
        real_response.delete(:_links)
        expect(real_response).to eq(hash)
      end


      it 'should render 424 for a non existent plugin' do
        controller.stub(:check_for_stale_request)
        hash = {plugin_id: 'plugin.id.1'}
        expect(@plugin_service).to receive(:isPluginLoaded).with('plugin.id.1').and_return(false)
        expect(@plugin_service).to receive(:getPluginSettings).with('plugin.id.1').and_return(@plugin_settings)

        put_with_api_header :update, params:{plugin_id: 'plugin.id.1', plugin_setting: hash}

        expect(response).to have_api_message_response(424, 'Your request could not be processed. The plugin with id \'plugin.id.1\' is not loaded.')
      end
    end

    xdescribe 'route' do
      describe "with_header" do
        it 'should route to show action of plugin_settings controller for plugin id with dots' do
          expect(:put => 'api/admin/plugin_settings/foo.bar').to route_to(action: 'update', controller: 'api_v1/admin/plugin_settings', plugin_id: 'foo.bar')
        end
      end
      describe "without_header" do
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
