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

class Api::ConfigurationController < Api::ApiController
  def config_revisions
    offset = params[:offset].to_i
    page_size = 10

    if security_service.isUserAdmin(current_user)
      config_revisions = config_repository.getCommits(page_size, offset)

      config_revision_api_models = []
      config_revisions.each do |config_revision|
        config_revision_api_models << ConfigRevisionAPIModel.new(config_revision)
      end
      render json: config_revision_api_models
    else
      render_error_response('Unauthorized to access this API.', 403, true)
    end
  end

  def config_diff
    from_revision = params[:from_revision]
    to_revision = params[:to_revision]

    if security_service.isUserAdmin(current_user)
      config_diff = config_repository.configChangesForCommits(from_revision, to_revision)
      render plain: config_diff
    else
      render_error_response('Unauthorized to access this API.', 403, true)
    end
  end
end
