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
    class EnvironmentsController < BaseController
      before_action :check_admin_user_and_401
      before_action :load_local_environment, only: [:put, :patch, :destroy]
      before_action :check_for_stale_request, only: [:put]

      def index
        render DEFAULT_FORMAT => Admin::Environments::EnvironmentsConfigRepresenter.new(environment_config_service.getEnvironments()).to_hash(url_builder: self)
      end

      def show
        is_query_param_provided = params.length > 3
        is_with_remote = params[:withconfigrepo]
        environment_name = params[:name]

        if is_with_remote.nil? and !is_query_param_provided
          load_local_environment(environment_name)
        elsif is_with_remote and is_with_remote.downcase == 'true'
          load_merged_environment(environment_name)
        else
          return render_not_found_error
        end

        json = Admin::Environments::EnvironmentConfigRepresenter.new(@environment_config).to_hash(url_builder: self)
        render DEFAULT_FORMAT => json if stale?(etag: etag_for(environment_config_service.getEnvironmentForEdit(environment_name)))
      end

      def create
        result = HttpLocalizedOperationResult.new
        get_environment_from_request
        environment_config_service.createEnvironment(@environment_config_from_request, current_user, result)
        handle_config_save_result(result, @environment_config_from_request.name.to_s)
      end

      def put
        result = HttpLocalizedOperationResult.new
        get_environment_from_request
        environment_config_service.updateEnvironment(@environment_config.name().toString(), @environment_config_from_request, current_user, etag_for(@environment_config), result)
        handle_config_save_result(result, @environment_config_from_request.name.to_s)
      end

      def patch
        result = HttpLocalizedOperationResult.new
        pipelines = params[:pipelines] || {}
        pipelines_to_add = pipelines[:add] || []
        pipelines_to_remove = pipelines[:remove] || []

        agents = params[:agents] || {}
        agents_to_add = agents[:add] || []
        agents_to_remove = agents[:remove] || []

        env_vars = params[:environment_variables] || {}

        env_vars_to_add = (env_vars[:add] || []).map { |env_var|
          Shared::EnvironmentVariableRepresenter.new(EnvironmentVariableConfig.new).from_hash(env_var)
        }

        env_vars_to_remove = env_vars[:remove] || []


        environment_config_service.patchEnvironment(@environment_config, pipelines_to_add, pipelines_to_remove, agents_to_add, agents_to_remove, env_vars_to_add, env_vars_to_remove, current_user, result)
        handle_config_save_result(result, @environment_config.name.to_s)
      end

      def destroy
        result = HttpLocalizedOperationResult.new
        environment_config_service.deleteEnvironment(@environment_config, current_user, result)
        render_http_operation_result(result)
      end

      protected

      def load_local_environment(environment_name = params[:name])
        @environment_config = environment_config_service.getEnvironmentForEdit(environment_name)
        raise RecordNotFound if @environment_config.nil?
        @environment_config.setOrigins(com.thoughtworks.go.config.remote.FileConfigOrigin.new)
      end

      def load_merged_environment(environment_name)
        result = HttpLocalizedOperationResult.new
        config_element = environment_config_service.getMergedEnvironmentforDisplay(environment_name, result)
        raise RecordNotFound if config_element.nil?
        @environment_config = config_element.getConfigElement()
      end

      def get_environment_from_request
        @environment_config_from_request ||= BasicEnvironmentConfig.new.tap do |config|
          Admin::Environments::EnvironmentConfigRepresenter.new(config).from_hash(params[:environment])
        end
      end

      def handle_config_save_result(result, environment_name)
        if result.isSuccessful
          load_local_environment(environment_name)
          json = Admin::Environments::EnvironmentConfigRepresenter.new(@environment_config).to_hash(url_builder: self)
          response.etag = [etag_for(@environment_config)]
          render DEFAULT_FORMAT => json
        else
          render_http_operation_result(result)
        end
      end

      def stale_message
        LocalizedMessage::string('STALE_RESOURCE_CONFIG', 'environment', params[:name])
      end

      def etag_for_entity_in_config
        etag_for(@environment_config)
      end
    end
  end
end
