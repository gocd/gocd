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

class AgentDetailsController < ApplicationController
  JobHistoryColumns = com.thoughtworks.go.server.service.JobInstanceService::JobHistoryColumns

  PAGE_SIZE = 50

  before_action :populate_agent_for_details

  layout "application", :except => [:show, :job_run_history]
    prepend_before_action :set_tab_name

  def show
    render :layout => "agent_detail"
  end

  def job_run_history
    params[:page] ||= 1
    @job_instances = job_instance_service.completedJobsOnAgent(params[:uuid], JobHistoryColumns.valueOf(params[:column] || "completed"), specified_order("DESC"), params[:page].to_i, PAGE_SIZE)
    render :layout => "agent_detail"
  end

  private

  def populate_agent_for_details
    uuid = params[:uuid]
    @agent = agent_service.findAgentViewModel(uuid)
    if @agent.isNullAgent()
      render_error_response(l.string("AGENT_WITH_UUID_NOT_FOUND", [uuid].to_java(java.lang.String)), 404, false)
      false
    end
  end

  def specified_order default="ASC"
    com.thoughtworks.go.server.ui.SortOrder.orderFor(params[:order] || default)
  end

   def set_tab_name
      @current_tab_name = "agents"
    end
end
