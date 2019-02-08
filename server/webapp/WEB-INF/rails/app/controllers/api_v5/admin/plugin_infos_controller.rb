##########################################################################
# Copyright 2018 ThoughtWorks, Inc.
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

module ApiV5
  module Admin
    class PluginInfosController < BaseController

      before_action :check_user_and_403

      PLUGIN_TYPES_FOR_VERSION = [
        PluginConstants.AUTHORIZATION_EXTENSION,
        PluginConstants.ELASTIC_AGENT_EXTENSION,
        PluginConstants.NOTIFICATION_EXTENSION,
        PluginConstants.SCM_EXTENSION,
        PluginConstants.PLUGGABLE_TASK_EXTENSION,
        PluginConstants.PACKAGE_MATERIAL_EXTENSION,
        PluginConstants.CONFIG_REPO_EXTENSION,
        PluginConstants.ANALYTICS_EXTENSION,
        PluginConstants.ARTIFACT_EXTENSION
      ]

      def index
        plugin_infos = default_plugin_info_finder.allPluginInfos(params[:type]).reject do |combined_plugin_info|
          combined_plugin_info.nil? || is_non_nil_and_unsupported?(combined_plugin_info)
        end

        if params[:include_bad].to_bool
          plugin_infos += default_plugin_manager.plugins().find_all(&:isInvalid).collect {|descriptor| BadPluginInfo.new(descriptor)}
        end
        render DEFAULT_FORMAT => Plugin::PluginInfosRepresenter.new(plugin_infos).to_hash(url_builder: self)
      rescue InvalidPluginTypeException
        raise UnprocessableEntity, "Invalid plugins type - `#{params[:type]}` !"
      end

      def show
        plugin_info = default_plugin_info_finder.pluginInfoFor(params[:id])

        raise RecordNotFound if is_non_nil_and_unsupported?(plugin_info)

        unless plugin_info
          descriptor = default_plugin_manager.getPluginDescriptorFor(params[:id])
          plugin_info = BadPluginInfo.new(descriptor) if descriptor.try(:isInvalid)
        end

        raise RecordNotFound unless plugin_info

        render DEFAULT_FORMAT => Plugin::PluginInfoRepresenter.new(plugin_info).to_hash(url_builder: self)
      end

      private
      def is_non_nil_and_unsupported? plugin_info
        !plugin_info.nil? && !(plugin_info.extensionNames() - PLUGIN_TYPES_FOR_VERSION).empty?
      end
    end
  end
end
