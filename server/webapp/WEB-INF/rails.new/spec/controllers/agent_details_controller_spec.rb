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

require 'rails_helper'

describe AgentDetailsController do

  before do
    allow(controller).to receive(:populate_config_validity)
    allow(controller).to receive(:agent_service).and_return(@agent_service = instance_double('com.thoughtworks.go.server.service.AgentService'))
    allow(controller).to receive(:job_instance_service).and_return(@job_instance_service = instance_double('com.thoughtworks.go.server.service.JobInstanceService'))
  end

  describe "agent_details" do
    include AgentMother

    before :each do
      @uuid="fff2222233333"
      @agent = building_agent()
    end

    it "should show agent details" do
      expect(@agent_service).to receive(:findAgentViewModel).with(@uuid).and_return(@agent)

      get :show, params: { :uuid => @uuid }

      assert_template layout: :agent_detail
      assert_template "show"
      expect(assigns(:agent)).to eq(@agent)
    end

    describe "when bad uuid is given" do
      render_views

      it "should show 404 when an agent is not found" do
        expect(@agent_service).to receive(:findAgentViewModel).with(@uuid).and_return(AgentViewModel.new(com.thoughtworks.go.domain.NullAgentInstance.new(@uuid)))

        get :show, params: { :uuid => @uuid }
        expect(response.status).to eq(404)
        expect(response.body).to have_content(/Agent with uuid '#{@uuid}' not found\./)
      end
    end

    it "should show job run history" do
      expect(@agent_service).to receive(:findAgentViewModel).with(@uuid).and_return(@agent)
      expect(@job_instance_service).to receive(:completedJobsOnAgent).with(@uuid, AgentDetailsController::JobHistoryColumns.completed, SortOrder::DESC, 1, AgentDetailsController::PAGE_SIZE).and_return(expected = JobInstancesModel.new(nil, nil))

      get :job_run_history, params: { :uuid => @uuid }

      assert_template "job_run_history"
      assert_template layout: :agent_detail
      expect(assigns(:agent)).to eq(@agent)
      expect(assigns(:job_instances)).to eq(expected)
    end

    it "should show a later page of job run history" do
      expect(@agent_service).to receive(:findAgentViewModel).with(@uuid).and_return(@agent)
      expect(@job_instance_service).to receive(:completedJobsOnAgent).with(@uuid, AgentDetailsController::JobHistoryColumns.stage, SortOrder::ASC, 3, AgentDetailsController::PAGE_SIZE).and_return(expected = JobInstancesModel.new(nil, nil))

      get :job_run_history, params: { :uuid => @uuid, :page => 3, :column => 'stage', :order => 'ASC' }

      assert_template "job_run_history"
      assert_template layout: :agent_detail
      expect(assigns(:agent)).to eq(@agent)
      expect(assigns(:job_instances)).to eq(expected)
    end
  end
end
