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
  module Security
    class RolesConfigRepresenter < ApiV1::BaseRepresenter
      alias_method :roles, :represented

      ROLE_TO_REPRESENTER_MAP = {
        PluginRoleConfig => PluginRoleConfigRepresenter,
        RoleConfig => RoleConfigRepresenter,
      }

      link :self do |opts|
        opts[:url_builder].apiv1_admin_security_roles_url
      end

      link :doc do |opts|
        'https://api.gocd.io/#roles'
      end

      link :find do |opts|
        opts[:url_builder].apiv1_admin_security_role_url(role_name: '__role_name__').gsub('__role_name__', ':role_name')
      end

      collection :roles, embedded: true, exec_context: :decorator, decorator: lambda { |role, *|
        RolesConfigRepresenter.representer_class_for(role)
      }

      class << self
        include JavaImports
        def representer_class_for(role)
          ROLE_TO_REPRESENTER_MAP[role.class] || (raise ApiV1::UnprocessableEntity, "Invalid role type '#{type}'. It has to be one of '#{ROLE_TO_REPRESENTER_MAP.keys.join(', ')}.'")
        end

        def representer_instance_for(role)
          representer_class_for(role).new(role)
        end

        def representer_instance_from_hash(role)
          if role[:auth_config_id]
            PluginRoleConfigRepresenter.new(PluginRoleConfig.new).from_hash(role)
          else
            RoleConfigRepresenter.new(RoleConfig.new).from_hash(role)
          end
        end
      end


    end
  end
end
