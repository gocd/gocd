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
  module Admin
    class PluginsController < ApiV1::BaseController
      before_action :check_admin_user_or_group_admin_user_and_401

      def index
        render json_hal_v1: ApiV1::PluginsRepresenter.new(plugin_service.plugins(params[:type])).to_hash(url_builder: self)
      end

      def show
        plugin = plugin_service.plugin(params[:id])

        raise RecordNotFound if plugin.nil?

        render json_hal_v1: ApiV1::PluginRepresenter.new(plugin).to_hash(url_builder: self)
      end
    end
  end
end
