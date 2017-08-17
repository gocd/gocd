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

describe ApiV3::Admin::PluginInfosController do
  include ApiHeaderSetupTeardown, ApiV3::ApiVersionHelper

  before(:each) do
    @default_plugin_info_finder = double('default_plugin_info_finder')
    controller.stub('default_plugin_info_finder').and_return(@default_plugin_info_finder)

    @default_plugin_manager = double('default_plugin_manager')
    controller.stub('default_plugin_manager').and_return(@default_plugin_manager)
    notification_view = com.thoughtworks.go.plugin.domain.common.PluginView.new('role_config_view_template')
    metadata = com.thoughtworks.go.plugin.domain.common.Metadata.new(true, false)
    @plugin_settings = com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('memberOf', metadata)], notification_view)

  end

  describe :security do
    describe :show do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:get, :show)
      end

      it 'should disallow non-admin user, with security enabled' do
        enable_security
        login_as_user
        expect(controller).to disallow_action(:get, :show, {:id => 'plugin_id'}).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:get, :show)
      end

      it 'should allow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to allow_action(:get, :show)
      end
    end

    describe :index do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:get, :index)
      end

      it 'should disallow non-admin user, with security enabled' do
        enable_security
        login_as_user
        expect(controller).to disallow_action(:get, :index).with(401, 'You are not authorized to perform this action.')
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:get, :index)
      end

      it 'should allow pipeline group admin users, with security enabled' do
        login_as_group_admin
        expect(controller).to allow_action(:get, :index)
      end
    end
  end

  describe :index do
    before(:each) do
      login_as_group_admin
    end

    it 'should list all plugin_infos' do
      vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
      about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', vendor, ['Linux'])
      descriptor = GoPluginDescriptor.new('foo.example', '1.0', about, nil, nil, false)

      plugin_info = com.thoughtworks.go.plugin.domain.notification.NotificationPluginInfo.new(descriptor, @plugin_settings)

      @default_plugin_info_finder.should_receive(:allPluginInfos).with(nil).and_return([plugin_info])

      get_with_api_header :index

      expect(response).to be_ok
      expect(actual_response).to eq(expected_response([plugin_info], ApiV3::Plugin::PluginInfosRepresenter))
    end

    it 'should list bad plugins when `include_bad` param is true' do
      good_vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
      good_about = GoPluginDescriptor::About.new('Good plugin', '1.2.3', '17.2.0', 'Does foo', good_vendor, ['Linux'])
      good_plugin = GoPluginDescriptor.new('good.plugin', '1.0', good_about, nil, nil, false)
      good_plugin_info = com.thoughtworks.go.plugin.domain.notification.NotificationPluginInfo.new(good_plugin, nil)

      bad_vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
      bad_about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', bad_vendor, ['Linux'])
      bad_plugin = GoPluginDescriptor.new('bad.plugin', '1.0', bad_about, nil, nil, false)
      bad_plugin.markAsInvalid(%w(foo bar), java.lang.RuntimeException.new('boom!'))

      bad_plugin_info = com.thoughtworks.go.plugin.domain.common.BadPluginInfo.new(bad_plugin)

      @default_plugin_manager.should_receive(:plugins).and_return([bad_plugin, good_plugin])
      @default_plugin_info_finder.should_receive(:allPluginInfos).with(nil).and_return([good_plugin_info])

      get_with_api_header :index, include_bad: true
      expect(response).to be_ok
      expect(actual_response).to eq(expected_response([good_plugin_info, bad_plugin_info], ApiV3::Plugin::PluginInfosRepresenter))
    end

    it 'should filter plugin_infos by type' do
      vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
      about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', vendor, ['Linux'])
      descriptor = GoPluginDescriptor.new('foo.example', '1.0', about, nil, nil, false)

      plugin_info = com.thoughtworks.go.plugin.domain.notification.NotificationPluginInfo.new(descriptor, @plugin_settings)

      @default_plugin_info_finder.should_receive(:allPluginInfos).with('scm').and_return([plugin_info])

      get_with_api_header :index, type: 'scm'

      expect(response).to be_ok
      expect(actual_response).to eq(expected_response([plugin_info], ApiV3::Plugin::PluginInfosRepresenter))
    end

    it 'should be a unprocessible entity for a invalid plugin type' do
      @default_plugin_info_finder.should_receive(:allPluginInfos).with('invalid_type').and_raise(InvalidPluginTypeException.new)

      get_with_api_header :index, type: 'invalid_type'

      expect(response.code).to eq('422')
      json = JSON.parse(response.body).deep_symbolize_keys
      expect(json[:message]).to eq('Your request could not be processed. Invalid plugins type - `invalid_type` !')
    end

    describe :route do
      describe :with_header do
        it 'should route to the index action of plugin_infos controller' do
          expect(:get => 'api/admin/plugin_info').to route_to(action: 'index', controller: 'api_v3/admin/plugin_infos')
        end
      end

      describe :without_header do
        before :each do
          teardown_header
        end
        it 'should not route to index action of plugin_infos controller without header' do
          expect(:get => 'api/admin/plugin_info').to_not route_to(action: 'index', controller: 'api_v3/admin/plugin_infos')
          expect(:get => 'api/admin/plugin_info').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/plugin_info')
        end
      end
    end
  end

  describe :show do
    before(:each) do
      login_as_group_admin
    end

    it 'should fetch a plugin_info for the given id' do
      vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
      about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', vendor, ['Linux'])
      descriptor = GoPluginDescriptor.new('foo.example', '1.0', about, nil, nil, false)

      plugin_info = com.thoughtworks.go.plugin.domain.notification.NotificationPluginInfo.new(descriptor, @plugin_settings)

      @default_plugin_info_finder.should_receive(:pluginInfoFor).with('plugin_id').and_return(plugin_info)

      get_with_api_header :show, id: 'plugin_id'

      expect(response).to be_ok
      expect(actual_response).to eq(expected_response(plugin_info, ApiV3::Plugin::PluginInfoRepresenter))
    end

    it 'should fetch a bad plugin info if plugin is bad' do
      bad_vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
      bad_about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', bad_vendor, ['Linux'])
      bad_plugin = GoPluginDescriptor.new('bad.plugin', '1.0', bad_about, nil, nil, false)
      bad_plugin.markAsInvalid(%w(foo bar), java.lang.RuntimeException.new('boom!'))

      bad_plugin_info = com.thoughtworks.go.plugin.domain.common.BadPluginInfo.new(bad_plugin)

      @default_plugin_info_finder.should_receive(:pluginInfoFor).with('bad.plugin').and_return(nil)
      @default_plugin_manager.should_receive(:getPluginDescriptorFor).with('bad.plugin').and_return(bad_plugin)

      get_with_api_header :show, id: 'bad.plugin'
      expect(response).to be_ok
      expect(actual_response).to eq(expected_response(bad_plugin_info, ApiV3::Plugin::PluginInfoRepresenter))
    end

    it 'should return 404 in absence of plugin_info' do
      @default_plugin_info_finder.should_receive(:pluginInfoFor).with('plugin_id').and_return(nil)
      @default_plugin_manager.should_receive(:getPluginDescriptorFor).with('plugin_id').and_return(nil)

      get_with_api_header :show, id: 'plugin_id'

      expect(response.code).to eq('404')
      json = JSON.parse(response.body).deep_symbolize_keys
      expect(json[:message]).to eq('Either the resource you requested was not found, or you are not authorized to perform this action.')
    end

    describe :route do
      describe :with_header do

        it 'should route to the show action of plugin_infos controller for alphanumeric plugin id' do
          expect(:get => 'api/admin/plugin_info/foo123bar').to route_to(action: 'show', controller: 'api_v3/admin/plugin_infos', id: 'foo123bar')
        end

        it 'should route to the show action of plugin_infos controller for plugin id with hyphen' do
          expect(:get => 'api/admin/plugin_info/foo-123-bar').to route_to(action: 'show', controller: 'api_v3/admin/plugin_infos', id: 'foo-123-bar')
        end

        it 'should route to the show action of plugin_infos controller for plugin id with underscore' do
          expect(:get => 'api/admin/plugin_info/foo_123_bar').to route_to(action: 'show', controller: 'api_v3/admin/plugin_infos', id: 'foo_123_bar')
        end

        it 'should route to the show action of plugin_infos controller for plugin id with dots' do
          expect(:get => 'api/admin/plugin_info/foo.123.bar').to route_to(action: 'show', controller: 'api_v3/admin/plugin_infos', id: 'foo.123.bar')
        end

        it 'should route to the show action of plugin_infos controller for capitalized plugin id' do
          expect(:get => 'api/admin/plugin_info/FOO').to route_to(action: 'show', controller: 'api_v3/admin/plugin_infos', id: 'FOO')
        end
      end

      describe :without_header do
        before :each do
          teardown_header
        end
        it 'should not route to show action of plugin_infos controller without header' do
          expect(:get => 'api/admin/plugin_info/abc').to_not route_to(action: 'show', controller: 'api_v3/admin/plugin_infos')
          expect(:get => 'api/admin/plugin_info/abc').to route_to(controller: 'application', action: 'unresolved', url: 'api/admin/plugin_info/abc')
        end
      end
    end
  end
end