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

describe ApiV3::Plugin::AuthenticationPluginInfoRepresenter do
  it 'should describe an authentication plugin' do
    vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
    about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', vendor, ['Linux'])
    descriptor = GoPluginDescriptor.new('foo.example', '1.0', about, nil, nil, false)

    auth_view = com.thoughtworks.go.plugin.domain.common.PluginView.new('role_config_view_template')
    metadata = com.thoughtworks.go.plugin.domain.common.Metadata.new(true, false)
    plugin_settings = com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('memberOf', metadata)], auth_view)

    plugin_info = com.thoughtworks.go.plugin.domain.authentication.AuthenticationPluginInfo.new(descriptor, "display_name", 'display_image_url', true, true, plugin_settings)
    actual_json = ApiV3::Plugin::AuthenticationPluginInfoRepresenter.new(plugin_info).to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:self, :doc, :find)
    expect(actual_json).to have_link(:self).with_url('http://test.host/api/admin/plugin_info/foo.example')
    expect(actual_json).to have_link(:doc).with_url('https://api.gocd.org/#plugin-info')
    expect(actual_json).to have_link(:find).with_url('http://test.host/api/admin/plugin_info/:plugin_id')
    actual_json.delete(:_links)

    expect(actual_json).to eq({
                                id: 'foo.example',
                                version: '1.0',
                                type: 'authentication',
                                status: {
                                  state: 'active'
                                },
                                plugin_file_location: nil,
                                bundled_plugin: false,
                                about: {
                                  name: 'Foo plugin',
                                  version: '1.2.3',
                                  target_go_version: '17.2.0',
                                  description: 'Does foo',
                                  target_operating_systems: ['Linux'],
                                  vendor: {
                                    name: 'bob',
                                    url: 'https://bob.example.com'}
                                },
                                display_name: 'display_name',
                                display_image_url: 'display_image_url',
                                supports_password_based_authentication: true,
                                supports_web_based_authentication: true,
                                plugin_settings: ApiV3::Plugin::PluggableInstanceSettingsRepresenter.new(plugin_settings).to_hash(url_builder: UrlBuilder.new)
                              })

  end
end
