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
      class RolesController < BaseController
        before_filter :check_admin_user_and_401
        before_action :check_for_stale_request, :check_for_attempted_rename, only: [:update]

        def index
          roles = service.listAll().to_a

          if params[:role_type] == 'role'
            roles.keep_if { |role| role.kind_of?(RoleConfig) }
          elsif params[:role_type] == 'plugin_role'
            roles.keep_if { |role| role.kind_of?(PluginRoleConfig) }
          elsif params[:role_type].present?
            return render_message("Bad role type `#{params[:role_type]}`. Valid values are `role` and `plugin_role`", :bad_request)
          end

          render DEFAULT_FORMAT => ApiV1::Security::RolesConfigRepresenter.new(roles.to_a).to_hash(url_builder: self)
        end

        def show
          role = load_entity_from_config
          if stale?(etag: etag_for(role))
            render DEFAULT_FORMAT => ApiV1::Security::RolesConfigRepresenter.representer_instance_for(role).to_hash(url_builder: self)
          end
        end

        def update
          role = load_entity_from_config
          role_from_request = ApiV1::Security::RolesConfigRepresenter.representer_instance_from_hash(params[:role])

          result = HttpLocalizedOperationResult.new
          service.update(current_user, etag_for(role), role_from_request, result)
          handle_create_or_update_response(result, role_from_request)
        end

        def create
          result = HttpLocalizedOperationResult.new
          role = ApiV1::Security::RolesConfigRepresenter.representer_instance_from_hash(params[:role])
          service.create(current_user, role, result)
          handle_create_or_update_response(result, role)
        end

        def destroy
          result = HttpLocalizedOperationResult.new
          service.delete(current_user, load_entity_from_config, result)
          render_http_operation_result(result)
        end

        protected

        def service
          role_config_service
        end

        def load_entity_from_config
          role_config_service.findRole(params[:role_name]) || (raise RecordNotFound)
        end

        def stale_message
          LocalizedMessage::string('STALE_RESOURCE_CONFIG', 'role', params[:role_name])
        end

        def etag_for_entity_in_config
          etag_for(load_entity_from_config)
        end

        def entity_representer(role)
          ApiV1::Security::RolesConfigRepresenter.representer_class_for(role)
        end

        def handle_create_or_update_response(result, updated_role)
          json = entity_representer(updated_role).new(updated_role).to_hash(url_builder: self)
          if result.isSuccessful
            response.etag = [etag_for(updated_role)]
            render DEFAULT_FORMAT => json
          else
            render_http_operation_result(result, {data: json})
          end
        end

        def check_for_attempted_rename
          unless params[:role].try(:[], :name).to_s == params[:role_name].to_s
            render_message('Renaming of roles is not supported by this API.', :unprocessable_entity)
          end
        end

      end
    end
  end
end