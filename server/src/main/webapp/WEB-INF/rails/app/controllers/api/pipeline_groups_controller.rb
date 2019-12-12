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

class Api::PipelineGroupsController < Api::ApiController
  before_action :check_api_enabled_toggle_and_404

  def list_configs
    pipeline_group_configs = pipeline_configs_service.getGroupsForUser(CaseInsensitiveString.str(current_user.getUsername()))
    pipeline_group_config_api_models = pipeline_group_configs.collect do |pipeline_group_config|
      PipelineGroupConfigAPIModel.new(pipeline_group_config)
    end
    render json: pipeline_group_config_api_models
  end

  private

  def check_api_enabled_toggle_and_404
    unless feature_toggle_service.isToggleOn(Toggles.ENABLE_PIPELINE_GROUP_CONFIG_LISTING_API)
      render_not_found_error
    end
  end
end
