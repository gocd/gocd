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
    class PluginInfosRepresenter < BaseRepresenter
      alias_method :plugins, :represented

      link :self do |opts|
        opts[:url_builder].apiv3_admin_plugin_info_index_url
      end

      link :find do |opts|
        opts[:url_builder].apiv3_admin_plugin_info_url(id: '__plugin_id__').gsub(/__plugin_id__/, ':plugin_id')
      end

      link :doc do
        'https://api.gocd.org/#plugin-info'
      end

      collection :plugins,
                 as: :plugin_info,
                 embedded: true,
                 exec_context: :decorator,
                 decorator: PluginInfoRepresenter

    end
  end
end
