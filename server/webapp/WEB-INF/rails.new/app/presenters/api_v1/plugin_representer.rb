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

module ApiV1
  class PluginRepresenter < BaseRepresenter
    alias_method :plugin, :represented

    link :self do |opts|
      opts[:url_builder].apiv1_admin_plugin_url(plugin.id())
    end

    link :doc do |opts|
      'http://api.go.cd/#plugins'
    end

    link :find do |opts|
      opts[:url_builder].apiv1_admin_plugin_url(id: '__plugin_id__').gsub(/__plugin_id__/, ':id')
    end

    property :id
    property :name
    property :version
    property :type
    property :viewTemplate, skip_nil: true
    collection :configurations,
               skip_nil: true,
               expect_hash: true,
               inherit: false,
               class: PluginConfigurationViewModel,
               decorator: ApiV1::PluginConfigurationRepresenter
  end
end