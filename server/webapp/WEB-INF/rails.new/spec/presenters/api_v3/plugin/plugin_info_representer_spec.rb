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

describe ApiV3::Plugin::PluginInfoRepresenter do

  describe 'bad plugin info' do
    it 'should describe a BadPluginInfo object' do
      vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
      about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', vendor, ['Linux'])
      descriptor = GoPluginDescriptor.new('foo.example', '1.0', about, '/path/to/foo.jar', nil, false)
      descriptor.markAsInvalid(%w(foo bar), java.lang.RuntimeException.new('boom!'))

      plugin_view = com.thoughtworks.go.plugin.domain.common.PluginView.new('plugin_view_template')
      plugin_metadata = com.thoughtworks.go.plugin.domain.common.Metadata.new(true, false)
      plugin_settings = com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('memberOf', plugin_metadata)], plugin_view)

      plugin_info = com.thoughtworks.go.plugin.domain.common.PluginInfo.new(descriptor, 'plugin-type', plugin_settings)
      actual_json = ApiV3::Plugin::PluginInfoRepresenter.new(plugin_info).to_hash(url_builder: UrlBuilder.new)

      expect(actual_json).to have_links(:self, :doc, :find)
      expect(actual_json).to have_link(:self).with_url('http://test.host/api/admin/plugin_info/foo.example')
      expect(actual_json).to have_link(:doc).with_url('https://api.gocd.org/#plugin-info')
      expect(actual_json).to have_link(:find).with_url('http://test.host/api/admin/plugin_info/:plugin_id')
      actual_json.delete(:_links)

      expect(actual_json).to eq({
                                  id: 'foo.example',
                                  type: 'plugin-type',
                                  plugin_file_location: '/path/to/foo.jar',
                                  bundled_plugin: false,
                                  status: {
                                    state: 'invalid',
                                    messages: %w(foo bar)
                                  },
                                  about: about_json
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

      plugin_info = com.thoughtworks.go.plugin.domain.configrepo.ConfigRepoPluginInfo.new(descriptor, plugin_settings)
      actual_json = ApiV3::Plugin::PluginInfoRepresenter.new(plugin_info).to_hash(url_builder: UrlBuilder.new)

      expect(actual_json).to have_links(:self, :doc, :find)
      expect(actual_json).to have_link(:self).with_url('http://test.host/api/admin/plugin_info/foo.example')
      expect(actual_json).to have_link(:doc).with_url('https://api.gocd.org/#plugin-info')
      expect(actual_json).to have_link(:find).with_url('http://test.host/api/admin/plugin_info/:plugin_id')
      actual_json.delete(:_links)

      expect(actual_json).to eq({
                                  id: 'foo.example',
                                  type: 'configrepo',
                                  plugin_file_location: '/path/to/foo.jar',
                                  bundled_plugin: false,
                                  status: {
                                    state: 'active'
                                  },
                                  about: about_json,
                                  extension_info: {
                                    plugin_settings: ApiV3::Plugin::PluggableInstanceSettingsRepresenter.new(plugin_settings).to_hash(url_builder: UrlBuilder.new)
                                  }
                                })
    end
  end

  describe 'scm plugin info' do
    it 'should describe an authentication plugin' do
      vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
      about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', vendor, ['Linux'])
      descriptor = GoPluginDescriptor.new('foo.example', '1.0', about, '/path/to/foo.jar', nil, false)

      task_view = com.thoughtworks.go.plugin.domain.common.PluginView.new('role_config_view_template')
      metadata = com.thoughtworks.go.plugin.domain.common.MetadataWithPartOfIdentity.new(true, false, true)
      scm_settings = com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('memberOf', metadata)], task_view)

      plugin_info = com.thoughtworks.go.plugin.domain.scm.SCMPluginInfo.new(descriptor, 'Foo task', scm_settings, nil)
      actual_json = ApiV3::Plugin::PluginInfoRepresenter.new(plugin_info).to_hash(url_builder: UrlBuilder.new)
      actual_json.delete(:_links)

      expect(actual_json).to eq({
                                  id: 'foo.example',
                                  type: 'scm',
                                  plugin_file_location: '/path/to/foo.jar',
                                  bundled_plugin: false,
                                  status: {
                                    state: 'active'
                                  },
                                  about: about_json,
                                  extension_info: {
                                    display_name: 'Foo task',
                                    scm_settings: ApiV3::Plugin::PluggableInstanceSettingsRepresenter.new(scm_settings).to_hash(url_builder: UrlBuilder.new),
                                  }
                                })

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

      plugin_info = com.thoughtworks.go.plugin.domain.pluggabletask.PluggableTaskPluginInfo.new(descriptor, 'Foo task', task_settings)
      actual_json = ApiV3::Plugin::PluginInfoRepresenter.new(plugin_info).to_hash(url_builder: UrlBuilder.new)
      actual_json.delete(:_links)

      expect(actual_json).to eq({
                                  id: 'foo.example',
                                  type: 'task',
                                  plugin_file_location: '/path/to/foo.jar',
                                  bundled_plugin: false,
                                  status: {
                                    state: 'active'
                                  },
                                  about: about_json,
                                  extension_info: {
                                    display_name: 'Foo task',
                                    task_settings: ApiV3::Plugin::PluggableInstanceSettingsRepresenter.new(task_settings).to_hash(url_builder: UrlBuilder.new),
                                  }
                                })

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

      plugin_info = com.thoughtworks.go.plugin.domain.packagematerial.PackageMaterialPluginInfo.new(descriptor, repo_settings, package_settings, nil)
      actual_json = ApiV3::Plugin::PluginInfoRepresenter.new(plugin_info).to_hash(url_builder: UrlBuilder.new)
      actual_json.delete(:_links)

      expect(actual_json).to eq({
                                  id: 'foo.example',
                                  type: 'package-repository',
                                  plugin_file_location: '/path/to/foo.jar',
                                  bundled_plugin: false,
                                  status: {
                                    state: 'active'
                                  },
                                  about: about_json,
                                  extension_info: {
                                    package_settings: ApiV3::Plugin::PluggableInstanceSettingsRepresenter.new(package_settings).to_hash(url_builder: UrlBuilder.new),
                                    repository_settings: ApiV3::Plugin::PluggableInstanceSettingsRepresenter.new(repo_settings).to_hash(url_builder: UrlBuilder.new),
                                  }
                                })

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

      plugin_info = com.thoughtworks.go.plugin.domain.notification.NotificationPluginInfo.new(descriptor, plugin_settings)
      actual_json = ApiV3::Plugin::PluginInfoRepresenter.new(plugin_info).to_hash(url_builder: UrlBuilder.new)
      actual_json.delete(:_links)

      expect(actual_json).to eq({
                                  id: 'foo.example',
                                  type: 'notification',
                                  plugin_file_location: '/path/to/foo.jar',
                                  bundled_plugin: false,
                                  status: {
                                    state: 'active'
                                  },
                                  about: about_json,
                                  extension_info: {
                                    plugin_settings: ApiV3::Plugin::PluggableInstanceSettingsRepresenter.new(plugin_settings).to_hash(url_builder: UrlBuilder.new),
                                  }
                                })

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
      capabilities = com.thoughtworks.go.plugin.domain.elastic.Capabilities.new(true)


      plugin_info = com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo.new(descriptor, profile_settings, image, plugin_settings, capabilities)
      actual_json = ApiV3::Plugin::PluginInfoRepresenter.new(plugin_info).to_hash(url_builder: UrlBuilder.new)
      expect(actual_json).to have_link(:image).with_url('http://test.host/api/plugin_images/foo.example/945f43c56990feb8732e7114054fa33cd51ba1f8a208eb5160517033466d4756')
      actual_json.delete(:_links)

      expect(actual_json).to eq({
                                  id: 'foo.example',
                                  type: 'elastic-agent',
                                  plugin_file_location: '/path/to/foo.jar',
                                  bundled_plugin: false,
                                  status: {
                                    state: 'active'
                                  },
                                  about: about_json,
                                  extension_info: {
                                    plugin_settings: ApiV3::Plugin::PluggableInstanceSettingsRepresenter.new(plugin_settings).to_hash(url_builder: UrlBuilder.new),
                                    profile_settings: ApiV3::Plugin::PluggableInstanceSettingsRepresenter.new(profile_settings).to_hash(url_builder: UrlBuilder.new),
                                    capabilities: ApiV3::Plugin::ElasticPluginCapabilitiesRepresenter.new(capabilities).to_hash(url_builder: UrlBuilder.new)
                                  }
                                })
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
        capabilities = com.thoughtworks.go.plugin.domain.authorization.Capabilities.new(com.thoughtworks.go.plugin.domain.authorization.SupportedAuthType::Password, true, true)

        plugin_info = com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo.new(descriptor, auth_config_settings, role_config_settings, image, capabilities, nil)
        actual_json = ApiV3::Plugin::PluginInfoRepresenter.new(plugin_info).to_hash(url_builder: UrlBuilder.new)
        expect(actual_json).to have_link(:image).with_url('http://test.host/api/plugin_images/foo.example/945f43c56990feb8732e7114054fa33cd51ba1f8a208eb5160517033466d4756')
        actual_json.delete(:_links)

        expect(actual_json).to eq({
                                    id: 'foo.example',
                                    type: 'authorization',
                                    plugin_file_location: '/path/to/foo.jar',
                                    bundled_plugin: false,
                                    status: {
                                      state: 'active'
                                    },
                                    about: about_json,
                                    extension_info: {
                                      auth_config_settings: ApiV3::Plugin::PluggableInstanceSettingsRepresenter.new(auth_config_settings).to_hash(url_builder: UrlBuilder.new),
                                      role_settings: ApiV3::Plugin::PluggableInstanceSettingsRepresenter.new(role_config_settings).to_hash(url_builder: UrlBuilder.new),
                                      capabilities: ApiV3::Plugin::AuthorizationCapabilitiesRepresenter.new(capabilities).to_hash(url_builder: UrlBuilder.new)
                                    }
                                  })

      end
    end

  describe 'authentication plugin info' do
      it 'should describe an authentication plugin' do
        vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
        about = GoPluginDescriptor::About.new('Foo plugin', '1.2.3', '17.2.0', 'Does foo', vendor, ['Linux'])
        descriptor = GoPluginDescriptor.new('foo.example', '1.0', about, '/path/to/foo.jar', nil, false)

        auth_view = com.thoughtworks.go.plugin.domain.common.PluginView.new('role_config_view_template')
        metadata = com.thoughtworks.go.plugin.domain.common.Metadata.new(true, false)
        plugin_settings = com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings.new([com.thoughtworks.go.plugin.domain.common.PluginConfiguration.new('memberOf', metadata)], auth_view)

        plugin_info = com.thoughtworks.go.plugin.domain.authentication.AuthenticationPluginInfo.new(descriptor, "display_name", 'display_image_url', true, true, plugin_settings)
        actual_json = ApiV3::Plugin::PluginInfoRepresenter.new(plugin_info).to_hash(url_builder: UrlBuilder.new)
        actual_json.delete(:_links)

        expect(actual_json).to eq({
                                    id: 'foo.example',
                                    type: 'authentication',
                                    plugin_file_location: '/path/to/foo.jar',
                                    bundled_plugin: false,
                                    status: {
                                      state: 'active'
                                    },
                                    about: about_json,
                                    extension_info: {
                                      plugin_settings: ApiV3::Plugin::PluggableInstanceSettingsRepresenter.new(plugin_settings).to_hash(url_builder: UrlBuilder.new),
                                      display_name: 'display_name',
                                      display_image_url: 'display_image_url',
                                      supports_password_based_authentication: true,
                                      supports_web_based_authentication: true,
                                    }
                                  })

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
