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
  module Security
    class RoleConfigRepresenter < ApiV1::BaseRepresenter
      alias_method :role, :represented

      TYPE_TO_ROLE = {
        'gocd'   => RoleConfig,
        'plugin' => PluginRoleConfig
      }

      ROLE_TO_TYPE = TYPE_TO_ROLE.invert

      ROLE_TO_REPRESENTER = {
        PluginRoleConfig => PluginRoleConfigRepresenter,
        RoleConfig       => GocdRoleConfigRepresenter,
      }

      error_representer({'authConfigId' => 'auth_config_id'})

      link :self do |opts|
        opts[:url_builder].apiv1_admin_security_role_url(role_name: role.name.to_s) unless role.name.blank?
      end

      link :doc do |opts|
        'https://api.gocd.org/#roles'
      end

      link :find do |opts|
        opts[:url_builder].apiv1_admin_security_role_url(role_name: '__role_name__').gsub('__role_name__', ':role_name')
      end

      property :name, case_insensitive_string: true

      property :type, getter: lambda { |options| ROLE_TO_TYPE[self.class] }, skip_parse: true

      nested :attributes, decorator: lambda { |role, *| ROLE_TO_REPRESENTER[role.class] }

      class << self
        def get_role_type(type)
          TYPE_TO_ROLE[type] or (raise UnprocessableEntity, "Invalid role type '#{type}'. It has to be one of '#{TYPE_TO_ROLE.keys.join(' ')}'")
        end
      end
    end
  end
end
