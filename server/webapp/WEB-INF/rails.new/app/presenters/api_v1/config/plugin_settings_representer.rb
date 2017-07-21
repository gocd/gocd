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

module ApiV1
  module Config
    class PluginSettingsRepresenter < ApiV1::BaseRepresenter

      error_representer
      link :self do |opts|
        opts[:url_builder].apiv1_admin_plugin_setting_url(plugin_id: plugin_settings.getPluginId)
      end

      link :doc do |opts|
        'https://api.gocd.org/#plugin-settings'
      end

      link :find do |opts|
        opts[:url_builder].apiv1_admin_plugin_setting_url(plugin_id: '__plugin_id__').gsub(/__plugin_id__/, ':plugin_id')
      end

      property :errors, exec_context: :decorator, decorator: ApiV1::Config::ErrorRepresenter, skip_parse: true, skip_render: lambda { |object, options| object.empty? }
      property :plugin_id,
               exec_context: :decorator

      collection :configuration,
                 exec_context: :decorator,
                 decorator: ApiV1::Config::PluginConfigurationPropertyRepresenter,
                 class: com.thoughtworks.go.domain.config.ConfigurationProperty

      delegate :plugin_id, :plugin_id=, to: :plugin_settings

      def configuration=(plugin_settings_properties)
        plugin_settings.addConfigurations(represented[:plugin_info], plugin_settings_properties)
      end

      def configuration
        plugin_settings.getSecurePluginSettingsProperties(represented[:plugin_info])
      end

      def plugin_settings
        represented[:plugin_settings]
      end

      def errors
        plugin_settings.errors;
      end
    end
  end
end
