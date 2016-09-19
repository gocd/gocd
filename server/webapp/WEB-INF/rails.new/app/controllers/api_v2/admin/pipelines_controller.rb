##########################################################################
# Copyright 2015 ThoughtWorks, Inc.
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

module ApiV2
  module Admin
    class PipelinesController < ApiV2::BaseController
      before_action :check_pipeline_group_admin_user_and_401
      before_action :load_pipeline, only: [:show, :update, :destroy]
      before_action :check_if_pipeline_by_same_name_already_exists, :check_group_not_blank, only: [:create]
      before_action :check_for_stale_request, :check_for_attempted_pipeline_rename, only: [:update]

      def show
        if stale?(etag: get_etag_for(@pipeline_config))
          json = ApiV2::Config::PipelineConfigRepresenter.new(@pipeline_config).to_hash(url_builder: self)
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
        pipeline_config_service.updatePipelineConfig(current_user, @pipeline_config_from_request, get_etag_for(@pipeline_config), result)
        handle_config_save_or_update_result(result)
      end

      def destroy
        result = HttpLocalizedOperationResult.new
        pipeline_config_service.deletePipelineConfig(current_user, @pipeline_config, result)
        render_http_operation_result(result)
      end

      private

      def get_pipeline_from_request
        @pipeline_config_from_request ||= PipelineConfig.new.tap do |config|
          ApiV2::Config::PipelineConfigRepresenter.new(config).from_hash(params[:pipeline], {go_config: go_config_service.getCurrentConfig()})
        end
      end

      def handle_config_save_or_update_result(result, pipeline_name = params[:pipeline_name])
        if result.isSuccessful
          load_pipeline(pipeline_name)
          json = ApiV2::Config::PipelineConfigRepresenter.new(@pipeline_config).to_hash(url_builder: self)
          response.etag = [get_etag_for(@pipeline_config)]
          render DEFAULT_FORMAT => json
        else
          json = ApiV2::Config::PipelineConfigRepresenter.new(@pipeline_config_from_request).to_hash(url_builder: self)
          render_http_operation_result(result, {data: json})
        end
      end

      def check_for_attempted_pipeline_rename
        unless CaseInsensitiveString.new(params[:pipeline][:name]) == CaseInsensitiveString.new(params[:pipeline_name])
          result = HttpLocalizedOperationResult.new
          result.notAcceptable(LocalizedMessage::string("PIPELINE_RENAMING_NOT_ALLOWED"))
          render_http_operation_result(result)
        end
      end

      def get_etag_for(pipeline)
        entity_hashing_service.md5ForEntity(pipeline)
      end

      def check_for_stale_request
        if request.env["HTTP_IF_MATCH"] != "\"#{Digest::MD5.hexdigest(get_etag_for(@pipeline_config))}\""
          result = HttpLocalizedOperationResult.new
          result.stale(LocalizedMessage::string("STALE_RESOURCE_CONFIG", 'pipeline', params[:pipeline_name]))
          render_http_operation_result(result)
        end
      end

      def load_pipeline(pipeline_name = params[:pipeline_name])
        @pipeline_config = pipeline_config_service.getPipelineConfig(pipeline_name)
        raise RecordNotFound if @pipeline_config.nil?
      end

      def check_if_pipeline_by_same_name_already_exists
        if (!pipeline_config_service.getPipelineConfig(params[:pipeline_name]).nil?)
          result = HttpLocalizedOperationResult.new
          result.unprocessableEntity(LocalizedMessage::string("CANNOT_CREATE_PIPELINE_ALREADY_EXISTS", params[:pipeline_name]))
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
