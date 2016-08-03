##########################################################################
# Copyright 2015 ThoughtWorks, Inc.
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
  class VersionInfoRepresenter < ApiV1::BaseRepresenter
    def initialize(version_info, system_environment)
      @version_info = version_info
      @system_environment = system_environment
      super(@version_info)
    end

    link :doc do
      'https://api.go.cd/#versionInfo'
    end

    link :self do |opts|
      opts[:url_builder].apiv1_stale_version_info_url
    end

    property :getComponentName, as: :component_name
    property :update_server_url, exec_context: :decorator
    property :installed_version, exec_context: :decorator
    property :latest_version, exec_context: :decorator

    def installed_version
      @version_info.getInstalledVersion.toString
    end

    def latest_version
      @version_info.getLatestVersion.blank? ? nil : @version_info.getLatestVersion.to_string
    end

    def update_server_url
      uri = URI(@system_environment.getUpdateServerUrl)
      ar = URI.decode_www_form(uri.query || "") << ['current_version', @version_info.getInstalledVersion.toString || 'unknown']
      uri.query = URI.encode_www_form(ar)
      uri.to_s
    end
  end
end
