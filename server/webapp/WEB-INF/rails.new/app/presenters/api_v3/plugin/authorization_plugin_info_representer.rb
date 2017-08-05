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
module ApiV3
  module Plugin
    class AuthorizationPluginInfoRepresenter < BasePluginInfoRepresenter
      alias_method :plugin, :represented

      link :image do |opts|
        opts[:url_builder].plugin_images_url(plugin_id: plugin.getDescriptor.id, hash: plugin.getImage.getHash()) if plugin.image
      end

      property :auth_config_settings,
               skip_nil: true,
               expect_hash: true,
               inherit: false,
               class: com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings,
               decorator: PluggableInstanceSettingsRepresenter

      property :role_settings,
               skip_nil: true,
               expect_hash: true,
               inherit: false,
               class: com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings,
               decorator: PluggableInstanceSettingsRepresenter

      property :capabilities,
               skip_nil: true,
               expect_hash: true,
               inherit: false,
               class: Capabilities,
               decorator: AuthorizationCapabilitiesRepresenter

    end
  end
end