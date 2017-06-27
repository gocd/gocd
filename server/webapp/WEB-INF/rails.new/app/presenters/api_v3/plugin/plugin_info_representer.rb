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
    class PluginInfoRepresenter < BaseRepresenter

      REPRESENTER_FOR_PLUGIN_INFO_TYPE = {
        com.thoughtworks.go.plugin.domain.authentication.AuthenticationPluginInfo => AuthenticationPluginInfoRepresenter,
        com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo => AuthorizationPluginInfoRepresenter,
        com.thoughtworks.go.plugin.domain.notification.NotificationPluginInfo => NotificationPluginInfoRepresenter,
        com.thoughtworks.go.plugin.domain.packagematerial.PackageMaterialPluginInfo => PackageRepositoryPluginInfoRepresenter,
        com.thoughtworks.go.plugin.domain.pluggabletask.PluggableTaskPluginInfo => PluggableTaskPluginInfoRepresenter,
        com.thoughtworks.go.plugin.domain.scm.SCMPluginInfo => SCMPluginInfoRepresenter,
        com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo => ElasticPluginInfoRepresenter,
        com.thoughtworks.go.plugin.domain.configrepo.ConfigRepoPluginInfo => ConfigRepoPluginInfoRepresenter,
        com.thoughtworks.go.plugin.domain.common.BadPluginInfo => BadPluginInfoRepresenter,
      }

      class <<self
        def new(plugin)
          representer_for_type(plugin.class).new(plugin)
        end

        def representer_for_type(type)
          REPRESENTER_FOR_PLUGIN_INFO_TYPE[type] || (raise UnprocessableEntity, "Invalid plugin type '#{type}'. It has to be one of '#{REPRESENTER_FOR_PLUGIN_INFO_TYPE.keys.join(', ')}.'")
        end
      end
    end
  end
end
