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

module ApiV5
  module Admin
    class PipelinesController < BaseController
      before_action :check_pipeline_group_admin_user_and_401
      before_action :load_pipeline, only: [:show, :update, :destroy]
      before_action :check_if_pipeline_is_defined_remotely, only: [:update, :destroy]
      before_action :check_if_pipeline_by_same_name_already_exists, :check_group_not_blank, only: [:create]
      before_action :check_for_stale_request, :check_for_attempted_pipeline_rename, only: [:update]

      def show
        if stale?(strong_etag: etag_for(@pipeline_config), template: false)
          json = Admin::Pipelines::PipelineConfigRepresenter.new(@pipeline_config).to_hash(url_builder: self)
          render DEFAULT_FORMAT => json
        end
      end

      def create
        result = HttpLocalizedOperationResult.new
        get_pipeline_from_request
        pipeline_config_service.createPipelineConfig(current_user, @pipeline_config_from_request, result, params[:group])
        handle_config_save_or_update_result(result, @pipeline_config_from_request.name.to_s)
        if result.isSuccessful
          pipeline_pause_service.pause(@pipeline_config_from_request.name.to_s, "Under construction", current_user)
        end
      end

      def update
        result = HttpLocalizedOperationResult.new
        get_pipeline_from_request
        pipeline_config_service.updatePipelineConfig(current_user, @pipeline_config_from_request, etag_for(@pipeline_config), result)
        handle_config_save_or_update_result(result)
      end

      def destroy
        result = HttpLocalizedOperationResult.new
        pipeline_config_service.deletePipelineConfig(current_user, @pipeline_config, result)
        render_http_operation_result(result)
      end

      protected

      def get_pipeline_from_request
        @pipeline_config_from_request ||= PipelineConfig.new.tap do |config|
          Admin::Pipelines::PipelineConfigRepresenter.new(config).from_hash(params[:pipeline], {go_config: go_config_service.getCurrentConfig()})
        end
      end

      def handle_config_save_or_update_result(result, pipeline_name = params[:pipeline_name])
        if result.isSuccessful
          load_pipeline(pipeline_name)
          json = Admin::Pipelines::PipelineConfigRepresenter.new(@pipeline_config).to_hash(url_builder: self)
          response.etag = [etag_for(@pipeline_config)]
          render DEFAULT_FORMAT => json
        else
          json = Admin::Pipelines::PipelineConfigRepresenter.new(@pipeline_config_from_request).to_hash(url_builder: self)
          render_http_operation_result(result, {data: json})
        end
      end

      def check_for_attempted_pipeline_rename
        unless CaseInsensitiveString.new(params[:pipeline][:name]) == CaseInsensitiveString.new(params[:pipeline_name])
          result = HttpLocalizedOperationResult.new
          result.notAcceptable(LocalizedMessage::string("RENAMING_NOT_ALLOWED", 'pipeline'))
          render_http_operation_result(result)
        end
      end

      def stale_message
        LocalizedMessage::string("STALE_RESOURCE_CONFIG", 'pipeline', params[:pipeline_name])
      end

      def etag_for_entity_in_config
        etag_for(@pipeline_config)
      end

      def load_pipeline(pipeline_name = params[:pipeline_name])
        @pipeline_config = pipeline_config_service.getPipelineConfig(pipeline_name)
        raise RecordNotFound if @pipeline_config.nil?
      end

      def check_if_pipeline_by_same_name_already_exists
        if (!pipeline_config_service.getPipelineConfig(params[:pipeline][:name]).nil?)
          result = HttpLocalizedOperationResult.new
          result.unprocessableEntity(LocalizedMessage::string("RESOURCE_ALREADY_EXISTS", 'pipeline', params[:pipeline][:name]))
          render_http_operation_result(result)
        end
      end

      def check_if_pipeline_is_defined_remotely
        unless @pipeline_config.isLocal()
          result = HttpLocalizedOperationResult.new
          pipeline_name = @pipeline_config.name()
          origin = @pipeline_config.getOrigin().displayName()
          result.unprocessableEntity(LocalizedMessage.string("CAN_NOT_OPERATE_ON_REMOTE_ENTITY", "pipeline", pipeline_name, origin))
          render_http_operation_result(result)
        end
      end

      def check_group_not_blank
        if (params[:group].blank?)
          result = HttpLocalizedOperationResult.new
          result.unprocessableEntity(LocalizedMessage::string("PIPELINE_GROUP_MANDATORY_FOR_PIPELINE_CREATE"))
          render_http_operation_result(result)
        end
      end
    end
  end
end
