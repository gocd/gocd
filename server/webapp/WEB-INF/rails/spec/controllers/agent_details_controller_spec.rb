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

require File.expand_path(File.dirname(__FILE__) + '/../spec_helper')

describe AgentDetailsController do

  before do
    controller.stub!(:set_locale)
    controller.stub(:licensed_agent_limit)
    controller.stub(:populate_config_validity)
  end

  describe :routes do
    it "should resolve the route to an agent" do
      params_from(:get, "/agents/uuid").should == {:controller => "agent_details", :action => 'show', :uuid => "uuid"}
      agent_detail_path(:uuid => "uuid").should == "/agents/uuid"
    end

    it "should resolve the route to an job run history for an agent" do
      params_from(:get, "/agents/uuid/job_run_history").should == {:controller => "agent_details", :action => 'job_run_history', :uuid => "uuid"}
      job_run_history_on_agent_path(:uuid => "uuid").should == "/agents/uuid/job_run_history"
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

      controller.should_receive(:render).with(:layout => "agent_detail")

      get "show", :uuid =>@uuid

      assigns[:agent].should == @agent
    end

    describe "when bad uuid is given" do
      integrate_views

      it "should show 404 when an agent is not found" do
        @agent_service.should_receive(:findAgentViewModel).with(@uuid).and_return(AgentViewModel.new(com.thoughtworks.go.domain.NullAgentInstance.new(@uuid)))

        get "show", :uuid => @uuid

        response.status.should == "404 Not Found"
        response.body.should =~ /Agent with uuid '#{@uuid}' not found\./
      end
    end

    it "should show job run history" do
      @agent_service.should_receive(:findAgentViewModel).with(@uuid).and_return(@agent)
      controller.should_receive(:render).with(:layout => "agent_detail")
      controller.should_receive(:job_instance_service).and_return(@job_instance_service = Object.new)
      @job_instance_service.should_receive(:completedJobsOnAgent).with(@uuid, AgentDetailsController::JobHistoryColumns.completed, SortOrder::DESC, 1, AgentDetailsController::PAGE_SIZE).and_return(expected = JobInstancesModel.new(nil, nil))

      get "job_run_history", :uuid => @uuid

      assigns[:agent].should == @agent
      assigns[:job_instances].should == expected
    end

    it "should show a later page of job run history" do
      @agent_service.should_receive(:findAgentViewModel).with(@uuid).and_return(@agent)
      controller.should_receive(:render).with(:layout => "agent_detail")
      controller.should_receive(:job_instance_service).and_return(@job_instance_service = Object.new)
      @job_instance_service.should_receive(:completedJobsOnAgent).with(@uuid, AgentDetailsController::JobHistoryColumns.stage, SortOrder::ASC, 3, AgentDetailsController::PAGE_SIZE).and_return(expected = JobInstancesModel.new(nil, nil))

      get "job_run_history", :uuid => @uuid, :page => 3, :column => 'stage', :order => 'ASC'

      assigns[:agent].should == @agent
      assigns[:job_instances].should == expected
    end
  end
end
