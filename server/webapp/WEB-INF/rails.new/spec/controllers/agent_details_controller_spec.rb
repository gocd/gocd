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


describe AgentDetailsController do

  before do
    controller.stub(:set_locale)
    controller.stub(:populate_config_validity)
  end

  describe :routes do
    it "should resolve the route to an agent" do
      expect(:get => "/agents/uuid").to route_to({:controller => "agent_details", :action => 'show',:uuid => "uuid"})
      expect(controller.send(:agent_detail_path,:uuid=>"uuid")).to eq("/agents/uuid")
    end

    it "should resolve the route to an job run history for an agent" do
      expect(:get => "/agents/uuid/job_run_history").to route_to({:controller => "agent_details", :action => 'job_run_history',:uuid => "uuid"})
      expect(controller.send(:job_run_history_on_agent_path, :uuid=>"uuid")).to eq("/agents/uuid/job_run_history")
    end
  end


  describe :agent_details do
    include AgentMother

    before :each do
      @uuid="fff2222233333"
      @agent = building_agent()
      controller.should_receive(:agent_service).and_return(@agent_service = Object.new)
    end

    it "should show agent details" do
      @agent_service.should_receive(:findAgentViewModel).with(@uuid).and_return(@agent)

      get "show", :uuid =>@uuid

      assert_template layout: :agent_detail
      assert_template "show"
      expect(assigns(:agent)).to eq(@agent)
    end

    describe "when bad uuid is given" do
      render_views

      it "should show 404 when an agent is not found" do
        @agent_service.should_receive(:findAgentViewModel).with(@uuid).and_return(AgentViewModel.new(com.thoughtworks.go.domain.NullAgentInstance.new(@uuid)))

        get "show", :uuid => @uuid
        expect(response.status).to eq(404)
        expect(response.body).to have_content(/Agent with uuid '#{@uuid}' not found\./)
      end
    end

    it "should show job run history" do
      @agent_service.should_receive(:findAgentViewModel).with(@uuid).and_return(@agent)
      controller.should_receive(:job_instance_service).and_return(@job_instance_service = Object.new)
      @job_instance_service.should_receive(:completedJobsOnAgent).with(@uuid, AgentDetailsController::JobHistoryColumns.completed, SortOrder::DESC, 1, AgentDetailsController::PAGE_SIZE).and_return(expected = JobInstancesModel.new(nil, nil))

      get "job_run_history", :uuid => @uuid

      assert_template "job_run_history"
      assert_template layout: :agent_detail
      expect(assigns(:agent)).to eq(@agent)
      expect(assigns(:job_instances)).to eq(expected)
    end

    it "should show a later page of job run history" do
      @agent_service.should_receive(:findAgentViewModel).with(@uuid).and_return(@agent)
      controller.should_receive(:job_instance_service).and_return(@job_instance_service = Object.new)
      @job_instance_service.should_receive(:completedJobsOnAgent).with(@uuid, AgentDetailsController::JobHistoryColumns.stage, SortOrder::ASC, 3, AgentDetailsController::PAGE_SIZE).and_return(expected = JobInstancesModel.new(nil, nil))

      get "job_run_history", :uuid => @uuid, :page => 3, :column => 'stage', :order => 'ASC'

      assert_template "job_run_history"
      assert_template layout: :agent_detail
      expect(assigns(:agent)).to eq(@agent)
      expect(assigns(:job_instances)).to eq(expected)
    end
  end
end
