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

module ApiV1
  module Admin
    module Authorization
      class TemplatesController < BaseController
        CLONER = Cloner.new

        before_action :load_template, only: [:show, :update]
        before_action :check_admin_user_and_401
        before_action :check_for_stale_request, only: [:update]

        def show
          json = ApiV1::Admin::Authorization::AuthorizationConfigRepresenter.new(@template.getAuthorization).to_hash(url_builder: self)
          render DEFAULT_FORMAT => json if stale?(etag: etag_for(@template))
        end

        def update
          result = HttpLocalizedOperationResult.new
          authorization = ApiV1::Admin::Authorization::AuthorizationConfigRepresenter.new(com.thoughtworks.go.config.Authorization.new).from_hash(params[:template])
          updated_template = CLONER.deepClone(@template)
          updated_template.setAuthorization(authorization)
          template_config_service.updateTemplateAuthConfig(current_user, updated_template, result, etag_for(@template))
          handle_create_or_update_response(result, updated_template)
        end

        def load_template(template_name = params[:template_name])
          result = HttpLocalizedOperationResult.new
          @template = template_config_service.loadForView(template_name, result)
          raise RecordNotFound unless @template
        end

        def handle_create_or_update_response(result, updated_template)
          json = ApiV1::Admin::Authorization::AuthorizationConfigRepresenter.new(updated_template.getAuthorization).to_hash(url_builder: self)
          if result.isSuccessful
            response.etag = [etag_for(updated_template)]
            render DEFAULT_FORMAT => json
          else
            render_http_operation_result(result, {data: json})
          end
        end

        def etag_for_entity_in_config
          etag_for(@template)
        end

        def stale_message
          LocalizedMessage::string('STALE_RESOURCE_CONFIG', 'Template', @template.name)
        end
      end
    end
  end
end