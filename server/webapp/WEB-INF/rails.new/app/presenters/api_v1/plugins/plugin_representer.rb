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
    class PluginRepresenter < BaseRepresenter

      TYPE_TO_REPRESENTER_MAP = {
        'scm'                => ApiV1::Plugins::ScmRepresenter,
        'package-repository' => ApiV1::Plugins::PackageRepositoryRepresenter,
        'task'               => ApiV1::Plugins::TaskRepresenter,
      }
      alias_method :plugin, :represented

      link :self do |opts|
        opts[:url_builder].apiv1_admin_plugin_api_url(type: plugin.getType, plugin_id: plugin.plugin_id) unless plugin.class::TYPE.eql?(DisabledPluginViewModel::TYPE)
      end

      link :doc do
        'http://api.go.cd/#plugins'
      end

      property :plugin_id
      property :version
      property :type
      property :message, skip_nil: true
      property :configurations,
               exec_context: :decorator,
               decorator:    lambda { |opts, *|
                 TYPE_TO_REPRESENTER_MAP[plugin.class::TYPE]
               }, skip_nil:  true

      def configurations
        plugin unless plugin.class::TYPE.eql?(DisabledPluginViewModel::TYPE)
      end

      def message
        plugin.status
      end
    end
  end
end
