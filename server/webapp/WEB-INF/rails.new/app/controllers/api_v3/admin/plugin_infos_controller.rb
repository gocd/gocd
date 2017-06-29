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
  module Admin
    class PluginInfosController < BaseController

      java_import com.thoughtworks.go.plugin.domain.common.BadPluginInfo

      before_action :check_admin_user_or_group_admin_user_and_401

      def index
        plugin_infos = default_plugin_info_finder.allPluginInfos(params[:type])

        if params[:include_bad].to_bool
          plugin_infos += default_plugin_manager.plugins().find_all(&:isInvalid).collect {|descriptor| BadPluginInfo.new(descriptor)}
        end
        render DEFAULT_FORMAT => Plugin::PluginInfosRepresenter.new(plugin_infos).to_hash(url_builder: self)
      rescue InvalidPluginTypeException
        raise UnprocessableEntity, "Invalid plugins type - `#{params[:type]}` !"
      end

      def show
        plugin_info = default_plugin_info_finder.pluginInfoFor(params[:id])

        unless plugin_info
          descriptor = default_plugin_manager.getPluginDescriptorFor(params[:id])
          plugin_info = BadPluginInfo.new(descriptor) if descriptor.try(:isInvalid)
        end

        raise RecordNotFound unless plugin_info

        render DEFAULT_FORMAT => Plugin::PluginInfoRepresenter.new(plugin_info).to_hash(url_builder: self)
      end

    end
  end
end
