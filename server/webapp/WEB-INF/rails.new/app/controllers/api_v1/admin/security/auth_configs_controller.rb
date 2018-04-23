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
    module Security
      class AuthConfigsController < BaseController
        before_action :check_admin_user_and_401
        before_action :check_for_stale_request, :check_for_attempted_rename, only: [:update]

        include ProfilesControllerActions

        def verify_connection
          entity_from_request = entity_representer.new(create_config_entity).from_hash(entity_json_from_request)
          response = security_auth_config_service.verify_connection(entity_from_request)
          handle_verify_connection_response(response, entity_from_request)
        end

        protected

        def handle_verify_connection_response(response, auth_config)
          if response.isSuccessful()
            render_verify_response_message(response, auth_config, 200)
          else
            render_verify_response_message(response, auth_config, 422)
          end
        end

        def render_verify_response_message(response, auth_config, status)
          auth_config_json = entity_representer.new(auth_config).to_hash(url_builder: self)
          render DEFAULT_FORMAT => {status: response.getStatus(), message: response.getMessage(), auth_config: auth_config_json}, status: status
        end

        def entity_json_from_request
          params[:auth_config]
        end

        def service
          security_auth_config_service
        end

        def all_entities_representer
          ApiV1::Security::AuthConfigsRepresenter
        end

        def entity_representer
          ApiV1::Security::AuthConfigRepresenter
        end

        def create_config_entity
          SecurityAuthConfig.new
        end

        def load_entity_from_config
          security_auth_config_service.findProfile(params[:auth_config_id]) || (raise RecordNotFound)
        end

        def stale_message
          LocalizedMessage::string('STALE_RESOURCE_CONFIG', 'Security auth config', params[:auth_config_id])
        end

        def etag_for_entity_in_config
          etag_for(load_entity_from_config)
        end

        def check_for_attempted_rename
          unless params[:auth_config].try(:[], :id).to_s == params[:auth_config_id].to_s
            render_message('Renaming of security auth config IDs is not supported by this API.', :unprocessable_entity)
          end
        end

      end
    end
  end
end