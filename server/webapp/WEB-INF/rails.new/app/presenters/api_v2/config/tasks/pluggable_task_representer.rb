##########################################################################
# Copyright 2016 ThoughtWorks, Inc.
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

module ApiV2
  module Config
    module Tasks
      class PluggableTaskRepresenter < ApiV2::Config::Tasks::BaseTaskRepresenter
        alias_method :pluggable_task, :represented

        property :plugin_configuration, decorator: ApiV2::Config::PluginConfigurationRepresenter, class: com.thoughtworks.go.domain.config.PluginConfiguration
        collection :configuration, exec_context: :decorator, decorator: PluginConfigurationPropertyRepresenter, class: com.thoughtworks.go.domain.config.ConfigurationProperty

        def configuration
          pluggable_task.getConfiguration()
        end

        def configuration=(value)
          pluggable_task.addConfigurations(value)
        end
      end
    end
  end
end
