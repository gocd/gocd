##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
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
##########################GO-LICENSE-END##################################

class Api::ServerController < Api::ApiController
  def info
    @base_url = system_environment.getBaseUrlForShine()
    @base_ssl_url = system_environment.getBaseSslUrlForShine()
    @artifacts_dir = go_config_service.artifactsDir().getAbsolutePath()
    @shine_db_path = system_environment.shineDb().getAbsolutePath()
    @config_dir = system_environment.configDir().getAbsolutePath()
  end

  def capture_support_info
    information = server_status_service.captureServerInfo(current_user, result = HttpLocalizedOperationResult.new)

    if result.isSuccessful()
      render text: information, layout: false
    else
      render_localized_operation_result(result)
    end

  end
end
