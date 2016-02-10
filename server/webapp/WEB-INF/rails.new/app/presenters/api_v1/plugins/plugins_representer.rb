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
  module Plugins
    class PluginsRepresenter < BaseRepresenter
      attr_accessor :plugins, :type

      def initialize(stages, params = [])
        @plugins = stages
        @type    = params[:type] unless params.empty?
      end

      link :self do |opts|
        if @type.blank?
          opts[:url_builder].apiv1_admin_all_plugin_list_api_url()
        else
          opts[:url_builder].apiv1_admin_plugin_list_api_url(type: type)
        end
      end

      link :doc do
        'http://api.go.cd/#plugins'
      end

      collection :plugins,
                 embedded:     true,
                 exec_context: :decorator,
                 decorator:    PluginRepresenter
    end
  end
end
