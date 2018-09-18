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

java_import 'org.springframework.dao.DataRetrievalFailureException'

class Api::JobsController < Api::ApiController
  include ApplicationHelper

  def render_not_found()
    render :plain => "Not Found!", :status => 404
  end

  def index
    return render_not_found unless number?(params[:id])
    job_id = Integer(params[:id])
    begin
      @doc = xml_api_service.write(JobXmlViewModel.new(job_instance_service.buildById(job_id)), "#{request.protocol}#{request.host_with_port}/go")
    rescue Exception => e
      logger.error(e)
      return render_not_found
    end
  end

  def scheduled
    scheduled_waiting_jobs = job_instance_service.waitingJobPlans()
    @doc = xml_api_service.write(JobPlanXmlViewModel.new(scheduled_waiting_jobs), "#{request.protocol}#{request.host_with_port}/go")
  end

  def history
    pipeline_name = params[:pipeline_name]
    stage_name = params[:stage_name]
    job_name = params[:job_name]
    offset = params[:offset].to_i
    page_size = 10
    job_instance_count = job_instance_service.getJobHistoryCount(pipeline_name, stage_name, job_name)
    result = HttpOperationResult.new

    pagination = Pagination.pageStartingAt(offset, job_instance_count, page_size)
    job_history = job_instance_service.findJobHistoryPage(pipeline_name, stage_name, job_name, pagination, CaseInsensitiveString.str(current_user.getUsername()), result)

    if result.canContinue()
      job_history_api_model = JobHistoryAPIModel.new(pagination, job_history)
      render json: job_history_api_model
    else
      render_error_response(result.detailedMessage(), result.httpCode(), true)
    end
  end
end
