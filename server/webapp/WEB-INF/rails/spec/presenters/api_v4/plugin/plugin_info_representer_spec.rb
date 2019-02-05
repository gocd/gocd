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

describe ApiV4::Plugin::PluginInfoRepresenter do

  describe 'bad plugin info' do
    it 'should describe a BadPluginInfo object' do
      vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
      about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', vendor, ['Linux'])
      descriptor = GoPluginDescriptor.new('foo.example', '1.0', about, '/path/to/foo.jar', nil, false)
      descriptor.markAsInvalid(%w(foo bar), java.lang.RuntimeException.new('boom!'))

      plugin_info = BadPluginInfo.new(descriptor)
      actual_json = ApiV4::Plugin::PluginInfoRepresenter.new(plugin_info).to_hash(url_builder: UrlBuilder.new)

      expect(actual_json).to have_links(:self, :doc, :find)
      expect(actual_json).to have_link(:self).with_url('http://test.host/api/admin/plugin_info/foo.example')
      expect(actual_json).to have_link(:doc).with_url(com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl '#plugin-info')
      expect(actual_json).to have_link(:find).with_url('http://test.host/api/admin/plugin_info/:plugin_id')
      actual_json.delete(:_links)

      expect(actual_json).to eq({
                                  id: 'foo.example',
                                  plugin_file_location: '/path/to/foo.jar',
                                  bundled_plugin: false,
                                  status: {
                                    state: 'invalid',
                                    messages: %w(foo bar)
                                  },
                                  about: about_json,
                                  extensions: []
                                })
    end
  end

  describe 'config repo plugin info' do
    it 'should describe a ConfigRepoPluginInfo object' do
      vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
      about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', vendor, ['Linux'])
      descriptor = GoPluginDescriptor.new('foo.example', '1.0', about, '/path/to/foo.jar', nil, false)

      plugin_view = com.thoughtworks.go.plugin.domain.common.PluginView.new('plugin_view_template')
      plugin_metadata = com.thoughtworks.go.plugin.domain.common.Metadata.new(true, false)
      plugin_settings = com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('memberOf', plugin_metadata)], plugin_view)

      plugin_info = CombinedPluginInfo.new(ConfigRepoPluginInfo.new(descriptor, nil, plugin_settings))
      actual_json = ApiV4::Plugin::PluginInfoRepresenter.new(plugin_info).to_hash(url_builder: UrlBuilder.new)

      expect(actual_json).to have_links(:self, :doc, :find)
      expect(actual_json).to have_link(:self).with_url('http://test.host/api/admin/plugin_info/foo.example')
      expect(actual_json).to have_link(:doc).with_url(com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl '#plugin-info')
      expect(actual_json).to have_link(:find).with_url('http://test.host/api/admin/plugin_info/:plugin_id')
      actual_json.delete(:_links)

      expect(actual_json).to eq({
                                  id: 'foo.example',
                                  plugin_file_location: '/path/to/foo.jar',
                                  bundled_plugin: false,
                                  status: {
                                    state: 'active'
                                  },
                                  about: about_json,
                                  extensions: [
                                    {
                                      type: 'configrepo',
                                      plugin_settings: ApiV4::Plugin::PluggableInstanceSettingsRepresenter.new(plugin_settings).to_hash(url_builder: UrlBuilder.new)
                                    }
                                  ]})
    end
  end

  describe 'scm plugin info' do
    it 'should describe a scm plugin' do
      vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
      about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', vendor, ['Linux'])
      descriptor = GoPluginDescriptor.new('foo.example', '1.0', about, '/path/to/foo.jar', nil, false)

      task_view = com.thoughtworks.go.plugin.domain.common.PluginView.new('role_config_view_template')
      metadata = com.thoughtworks.go.plugin.domain.common.MetadataWithPartOfIdentity.new(true, false, true)
      scm_settings = com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('memberOf', metadata)], task_view)

      plugin_info = CombinedPluginInfo.new(SCMPluginInfo.new(descriptor, 'Foo task', scm_settings, nil))
      actual_json = ApiV4::Plugin::PluginInfoRepresenter.new(plugin_info).to_hash(url_builder: UrlBuilder.new)
      actual_json.delete(:_links)

      expect(actual_json).to eq({
                                  id: 'foo.example',
                                  plugin_file_location: '/path/to/foo.jar',
                                  bundled_plugin: false,
                                  status: {
                                    state: 'active'
                                  },
                                  about: about_json,
                                  extensions: [
                                    {
                                      type: 'scm',
                                      display_name: 'Foo task',
                                      scm_settings: ApiV4::Plugin::PluggableInstanceSettingsRepresenter.new(scm_settings).to_hash(url_builder: UrlBuilder.new),
                                    }
                                  ]})
    end
  end

  describe 'pluggable task plugin info' do
    it 'should describe a pluggable task plugin' do
      vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
      about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', vendor, ['Linux'])
      descriptor = GoPluginDescriptor.new('foo.example', '1.0', about, '/path/to/foo.jar', nil, false)

      task_view = com.thoughtworks.go.plugin.domain.common.PluginView.new('pluggable_task_view_template')
      metadata = com.thoughtworks.go.plugin.domain.common.Metadata.new(true, false)
      task_settings = com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('memberOf', metadata)], task_view)

      plugin_info = CombinedPluginInfo.new(PluggableTaskPluginInfo.new(descriptor, 'Foo task', task_settings))
      actual_json = ApiV4::Plugin::PluginInfoRepresenter.new(plugin_info).to_hash(url_builder: UrlBuilder.new)
      actual_json.delete(:_links)

      expect(actual_json).to eq({
                                  id: 'foo.example',
                                  plugin_file_location: '/path/to/foo.jar',
                                  bundled_plugin: false,
                                  status: {
                                    state: 'active'
                                  },
                                  about: about_json,
                                  extensions: [
                                    {
                                      type: 'task',
                                      display_name: 'Foo task',
                                      task_settings: ApiV4::Plugin::PluggableInstanceSettingsRepresenter.new(task_settings).to_hash(url_builder: UrlBuilder.new),
                                    }
                                  ]})
    end

  end

  describe 'package repository plugin info' do
    it 'should describe a package repository plugin plugin' do
      vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
      about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', vendor, ['Linux'])
      descriptor = GoPluginDescriptor.new('foo.example', '1.0', about, '/path/to/foo.jar', nil, false)

      package_view = com.thoughtworks.go.plugin.domain.common.PluginView.new('package_view_template')
      package_metadata = com.thoughtworks.go.plugin.domain.common.PackageMaterialMetadata.new(true, false, true, "Url", 1)
      package_settings = com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('url', package_metadata)], package_view)

      repo_view = com.thoughtworks.go.plugin.domain.common.PluginView.new('repo_view_template')
      repo_metadata = com.thoughtworks.go.plugin.domain.common.PackageMaterialMetadata.new(true, false, true, "Member Of", 1)
      repo_settings = com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('memberOf', repo_metadata)], repo_view)

      plugin_info = CombinedPluginInfo.new(PackageMaterialPluginInfo.new(descriptor, repo_settings, package_settings, nil))
      actual_json = ApiV4::Plugin::PluginInfoRepresenter.new(plugin_info).to_hash(url_builder: UrlBuilder.new)
      actual_json.delete(:_links)

      expect(actual_json).to eq({
                                  id: 'foo.example',
                                  plugin_file_location: '/path/to/foo.jar',
                                  bundled_plugin: false,
                                  status: {
                                    state: 'active'
                                  },
                                  about: about_json,
                                  extensions: [
                                    {
                                      type: 'package-repository',
                                      package_settings: ApiV4::Plugin::PluggableInstanceSettingsRepresenter.new(package_settings).to_hash(url_builder: UrlBuilder.new),
                                      repository_settings: ApiV4::Plugin::PluggableInstanceSettingsRepresenter.new(repo_settings).to_hash(url_builder: UrlBuilder.new),
                                    }
                                  ]})
    end
  end

  describe 'notification plugin info' do
    it 'should describe an notification plugin' do
      vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
      about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', vendor, ['Linux'])
      descriptor = GoPluginDescriptor.new('foo.example', '1.0', about, '/path/to/foo.jar', nil, false)

      auth_view = com.thoughtworks.go.plugin.domain.common.PluginView.new('notification_view_template')
      metadata = com.thoughtworks.go.plugin.domain.common.Metadata.new(true, false)
      plugin_settings = com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('memberOf', metadata)], auth_view)

      plugin_info = CombinedPluginInfo.new(NotificationPluginInfo.new(descriptor, plugin_settings))
      actual_json = ApiV4::Plugin::PluginInfoRepresenter.new(plugin_info).to_hash(url_builder: UrlBuilder.new)
      actual_json.delete(:_links)

      expect(actual_json).to eq({
                                  id: 'foo.example',
                                  plugin_file_location: '/path/to/foo.jar',
                                  bundled_plugin: false,
                                  status: {
                                    state: 'active'
                                  },
                                  about: about_json,
                                  extensions: [
                                    {
                                      type: 'notification',
                                      plugin_settings: ApiV4::Plugin::PluggableInstanceSettingsRepresenter.new(plugin_settings).to_hash(url_builder: UrlBuilder.new),
                                    }
                                  ]})
    end
  end

  describe 'elastic agent plugin info' do
    it 'should describe an elastic agent plugin' do
      vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
      about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', vendor, ['Linux'])
      descriptor = GoPluginDescriptor.new('foo.example', '1.0', about, '/path/to/foo.jar', nil, false)

      image = com.thoughtworks.go.plugin.domain.common.Image.new('foo', Base64.strict_encode64('bar'), "945f43c56990feb8732e7114054fa33cd51ba1f8a208eb5160517033466d4756")
      profile_view = com.thoughtworks.go.plugin.domain.common.PluginView.new('elastic_agent_view_template')
      metadata = com.thoughtworks.go.plugin.domain.common.Metadata.new(true, false)
      profile_settings = com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('memberOf', metadata)], profile_view)

      plugin_view = com.thoughtworks.go.plugin.domain.common.PluginView.new('plugin_view_template')
      plugin_metadata = com.thoughtworks.go.plugin.domain.common.Metadata.new(true, false)
      plugin_settings = com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('memberOf', plugin_metadata)], plugin_view)
      capabilities = com.thoughtworks.go.plugin.domain.elastic.Capabilities.new(true, true)

      plugin_info = CombinedPluginInfo.new(ElasticAgentPluginInfo.new(descriptor, profile_settings, image, plugin_settings, capabilities))
      actual_json = ApiV4::Plugin::PluginInfoRepresenter.new(plugin_info).to_hash(url_builder: UrlBuilder.new)
      expect(actual_json).to have_link(:image).with_url('http://test.host/go/api/plugin_images/foo.example/945f43c56990feb8732e7114054fa33cd51ba1f8a208eb5160517033466d4756')
      actual_json.delete(:_links)

      expect(actual_json).to eq({
                                  id: 'foo.example',
                                  plugin_file_location: '/path/to/foo.jar',
                                  bundled_plugin: false,
                                  status: {
                                    state: 'active'
                                  },
                                  about: about_json,
                                  extensions: [
                                    {
                                      type: 'elastic-agent',
                                      plugin_settings: ApiV4::Plugin::PluggableInstanceSettingsRepresenter.new(plugin_settings).to_hash(url_builder: UrlBuilder.new),
                                      profile_settings: ApiV4::Plugin::PluggableInstanceSettingsRepresenter.new(profile_settings).to_hash(url_builder: UrlBuilder.new),
                                      capabilities: ApiV4::Plugin::ElasticPluginCapabilitiesRepresenter.new(capabilities).to_hash(url_builder: UrlBuilder.new)
                                    }
                                  ]})
    end
  end

  describe 'authorization plugin info' do
    it 'should describe an authorization plugin' do
      vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
      about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', vendor, ['Linux'])
      descriptor = GoPluginDescriptor.new('foo.example', '1.0', about, '/path/to/foo.jar', nil, false)

      image = com.thoughtworks.go.plugin.domain.common.Image.new('foo', Base64.strict_encode64('bar'), "945f43c56990feb8732e7114054fa33cd51ba1f8a208eb5160517033466d4756")
      auth_config_view = com.thoughtworks.go.plugin.domain.common.PluginView.new('auth_config_view_template')
      auth_config_settings = com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('url', com.thoughtworks.go.plugin.domain.common.Metadata.new(true, false))], auth_config_view)

      role_config_view = com.thoughtworks.go.plugin.domain.common.PluginView.new('role_config_view_template')
      role_config_settings = com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('memberOf', com.thoughtworks.go.plugin.domain.common.Metadata.new(true, false))], role_config_view)
      capabilities = com.thoughtworks.go.plugin.domain.authorization.Capabilities.new(com.thoughtworks.go.plugin.domain.authorization.SupportedAuthType::Password, true, true, false)

      plugin_info = CombinedPluginInfo.new(AuthorizationPluginInfo.new(descriptor, auth_config_settings, role_config_settings, image, capabilities))
      actual_json = ApiV4::Plugin::PluginInfoRepresenter.new(plugin_info).to_hash(url_builder: UrlBuilder.new)
      expect(actual_json).to have_link(:image).with_url('http://test.host/go/api/plugin_images/foo.example/945f43c56990feb8732e7114054fa33cd51ba1f8a208eb5160517033466d4756')
      actual_json.delete(:_links)

      expect(actual_json).to eq({
                                  id: 'foo.example',
                                  plugin_file_location: '/path/to/foo.jar',
                                  bundled_plugin: false,
                                  status: {
                                    state: 'active'
                                  },
                                  about: about_json,
                                  extensions: [
                                    {
                                      type: 'authorization',
                                      auth_config_settings: ApiV4::Plugin::PluggableInstanceSettingsRepresenter.new(auth_config_settings).to_hash(url_builder: UrlBuilder.new),
                                      role_settings: ApiV4::Plugin::PluggableInstanceSettingsRepresenter.new(role_config_settings).to_hash(url_builder: UrlBuilder.new),
                                      capabilities: ApiV4::Plugin::AuthorizationCapabilitiesRepresenter.new(capabilities).to_hash(url_builder: UrlBuilder.new)
                                    }
                                  ]})
    end
  end

  describe 'analytics plugin info' do
    it 'should serialize analytics plugin info to JSON' do
      vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
      about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', vendor, ['Linux'])
      descriptor = GoPluginDescriptor.new('foo.example', '1.0', about, '/path/to/foo.jar', nil, false)

      image = com.thoughtworks.go.plugin.domain.common.Image.new('foo', Base64.strict_encode64('bar'), "945f43c56990feb8732e7114054fa33cd51ba1f8a208eb5160517033466d4756")

      plugin_view = com.thoughtworks.go.plugin.domain.common.PluginView.new('plugin_view_template')
      plugin_metadata = com.thoughtworks.go.plugin.domain.common.Metadata.new(true, false)
      plugin_settings = com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('username', plugin_metadata)], plugin_view)
      dashboard_analytics = SupportedAnalytics.new('dashboard', 'top_ten_agents_by_utilization', 'Top Ten Agents With Highest Utilization')
      pipeline_analytics  = SupportedAnalytics.new('pipeline', 'top_ten_pipelines_by_wait_time', 'Top Ten Pipelines With Highest Wait Time')
      capabilities = com.thoughtworks.go.plugin.domain.analytics.Capabilities.new([dashboard_analytics, pipeline_analytics])

      plugin_info = CombinedPluginInfo.new(AnalyticsPluginInfo.new(descriptor, image, capabilities, plugin_settings))
      actual_json = ApiV4::Plugin::PluginInfoRepresenter.new(plugin_info).to_hash(url_builder: UrlBuilder.new)
      expect(actual_json).to have_link(:image).with_url('http://test.host/go/api/plugin_images/foo.example/945f43c56990feb8732e7114054fa33cd51ba1f8a208eb5160517033466d4756')
      actual_json.delete(:_links)

      expect(actual_json).to eq({
                                  id: 'foo.example',
                                  status: {
                                      state: 'active'
                                  },
                                  plugin_file_location: '/path/to/foo.jar',
                                  bundled_plugin: false,

                                  about: about_json,
                                  extensions: [
                                    {
                                      type: 'analytics',
                                      plugin_settings: ApiV4::Plugin::PluggableInstanceSettingsRepresenter.new(plugin_settings).to_hash(url_builder: UrlBuilder.new),
                                      capabilities: {
                                        supported_analytics: [
                                          {:type => 'dashboard', :id => 'top_ten_agents_by_utilization', :title => 'Top Ten Agents With Highest Utilization'},
                                          {:type => 'pipeline', :id => 'top_ten_pipelines_by_wait_time', :title => 'Top Ten Pipelines With Highest Wait Time'}
                                        ]
                                      }
                                    }
                                  ]
                                })
    end
  end

  describe 'artifact plugin info' do
    it 'should describe an artifact plugin' do
      vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
      about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', vendor, ['Linux'])
      descriptor = GoPluginDescriptor.new('foo.example', '1.0', about, '/path/to/foo.jar', nil, false)

      store_config_view = com.thoughtworks.go.plugin.domain.common.PluginView.new('store_config_view_template')
      store_config_settings = com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('bucket', com.thoughtworks.go.plugin.domain.common.Metadata.new(true, false))], store_config_view)

      artifact_config_view = com.thoughtworks.go.plugin.domain.common.PluginView.new('publish_artifact_config_view_template')
      artifact_config_settings = com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('filename', com.thoughtworks.go.plugin.domain.common.Metadata.new(true, false))], artifact_config_view)

      fetch_artifact_config_view = com.thoughtworks.go.plugin.domain.common.PluginView.new('fetch_artifact_config_view_template')
      fetch_artifact_config_settings = com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('destination', com.thoughtworks.go.plugin.domain.common.Metadata.new(true, false))], fetch_artifact_config_view)

      capabilities = com.thoughtworks.go.plugin.domain.artifact.Capabilities.new
      plugin_info = CombinedPluginInfo.new(ArtifactPluginInfo.new(descriptor, store_config_settings, artifact_config_settings, fetch_artifact_config_settings, nil, capabilities))
      actual_json = ApiV4::Plugin::PluginInfoRepresenter.new(plugin_info).to_hash(url_builder: UrlBuilder.new)
      actual_json.delete(:_links)

      expect(actual_json).to eq({
                                  id: 'foo.example',
                                  plugin_file_location: '/path/to/foo.jar',
                                  bundled_plugin: false,
                                  status: {
                                    state: 'active'
                                  },
                                  about: about_json,
                                  extensions: [
                                    {
                                      type: 'artifact',
                                      capabilities: ApiV4::Plugin::ArtifactCapabilitiesRepresenter.new(capabilities).to_hash(url_builder: UrlBuilder.new),
                                      store_config_settings: ApiV4::Plugin::PluggableInstanceSettingsRepresenter.new(store_config_settings).to_hash(url_builder: UrlBuilder.new),
                                      artifact_config_settings: ApiV4::Plugin::PluggableInstanceSettingsRepresenter.new(artifact_config_settings).to_hash(url_builder: UrlBuilder.new),
                                      fetch_artifact_settings: ApiV4::Plugin::PluggableInstanceSettingsRepresenter.new(fetch_artifact_config_settings).to_hash(url_builder: UrlBuilder.new),
                                    }
                                  ]})
    end
  end

  describe 'plugin info with multiple extensions in it' do
    it 'should have multiple extensions' do
      vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
      about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', vendor, ['Linux'])
      descriptor = GoPluginDescriptor.new('foo.example', '1.0', about, '/path/to/foo.jar', nil, false)

      task_plugin_view = com.thoughtworks.go.plugin.domain.common.PluginView.new('pluggable_task_view_template')
      task_plugin_metadata = com.thoughtworks.go.plugin.domain.common.Metadata.new(true, false)
      task_plugin_settings = com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('memberOf', task_plugin_metadata)], task_plugin_view)
      task_plugin_info = PluggableTaskPluginInfo.new(descriptor, 'Foo task', task_plugin_settings)

      notification_plugin_view = com.thoughtworks.go.plugin.domain.common.PluginView.new('pluggable_task_view_template')
      notification_plugin_metadata = com.thoughtworks.go.plugin.domain.common.Metadata.new(true, false)
      notification_plugin_settings = com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('memberOf', notification_plugin_metadata)], notification_plugin_view)
      notification_plugin_info = NotificationPluginInfo.new(descriptor, notification_plugin_settings)

      config_repo_plugin_info = ConfigRepoPluginInfo.new(descriptor, nil, nil)

      plugin_info_with_multiple_extensions = CombinedPluginInfo.new([notification_plugin_info, task_plugin_info, config_repo_plugin_info])
      actual_json = ApiV4::Plugin::PluginInfoRepresenter.new(plugin_info_with_multiple_extensions).to_hash(url_builder: UrlBuilder.new)

      actual_json.delete(:_links)

      expect(actual_json).to eq({
                                  id: 'foo.example',
                                  plugin_file_location: '/path/to/foo.jar',
                                  bundled_plugin: false,
                                  status: {
                                    state: 'active'
                                  },
                                  about: about_json,
                                  extensions: [
                                    {
                                      type: 'notification',
                                      plugin_settings: ApiV4::Plugin::PluggableInstanceSettingsRepresenter.new(notification_plugin_settings).to_hash(url_builder: UrlBuilder.new)
                                    },
                                    {
                                      type: 'task',
                                      display_name: 'Foo task',
                                      task_settings: ApiV4::Plugin::PluggableInstanceSettingsRepresenter.new(task_plugin_settings).to_hash(url_builder: UrlBuilder.new),
                                    },
                                    {
                                      type: 'configrepo'
                                    }
                                  ]})
    end
  end

  def about_json
    {
      name: 'Foo plugin',
      version: '1.2.3',
      target_go_version: '17.2.0',
      description: 'Does foo',
      target_operating_systems: ['Linux'],
      vendor: {
        name: 'bob',
        url: 'https://bob.example.com'}
    }
  end
end
