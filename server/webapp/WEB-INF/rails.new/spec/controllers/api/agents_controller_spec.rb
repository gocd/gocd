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

require 'spec_helper'

describe Api::AgentsController do
  before do
    controller.stub(:agent_service).and_return(@agent_service = double('agent-service'))
    controller.stub(:job_instance_service).and_return(@job_instance_service = double('job instance service'))
    login_as_user
    login_as_admin
  end

  describe :job_run_history do
    include APIModelMother

    it "should resolve routes" do
      expect(:get => "/api/agents/1234/job_run_history").to route_to({:controller => 'api/agents', :action => 'job_run_history', :uuid => '1234', :offset => '0', :no_layout => true})
      expect(:get => "/api/agents/1234/job_run_history/1").to route_to({:controller => 'api/agents', :action => 'job_run_history', :uuid => '1234', :offset => '1', :no_layout => true})
    end

    it "should render job run history json" do
      @job_instance_service.should_receive(:totalCompletedJobsCountOn).with('uuid').and_return(10)
      @job_instance_service.should_receive(:completedJobsOnAgent).with('uuid', anything, anything, anything).and_return(create_agent_job_run_history_model)

      get :job_run_history, :uuid => 'uuid', :offset => '5', :no_layout => true

      expect(response.body).to eq(AgentJobRunHistoryAPIModel.new(Pagination.pageStartingAt(5, 10, 10), create_agent_job_run_history_model).to_json)
    end
  end
end
