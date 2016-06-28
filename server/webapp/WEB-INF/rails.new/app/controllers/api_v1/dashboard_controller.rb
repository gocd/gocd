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
  class DashboardController < ApiV1::BaseController

    include ApplicationHelper

    def dashboard
      # TODO: What happens when there is no cookie!
      pipeline_selections = go_config_service.getSelectedPipelines(cookies[:selected_pipelines], current_user_entity_id)
      pipeline_groups     = pipeline_history_service.allActivePipelineInstances(current_user, pipeline_selections)
      presenters          = Dashboard::PipelineGroupsRepresenter.new(pipeline_groups)

      render DEFAULT_FORMAT => presenters.to_hash(url_builder: self)
    end

  end
end
