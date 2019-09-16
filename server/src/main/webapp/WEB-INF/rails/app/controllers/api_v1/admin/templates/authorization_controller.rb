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
    module Templates
      class AuthorizationController < BaseController

        before_action :check_admin_user_and_403
        before_action :load_template, only: [:show, :update]
        before_action :check_for_stale_request, only: [:update]

        def show
          json = ApiV1::Admin::Authorization::AuthorizationConfigRepresenter.new(@template.getAuthorization).to_hash(url_builder: self)
          render DEFAULT_FORMAT => json if stale?(strong_etag: etag_for(@template))
        end

        def update
          result = HttpLocalizedOperationResult.new
          updated_authorization = ApiV1::Admin::Authorization::AuthorizationConfigRepresenter.new(com.thoughtworks.go.config.Authorization.new).from_hash(params[:authorization])
          template_config_service.updateTemplateAuthConfig(current_user, @template, updated_authorization, result, etag_for(@template))
          handle_update_response(result, updated_authorization)
        end

        private
        def load_template(template_name = params[:template_name])
          result = HttpLocalizedOperationResult.new
          @template = template_config_service.loadForView(template_name, result)
          raise RecordNotFound unless @template
        end

        def handle_update_response(result, updated_authorization)
          json = ApiV1::Admin::Authorization::AuthorizationConfigRepresenter.new(updated_authorization).to_hash(url_builder: self)
          if result.isSuccessful
            response.etag = [etag_for(@template)]
            render DEFAULT_FORMAT => json
          else
            render_http_operation_result(result, {data: json})
          end
        end

        def etag_for_entity_in_config
          etag_for(@template)
        end

        def stale_message
          com.thoughtworks.go.i18n.LocalizedMessage::staleResourceConfig('Template', @template.name)
        end
      end
    end
  end
end
