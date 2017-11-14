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

module ApiV4
  module Admin
    class TemplatesController < ApiV4::BaseController
      before_action :load_template, only: [:show, :update, :destroy]
      before_action :check_admin_user_or_group_admin_user_and_401, only: [:create]
      before_action :check_admin_or_template_admin_and_401, only: [:destroy, :update]
      before_action :check_view_access_to_template_and_401, only: [:show, :index]
      before_action :check_for_stale_request, :check_for_attempted_template_rename, only: [:update]

      def index
        templates = template_config_service.getTemplatesList(current_user)
        json = ApiV4::Admin::Templates::TemplatesConfigRepresenter.new(templates).to_hash(url_builder: self)
        render DEFAULT_FORMAT => json
      end

      def show
        json = ApiV4::Admin::Templates::TemplateConfigRepresenter.new(@template).to_hash(url_builder: self)
        render DEFAULT_FORMAT => json if stale?(strong_etag: etag_for(@template), template: false)
      end

      def create
        result = HttpLocalizedOperationResult.new
        @template = ApiV4::Admin::Templates::TemplateConfigRepresenter.new(PipelineTemplateConfig.new).from_hash(params[:template])
        template_config_service.createTemplateConfig(current_user, @template, result)
        handle_create_or_update_response(result, @template)
      end

      def update
        result = HttpLocalizedOperationResult.new
        updated_template = ApiV4::Admin::Templates::TemplateConfigRepresenter.new(PipelineTemplateConfig.new).from_hash(params[:template])
        template_config_service.updateTemplateConfig(current_user, updated_template, result, etag_for(@template))
        handle_create_or_update_response(result, updated_template)
      end

      def destroy
        result = HttpLocalizedOperationResult.new
        template_config_service.deleteTemplateConfig(current_user, @template, result)
        render_http_operation_result(result)
      end

      protected

      def load_template(template_name = params[:template_name])
        result = HttpLocalizedOperationResult.new
        @template = template_config_service.loadForView(template_name, result)
        raise RecordNotFound unless @template
      end

      def stale_message
        LocalizedMessage::string('STALE_RESOURCE_CONFIG', 'Template', params[:template][:name])
      end

      def etag_for_entity_in_config
        etag_for(@template)
      end

      def check_for_attempted_template_rename
        unless params[:template][:name].downcase == params[:template_name].downcase
          render_message('Renaming of Templates is not supported by this API.', :unprocessable_entity)
        end
      end

      def handle_create_or_update_response(result, updated_template)
        json = ApiV4::Admin::Templates::TemplateConfigRepresenter.new(updated_template).to_hash(url_builder: self)
        if result.isSuccessful
          response.etag = [etag_for(updated_template)]
          render DEFAULT_FORMAT => json
        else
          render_http_operation_result(result, {data: json})
        end
      end

    end
  end
end

