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
    class EnvironmentsController < ApiV1::BaseController
      before_action :check_admin_user_and_401
      before_action :load_environment, only: [:show, :put, :patch, :destroy]
      before_action :check_for_stale_request, only: [:put]

      def index
        render DEFAULT_FORMAT => ApiV1::Config::EnvironmentsConfigRepresenter.new(environment_config_service.getEnvironments()).to_hash(url_builder: self)
      end

      def show
        json = ApiV1::Config::EnvironmentConfigRepresenter.new(@environment_config).to_hash(url_builder: self)
        render DEFAULT_FORMAT => json if stale?(etag: get_etag_for_environment)
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
        environment_config_service.updateEnvironment(@environment_config, @environment_config_from_request, current_user, get_etag_for_environment, result)
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

        environment_config_service.patchEnvironment(@environment_config, pipelines_to_add, pipelines_to_remove, agents_to_add, agents_to_remove, current_user, result)
        handle_config_save_result(result, @environment_config.name.to_s)
      end

      def destroy
        result = HttpLocalizedOperationResult.new
        environment_config_service.deleteEnvironment(@environment_config, current_user, result)
        render_http_operation_result(result)
      end

      private

      def load_environment(environment_name = params[:name])
        @environment_config = environment_config_service.getEnvironmentConfig(environment_name)
      rescue com.thoughtworks.go.config.exceptions.NoSuchEnvironmentException
        raise ApiV1::RecordNotFound
      end

      def get_environment_from_request
        @environment_config_from_request ||= BasicEnvironmentConfig.new.tap do |config|
          ApiV1::Config::EnvironmentConfigRepresenter.new(config).from_hash(params[:environment])
        end
      end

      def get_etag_for_environment
        entity_hashing_service.md5ForEntity(@environment_config)
      end

      def handle_config_save_result(result, environment_name)
        if result.isSuccessful
          load_environment(environment_name)
          json = ApiV1::Config::EnvironmentConfigRepresenter.new(@environment_config).to_hash(url_builder: self)
          response.etag = [get_etag_for_environment]
          render DEFAULT_FORMAT => json
        else
          render_http_operation_result(result)
        end
      end

      def check_for_stale_request
        if request.env['HTTP_IF_MATCH'] != "\"#{Digest::MD5.hexdigest(get_etag_for_environment)}\""
          result = HttpLocalizedOperationResult.new
          result.stale(LocalizedMessage::string('STALE_RESOURCE_CONFIG', 'environment', params[:name]))
          render_http_operation_result(result)
        end
      end
    end
  end
end
