#
# Copyright 2019 ThoughtWorks, Inc.
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
#

require 'rails_helper'

describe ApiV4::Admin::PluginInfosController do
  include ApiHeaderSetupForRouting
  include ApiV4::ApiVersionHelper

  before(:each) do
    @default_plugin_info_finder = double('default_plugin_info_finder')
    allow(controller).to receive('default_plugin_info_finder').and_return(@default_plugin_info_finder)

    @default_plugin_manager = double('default_plugin_manager')
    allow(controller).to receive('default_plugin_manager').and_return(@default_plugin_manager)
    notification_view = com.thoughtworks.go.plugin.domain.common.PluginView.new('role_config_view_template')
    metadata = com.thoughtworks.go.plugin.domain.common.Metadata.new(true, false)
    @plugin_settings = com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('memberOf', metadata)], notification_view)

  end

  describe "security" do
    describe "show" do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:get, :show)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:get, :show, params: {:id => 'plugin_id'}).with(403, 'You are not authorized to perform this action.')
      end

      it 'should allow non-admin user, with security enabled' do
        enable_security
        login_as_user
        expect(controller).to allow_action(:get, :show, params: {:id => 'plugin_id'})
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:get, :show)
      end
    end

    describe "index" do
      it 'should allow anyone, with security disabled' do
        disable_security
        expect(controller).to allow_action(:get, :index)
      end

      it 'should disallow anonymous users, with security enabled' do
        enable_security
        login_as_anonymous

        expect(controller).to disallow_action(:get, :index).with(403, 'You are not authorized to perform this action.')
      end

      it 'should allow non-admin user, with security enabled' do
        enable_security
        login_as_user
        expect(controller).to allow_action(:get, :index)
      end

      it 'should allow admin users, with security enabled' do
        login_as_admin
        expect(controller).to allow_action(:get, :index)
      end
    end
  end

  describe "index" do
    before(:each) do
      login_as_group_admin
    end

    it 'should list all plugin_infos' do
      vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
      about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', vendor, ['Linux'])
      descriptor = GoPluginDescriptor.new('foo.example', '1.0', about, nil, nil, false)

      plugin_info = CombinedPluginInfo.new(NotificationPluginInfo.new(descriptor, @plugin_settings))

      expect(@default_plugin_info_finder).to receive(:allPluginInfos).with(nil).and_return([plugin_info])

      get_with_api_header :index

      expect(response).to be_ok
      expect(actual_response).to eq(expected_response([plugin_info], ApiV4::Plugin::PluginInfosRepresenter))
    end

    it 'should list bad plugins when `include_bad` param is true' do
      good_vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
      good_about = GoPluginDescriptor::About.new('Good plugin', '1.2.3', '17.2.0', 'Does foo', good_vendor, ['Linux'])
      good_plugin = GoPluginDescriptor.new('good.plugin', '1.0', good_about, nil, nil, false)
      good_plugin_info = CombinedPluginInfo.new(NotificationPluginInfo.new(good_plugin, nil))

      bad_vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
      bad_about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', bad_vendor, ['Linux'])
      bad_plugin = GoPluginDescriptor.new('bad.plugin', '1.0', bad_about, nil, nil, false)
      bad_plugin.markAsInvalid(%w(foo bar), java.lang.RuntimeException.new('boom!'))

      bad_plugin_info = BadPluginInfo.new(bad_plugin)

      expect(@default_plugin_manager).to receive(:plugins).and_return([bad_plugin, good_plugin])
      expect(@default_plugin_info_finder).to receive(:allPluginInfos).with(nil).and_return([good_plugin_info])

      get_with_api_header :index, params:{include_bad: true}
      expect(response).to be_ok
      expect(actual_response).to eq(expected_response([good_plugin_info, bad_plugin_info], ApiV4::Plugin::PluginInfosRepresenter))
    end

    it 'should filter plugin_infos by type' do
      vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
      about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', vendor, ['Linux'])
      descriptor = GoPluginDescriptor.new('foo.example', '1.0', about, nil, nil, false)

      plugin_info = CombinedPluginInfo.new(NotificationPluginInfo.new(descriptor, @plugin_settings))

      expect(@default_plugin_info_finder).to receive(:allPluginInfos).with('scm').and_return([plugin_info])

      get_with_api_header :index, params:{type: 'scm'}

      expect(response).to be_ok
      expect(actual_response).to eq(expected_response([plugin_info], ApiV4::Plugin::PluginInfosRepresenter))
    end

    it 'should filter unsupported plugin extensions' do
      vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
      about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', vendor, ['Linux'])
      descriptor = GoPluginDescriptor.new('foo.example', '1.0', about, nil, nil, false)
      notification_plugin_info = CombinedPluginInfo.new(NotificationPluginInfo.new(descriptor, @plugin_settings))
      unsupported_plugin_info = instance_double('unsupported_plugin')

      expect(unsupported_plugin_info).to receive(:extensionNames).and_return(['unsupported'])
      expect(@default_plugin_info_finder).to receive(:allPluginInfos).with('scm').and_return([notification_plugin_info, unsupported_plugin_info])

      get_with_api_header :index, params:{type: 'scm'}

      expect(response).to be_ok
      expect(actual_response).to eq(expected_response([notification_plugin_info], ApiV4::Plugin::PluginInfosRepresenter))
    end

    it 'should not filter supported plugin extensions' do
      vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
      about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', vendor, ['Linux'])
      descriptor = GoPluginDescriptor.new('foo.example', '1.0', about, nil, nil, false)

      allPluginInfos = [CombinedPluginInfo.new(AnalyticsPluginInfo.new(descriptor, nil, nil, nil)),
                        CombinedPluginInfo.new(AuthorizationPluginInfo.new(descriptor, nil, nil, nil, nil)),
                        CombinedPluginInfo.new(ConfigRepoPluginInfo.new(descriptor, nil, @plugin_settings, nil)),
                        CombinedPluginInfo.new(ElasticAgentPluginInfo.new(descriptor, nil, nil, nil, nil, nil)),
                        CombinedPluginInfo.new(NotificationPluginInfo.new(descriptor, @plugin_settings)),
                        CombinedPluginInfo.new(PackageMaterialPluginInfo.new(descriptor, nil, nil, nil)),
                        CombinedPluginInfo.new(PluggableTaskPluginInfo.new(descriptor, nil, nil)),
                        CombinedPluginInfo.new(SCMPluginInfo.new(descriptor, nil, nil, nil)),
                        CombinedPluginInfo.new(ArtifactPluginInfo.new(descriptor, nil, nil, nil, nil, nil))]

      expect(@default_plugin_info_finder).to receive(:allPluginInfos).and_return(allPluginInfos)

      get_with_api_header :index

      expected_response = %w(analytics artifact authorization configrepo elastic-agent notification package-repository scm task)

      expect(response).to be_ok
      expect(actual_response[:_embedded][:plugin_info].length).to eq(9)
      expect((actual_response[:_embedded][:plugin_info].map do |pi|
        expect(pi[:extensions].length).to eq(1)
        pi[:extensions][0][:type]
      end).flatten.sort).to eq(expected_response)
    end

    it 'should be an unprocessable entity for a invalid plugin type' do
      expect(@default_plugin_info_finder).to receive(:allPluginInfos).with('invalid_type').and_raise(InvalidPluginTypeException.new)

      get_with_api_header :index, params:{type: 'invalid_type'}

      expect(response.code).to eq('422')
      json = JSON.parse(response.body).deep_symbolize_keys
      expect(json[:message]).to eq('Your request could not be processed. Invalid plugins type - `invalid_type` !')
    end

  end

  describe "show" do
    before(:each) do
      login_as_group_admin
    end

    it 'should fetch a plugin_info for the given id' do
      vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
      about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', vendor, ['Linux'])
      descriptor = GoPluginDescriptor.new('foo.example', '1.0', about, nil, nil, false)

      plugin_info = CombinedPluginInfo.new(NotificationPluginInfo.new(descriptor, @plugin_settings))

      expect(@default_plugin_info_finder).to receive(:pluginInfoFor).with('plugin_id').and_return(plugin_info)

      get_with_api_header :show, params:{id: 'plugin_id'}

      expect(response).to be_ok
      expect(actual_response).to eq(expected_response(plugin_info, ApiV4::Plugin::PluginInfoRepresenter))
    end

    it 'should fetch a bad plugin info if plugin is bad' do
      bad_vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
      bad_about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', bad_vendor, ['Linux'])
      bad_plugin = GoPluginDescriptor.new('bad.plugin', '1.0', bad_about, nil, nil, false)
      bad_plugin.markAsInvalid(%w(foo bar), java.lang.RuntimeException.new('boom!'))

      bad_plugin_info = com.thoughtworks.go.plugin.domain.common.BadPluginInfo.new(bad_plugin)

      expect(@default_plugin_info_finder).to receive(:pluginInfoFor).with('bad.plugin').and_return(nil)
      expect(@default_plugin_manager).to receive(:getPluginDescriptorFor).with('bad.plugin').and_return(bad_plugin)

      get_with_api_header :show, params:{id: 'bad.plugin'}
      expect(response).to be_ok
      expect(actual_response).to eq(expected_response(bad_plugin_info, ApiV4::Plugin::PluginInfoRepresenter))
    end

    it 'should return 404 for unsupported plugins' do
      unsupported_plugin_info = instance_double('unsupported_plugin')

      expect(unsupported_plugin_info).to receive(:extensionNames).and_return(['unsupported'])
      expect(@default_plugin_info_finder).to receive(:pluginInfoFor).with('unsupported.plugin').and_return(unsupported_plugin_info)

      get_with_api_header :show, params:{id: 'unsupported.plugin'}

      expect(response.code).to eq('404')
    end

    it 'should return 404 in absence of plugin_info' do
      expect(@default_plugin_info_finder).to receive(:pluginInfoFor).with('plugin_id').and_return(nil)
      expect(@default_plugin_manager).to receive(:getPluginDescriptorFor).with('plugin_id').and_return(nil)

      get_with_api_header :show, params:{id: 'plugin_id'}

      expect(response.code).to eq('404')
      json = JSON.parse(response.body).deep_symbolize_keys
      expect(json[:message]).to eq('Either the resource you requested was not found, or you are not authorized to perform this action.')
    end

  end
end
