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
  module Security
    class AuthConfigRepresenter < PluginProfileRepresenter
      alias_method :auth_config, :represented


      link :self do |opts|
        opts[:url_builder].apiv1_admin_security_auth_config_url(auth_config_id: auth_config.id) unless auth_config.id.blank?
      end

      link :doc do |opts|
        CurrentGoCDVersion.api_docs_url('#authorization-configuration')
      end

      link :find do |opts|
        opts[:url_builder].apiv1_admin_security_auth_config_url(auth_config_id: ':auth_config_id')
      end

    end
  end
end
