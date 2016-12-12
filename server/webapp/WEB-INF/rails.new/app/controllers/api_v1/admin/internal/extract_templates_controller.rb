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
    module Internal
      class ExtractTemplatesController < ApiV1::BaseController
        before_action :check_admin_user_and_401

        def create
          result = HttpLocalizedOperationResult.new
          @template = ApiV3::Config::TemplateConfigRepresenter.new(PipelineTemplateConfig.new).from_hash(params[:extract_template])
          pipeline_to_extract_from = params[:pipeline]
          template_config_service.extractFromPipeline(@template, pipeline_to_extract_from, current_user, result)
          handle_create_or_update_response(result, @template)
        end

        private

        def handle_create_or_update_response(result, updated_template)
          json = ApiV3::Config::TemplateConfigRepresenter.new(updated_template).to_hash(url_builder: self)
          if result.isSuccessful
            render DEFAULT_FORMAT => json
          else
            render_http_operation_result(result, {data: json})
          end
        end
      end
    end
  end
end
