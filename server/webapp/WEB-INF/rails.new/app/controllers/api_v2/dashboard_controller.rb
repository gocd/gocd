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
module ApiV2
  class DashboardController < ApiV2::BaseController

    include ApplicationHelper

    def dashboard
      name_of_current_user = CaseInsensitiveString.str(current_user.getUsername())

      pipelines_across_groups = go_dashboard_service.allPipelinesForDashboard()
      pipelines_viewable_by_user = pipelines_across_groups.select do |pipeline|
        pipeline.canBeViewedBy(name_of_current_user)
      end

      presenters              = Dashboard::PipelineGroupsRepresenter.new(pipelines_viewable_by_user)
      render DEFAULT_FORMAT => presenters.to_hash(url_builder: self)
    end

  end
end
