#
# Copyright 2019 ThoughtWorks, Inc.
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
#

module ApiV1
  module Admin
    class PluginSettingsController < ApiV1::BaseController
      before_action :check_admin_user_and_403
      before_action :load_plugin_settings, only: [:show, :update]
      before_action :check_for_stale_request, only: [:update]

      def show
        json = ApiV1::Config::PluginSettingsRepresenter.new({plugin_settings: @plugin_settings, plugin_info: load_plugin_info(@plugin_settings.getPluginId)}).to_hash(url_builder: self)
        render DEFAULT_FORMAT => json if stale?(strong_etag: etag_for(@plugin_settings))
      end

      def create
        result = HttpLocalizedOperationResult.new
        object = ApiV1::Config::PluginSettingsRepresenter.new({plugin_settings: PluginSettings.new, plugin_info: load_plugin_info(params[:plugin_setting][:plugin_id])}).from_hash(params[:plugin_setting])
        @plugin_settings = object[:plugin_settings]
        plugin_service.createPluginSettings(@plugin_settings, current_user, result)
        handle_create_or_update_response(result, @plugin_settings)
      end

      def update
        result = HttpLocalizedOperationResult.new
        object = ApiV1::Config::PluginSettingsRepresenter.new({plugin_settings: PluginSettings.new, plugin_info: load_plugin_info(params[:plugin_setting][:plugin_id])}).from_hash(params[:plugin_setting])
        new_plugin_settings = object[:plugin_settings]
        plugin_service.updatePluginSettings(new_plugin_settings, current_user, result, etag_for(@plugin_settings))
        handle_create_or_update_response(result, new_plugin_settings)
      end

      protected

      def handle_create_or_update_response(result, updated_plugin_settings)
        json = ApiV1::Config::PluginSettingsRepresenter.new({plugin_settings: updated_plugin_settings, plugin_info: load_plugin_info(updated_plugin_settings.getPluginId)}).to_hash(url_builder: self)
        if result.isSuccessful
          response.etag = [etag_for(updated_plugin_settings)]
          render DEFAULT_FORMAT => json
        else
          render_http_operation_result(result, {data: json})
        end
      end

      def load_plugin_info(plugin_id)
        raise FailedDependency.new("The plugin with id '#{plugin_id}' is not loaded.") unless plugin_service.isPluginLoaded(plugin_id)
        plugin_info = plugin_service.pluginInfoForExtensionThatHandlesPluginSettings(plugin_id)
        raise UnprocessableEntity.new("The plugin with id '#{plugin_id}' does not support plugin-settings.") unless plugin_info
        plugin_info
      end

      def load_plugin_settings
        @plugin_settings = plugin_service.getPluginSettings(params[:plugin_id])
        raise RecordNotFound unless @plugin_settings
      end

      def etag_for_entity_in_config
        etag_for(@plugin_settings)
      end

      def stale_message
        com.thoughtworks.go.i18n.LocalizedMessage::staleResourceConfig('Plugin Settings', params[:plugin_setting][:plugin_id])
      end
    end
  end
end
