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

describe ApiV4::Plugin::PluginInfosRepresenter do
  it 'renders all plugin_infos with hal representation' do
    vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
    about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', vendor, ['Linux'])
    descriptor = GoPluginDescriptor.new('foo.example', '1.0', about, nil, nil, false)

    auth_configs = pluggable_instance_settings('MANAGER_DN', 'auth_config_view')
    role_configs = pluggable_instance_settings('MEMBER_OF', 'role_config_view')
    capabilities = com.thoughtworks.go.plugin.domain.authorization.Capabilities.new(com.thoughtworks.go.plugin.domain.authorization.SupportedAuthType::Web, true, true)
    image = com.thoughtworks.go.plugin.domain.common.Image.new('foo', Base64.strict_encode64('bar'), "945f43c56990feb8732e7114054fa33cd51ba1f8a208eb5160517033466d4756")

    plugin_info = CombinedPluginInfo.new(AuthorizationPluginInfo.new(descriptor, auth_configs, role_configs, image, capabilities))

    actual_json = ApiV4::Plugin::PluginInfosRepresenter.new([plugin_info]).to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:self, :doc, :find)
    expect(actual_json).to have_link(:self).with_url('http://test.host/api/admin/plugin_info')
    expect(actual_json).to have_link(:doc).with_url(com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl '#plugin-info')
    expect(actual_json).to have_link(:find).with_url('http://test.host/api/admin/plugin_info/:plugin_id')
    actual_json.delete(:_links)
    expect(actual_json.fetch(:_embedded)).to eq({plugin_info: [ApiV4::Plugin::PluginInfoRepresenter.new(plugin_info).to_hash(url_builder: UrlBuilder.new)]})
  end
end

def pluggable_instance_settings(field_name, view)
  plugin_view = com.thoughtworks.go.plugin.domain.common.PluginView.new(view)
  field_metadata = com.thoughtworks.go.plugin.domain.common.Metadata.new(true, false)
  plugin_configuration = com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new(field_name, field_metadata)
  com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([plugin_configuration], plugin_view)
end
