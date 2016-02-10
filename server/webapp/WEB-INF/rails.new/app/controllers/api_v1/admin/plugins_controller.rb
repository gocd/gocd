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
      before_action :check_admin_user_and_401

      def index
        plugin_view_models = plugin_service.populatePluginViewModels()
        json               = ApiV1::Plugins::PluginsRepresenter.new(plugin_view_models).to_hash(url_builder: self)
        render json_hal_v1: json
      end

      def show
        type      = params[:type]
        plugin_id = params[:plugin_id]

        if (type && plugin_id)
          plugin_view_model = plugin_service.populatePluginViewModel(type, plugin_id)
          if plugin_view_model
            json = ApiV1::Plugins::PluginRepresenter.new(plugin_view_model).to_hash(url_builder: self)
            render json_hal_v1: json
          else
            raise ApiV1::UnprocessableEntity, "Invalid plugin id '#{plugin_id}' or invalid plugin type '#{type}'. Type has to be one of 'scm','package-repository', 'task'"
          end
        end

        if (type && plugin_id.blank?)
          plugin_view_models = plugin_service.populatePluginViewModelsOfType(type)
          if plugin_view_models
            json = ApiV1::Plugins::PluginsRepresenter.new(plugin_view_models, {type: type}).to_hash(url_builder: self)
            render json_hal_v1: json
          else
            raise ApiV1::UnprocessableEntity, "Invalid plugin type '#{type}'. It has to be one of 'scm', 'package-repository', 'task'"
          end
        end
      end
    end
  end
end
