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
        before_action :check_admin_user_and_401
        before_action :check_for_stale_request, :check_for_attempted_rename, only: [:update]
        before_action :check_if_role_by_same_name_already_exists, only: [:create]

        GOCD_ROLE   = 'gocd'
        PLUGIN_ROLE = 'plugin'

        def index
          return render_message("Bad role type `#{params[:type]}`. Valid values are `gocd` and `plugin`", :bad_request) unless is_valid_plugin_type?

          roles = role_config_service.listAll().to_a
          filter_roles(roles)

          render DEFAULT_FORMAT => ApiV1::Security::RolesConfigRepresenter.new(roles.to_a).to_hash(url_builder: self)
        end

        def show
          role = load_entity_from_config
          if stale?(etag: etag_for(role))
            render DEFAULT_FORMAT => ApiV1::Security::RoleConfigRepresenter.new(role).to_hash(url_builder: self)
          end
        end

        def update
          role = load_entity_from_config
          role_from_request = ApiV1::Security::RoleConfigRepresenter.new(role_for(params[:role][:type])).from_hash(params[:role])

          result = HttpLocalizedOperationResult.new
          role_config_service.update(current_user, etag_for(role), role_from_request, result)
          handle_create_or_update_response(result, role_from_request)
        end

        def create
          result = HttpLocalizedOperationResult.new
          role = ApiV1::Security::RoleConfigRepresenter.new(role_for(params[:role][:type])).from_hash(params[:role])
          role_config_service.create(current_user, role, result)
          handle_create_or_update_response(result, role)
        end

        def destroy
          result = HttpLocalizedOperationResult.new
          role_config_service.delete(current_user, load_entity_from_config, result)
          render_http_operation_result(result)
        end

        private

        def is_valid_plugin_type?
          return true if params[:type].blank?

          [GOCD_ROLE, PLUGIN_ROLE].include?(params[:type])
        end

        def filter_roles(roles)
          type_to_role = {
            GOCD_ROLE   => RoleConfig,
            PLUGIN_ROLE => PluginRoleConfig
          }

          roles.keep_if { |role| role.kind_of?(type_to_role[params[:type]]) } unless params[:type].blank?
        end

        def role_for(type)
          ApiV1::Security::RoleConfigRepresenter.get_role_type(type).new
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

        def entity_representer
          ApiV1::Security::RoleConfigRepresenter
        end

        def check_for_attempted_rename
          unless params[:role].try(:[], :name).to_s == params[:role_name].to_s
            render_message('Renaming of roles is not supported by this API.', :unprocessable_entity)
          end
        end

        def check_if_role_by_same_name_already_exists
          if (!role_config_service.findRole(params[:role][:name]).nil?)
            role = ApiV1::Security::RoleConfigRepresenter.new(role_for(params[:role][:type])).from_hash(params[:role])
            role.addError('name', 'Role names should be unique. Role with the same name exists.')
            result = HttpLocalizedOperationResult.new
            result.unprocessableEntity(LocalizedMessage::string("RESOURCE_ALREADY_EXISTS", 'role', params[:role][:name]))
            render_http_operation_result(result, {data: ApiV1::Security::RoleConfigRepresenter.new(role).to_hash(url_builder: self)})
          end
        end
      end
    end
  end
end