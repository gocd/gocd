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

describe ApiV1::Config::PluginSettingsRepresenter do
  describe 'serialize' do
    it 'renders plugin settings with hal representation' do
      plugin_info = com.thoughtworks.go.plugin.domain.configrepo.ConfigRepoPluginInfo.new(nil, com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('username', nil)]))
      presenter = ApiV1::Config::PluginSettingsRepresenter.new({plugin_settings: plugin_settings, plugin_info: plugin_info})
      actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
      expect(actual_json).to have_link(:self).with_url(UrlBuilder.new.apiv1_admin_plugin_setting_url(plugin_id: 'foo.bar'))
      expect(actual_json).to have_link(:doc).with_url('https://api.gocd.org/#plugin-settings')
      expect(actual_json).to have_link(:find).with_url('http://test.host/api/admin/plugin_settings/:plugin_id')
      actual_json.delete(:_links)

      expect(actual_json).to eq(plugin_settings_json)
    end
  end

  describe 'deserialize' do
    it 'create plugin settings out of given json' do
      plugin_settings = PluginSettings.new
      plugin_info = com.thoughtworks.go.plugin.domain.configrepo.ConfigRepoPluginInfo.new(nil, com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('username', nil)]))
      ApiV1::Config::PluginSettingsRepresenter.new({plugin_settings: plugin_settings, plugin_info: plugin_info}).from_hash(plugin_settings_json)
      expected_plugin_settings = plugin_settings
      expect(expected_plugin_settings.getPluginId).to eq(plugin_settings.getPluginId)
      expect(expected_plugin_settings.getPluginSettingsProperties).to eq(plugin_settings.getPluginSettingsProperties)
    end
  end

  def plugin_settings
    plugin_settings = PluginSettings.new('foo.bar')
    plugin_settings.setPluginSettingsProperties([ConfigurationProperty.new(ConfigurationKey.new('username'), ConfigurationValue.new('admin'))])
    plugin_settings
  end

  def plugin_settings_json
    {
      plugin_id: "foo.bar",
      configuration: [
        {
          key: 'username',
          value: 'admin'
        }
      ]
    }
  end
end