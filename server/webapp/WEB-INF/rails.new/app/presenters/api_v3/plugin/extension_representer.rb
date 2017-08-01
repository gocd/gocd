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
    class ExtensionRepresenter < BaseRepresenter

      property :auth_config_settings,
               skip_nil: true,
               getter: lambda {|opts| self[:auth_config_settings]},
               class: com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings,
               decorator: PluggableInstanceSettingsRepresenter

      property :role_settings,
               skip_nil: true,
               getter: lambda {|opts| self[:role_settings]},
               class: com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings,
               decorator: PluggableInstanceSettingsRepresenter

      property :profile_settings,
               skip_nil: true,
               getter: lambda {|opts| self[:profile_settings]},
               class: com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings,
               decorator: PluggableInstanceSettingsRepresenter

      property :package_settings,
               skip_nil: true,
               getter: lambda {|opts| self[:package_settings]},
               class: com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings,
               decorator: PluggableInstanceSettingsRepresenter


      property :repository_settings,
               skip_nil: true,
               getter: lambda {|opts| self[:repository_settings]},
               class: com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings,
               decorator: PluggableInstanceSettingsRepresenter

      property :scm_settings,
               skip_nil: true,
               getter: lambda {|opts| self[:scm_settings]},
               class: com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings,
               decorator: PluggableInstanceSettingsRepresenter

      property :task_settings,
               skip_nil: true,
               getter: lambda {|opts| self[:task_settings]},
               class: com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings,
               decorator: PluggableInstanceSettingsRepresenter
    end
  end
end
