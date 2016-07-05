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

class Api::AgentsController < Api::ApiController
  include AgentBulkEditor
  include ApiV1::AuthenticationHelper

  before_action :check_user_and_404
  before_action :check_admin_user_and_401, except: [:index, :show, :job_run_history]

  JobHistoryColumns = com.thoughtworks.go.server.service.JobInstanceService::JobHistoryColumns

  def job_run_history
    offset = params[:offset].to_i
    page_size = 10
    job_instance_count = job_instance_service.totalCompletedJobsCountOn(params[:uuid])

    pagination = Pagination.pageStartingAt(offset, job_instance_count, page_size)

    job_instances = job_instance_service.completedJobsOnAgent(params[:uuid], JobHistoryColumns.valueOf("completed"), com.thoughtworks.go.server.ui.SortOrder.orderFor("DESC"), pagination)

    render json: AgentJobRunHistoryAPIModel.new(pagination, job_instances)
  end
end
