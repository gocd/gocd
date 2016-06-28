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
  class VersionInfosController < ApiV1::BaseController
    before_action :check_user_and_404, :only => [:update_server, :stale]

    def update_server
      go_latest_version = ApiV1::GoLatestVersion.new(params, system_environment)
      unless go_latest_version.valid?
        Rails.logger.error('[Go Update Check] Latest version update failed, version information from update server tampered.')
        return render_message(message_tampered_error_message, 400, {})
      end

      result = HttpLocalizedOperationResult.new
      version_info = version_info_service.updateServerLatestVersion(go_latest_version.latest_version, result)

      if result.isSuccessful
        render DEFAULT_FORMAT => to_json_hal(version_info)
      else
        render_http_operation_result(result)
      end
    end

    def stale
      version_info = version_info_service.getStaleVersionInfo()

      render DEFAULT_FORMAT => to_json_hal(version_info)
    end

    private

    def to_json_hal(version_info)
      return {} if version_info.nil?

      VersionInfoRepresenter.new(version_info, system_environment).to_hash(url_builder: self)
    end

    def message_tampered_error_message
      'Message tampered, cannot process.'
    end
  end
end
