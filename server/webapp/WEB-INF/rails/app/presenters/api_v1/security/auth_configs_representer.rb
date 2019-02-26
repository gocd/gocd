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
    class AuthConfigsRepresenter < BaseRepresenter
      alias_method :profiles, :represented
      link :self do |opts|
        opts[:url_builder].apiv1_admin_security_auth_configs_url
      end

      link :doc do |opts|
        'https://api.gocd.org/#authorization-configuration'
      end

      link :find do |opts|
        opts[:url_builder].apiv1_admin_security_auth_config_url(auth_config_id: '__auth_config_id__').gsub('__auth_config_id__', ':auth_config_id')
      end

      collection :profiles, as: :auth_configs, embedded: true, exec_context: :decorator, decorator: AuthConfigRepresenter

    end
  end
end
