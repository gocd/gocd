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

describe ApiV3::Plugin::AuthorizationPluginInfoRepresenter do
  it 'should describe an authorization plugin' do
    vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
    about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', vendor, ['Linux'])
    descriptor = GoPluginDescriptor.new('foo.example', '1.0', about, nil, nil, false)

    image = com.thoughtworks.go.plugin.domain.common.Image.new('foo', Base64.strict_encode64('bar'), "945f43c56990feb8732e7114054fa33cd51ba1f8a208eb5160517033466d4756")
    auth_config_view = com.thoughtworks.go.plugin.domain.common.PluginView.new('auth_config_view_template')
    auth_config_settings = com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('url', com.thoughtworks.go.plugin.domain.common.Metadata.new(true, false))], auth_config_view)

    role_config_view = com.thoughtworks.go.plugin.domain.common.PluginView.new('role_config_view_template')
    role_config_settings = com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('memberOf', com.thoughtworks.go.plugin.domain.common.Metadata.new(true, false))], role_config_view)
    capabilities = com.thoughtworks.go.plugin.domain.authorization.Capabilities.new(com.thoughtworks.go.plugin.domain.authorization.SupportedAuthType::Password, true, true)

    plugin_info = com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo.new(descriptor, auth_config_settings, role_config_settings, image, capabilities, nil)
    actual_json = ApiV3::Plugin::AuthorizationPluginInfoRepresenter.new(plugin_info).to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:image)
    expect(actual_json).to have_link(:image).with_url('http://test.host/api/plugin_images/foo.example/945f43c56990feb8732e7114054fa33cd51ba1f8a208eb5160517033466d4756')
    actual_json.delete(:_links)

    expect(actual_json).to eq({
                                auth_config_settings: ApiV3::Plugin::PluggableInstanceSettingsRepresenter.new(auth_config_settings).to_hash(url_builder: UrlBuilder.new),
                                role_settings: ApiV3::Plugin::PluggableInstanceSettingsRepresenter.new(role_config_settings).to_hash(url_builder: UrlBuilder.new),
                                capabilities: ApiV3::Plugin::AuthorizationCapabilitiesRepresenter.new(capabilities).to_hash(url_builder: UrlBuilder.new)
                              })

  end
end
