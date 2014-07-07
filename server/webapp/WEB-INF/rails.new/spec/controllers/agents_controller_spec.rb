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

describe AgentsController do

  before do
    controller.stub!(:set_locale)
    controller.stub(:licensed_agent_limit)
    controller.stub(:populate_config_validity)
  end

  describe "GET 'index'" do

    before(:all) do
      config = ConfigMigrator.migrate(ConfigFileFixture::WITH_VARITY_OF_AGENTS)
      cachedGoConfig = Spring.bean("cachedGoConfig")
      cachedGoConfig.save(config, false)
    end

    it "should filter agents based on parameters" do
      get :index, {:filter => 'resource:filtering_resource'}
      assigns[:agents].size.should == 1
    end


    it "should set current tab" do
     get :index
     assigns[:current_tab_name].should == "agents"
    end


    it "should show all agents" do
      get 'index'

      response.should be_success
      assigns[:agents].size.should == 6
      controller.class.read_inheritable_attribute(:layout).should == "application"
    end

    it "should sortBy DESC on fields" do
      get "index", :column => 'hostname', :order => 'DESC'
      assigns[:agents].inject(nil) do |previous, agent|
        previous.getHostname().should > agent.getHostname() if previous
        agent
      end
    end

    it "should sortBy ASC on fields" do
      get "index", :column => "ip_address", :order => "ASC"
      assigns[:agents].inject(nil) do |previous, agent|
        previous.getIpAddress().should < agent.getIpAddress() if previous
        agent
      end
    end

    it "should sort on status if no sort requested" do
      controller.stub!(:agent_service).and_return(agent_service = Object.new)
      agent_service.stub(:agents).and_return(agents = [])
      agents.should_receive(:sortBy).with(AgentsController::AgentViewModel.STATUS_COMPARATOR, com.thoughtworks.go.server.ui.SortOrder::ASC).and_return([])
      agents.should_receive(:filter).with(nil).and_return([])
      agents.stub(:disabledCount).and_return(0)
      agents.stub(:enabledCount).and_return(10)
      agents.stub(:pendingCount).and_return(0)
      get "index"
      assigns[:agents].should == []
    end

    describe :apply_default_sort_unless_sorted do
      before do
        controller.stub(:params).and_return(@params = { })
      end
      it "should apply default sort if not sorted" do
        controller.send(:apply_default_sort_unless_sorted)
        @params[:column].should == "status"
        @params[:order].should == "ASC"
      end

      it "should not override applied sort" do
        @params[:column] = "foo"
        @params[:order] = "DESC"
        controller.send(:apply_default_sort_unless_sorted)
        @params[:column].should == "foo"
        @params[:order].should == "DESC"
      end

      it "should be applied on GET index" do
        controller.should_receive(:apply_default_sort_unless_sorted)
        get :index
      end
    end

    it "should use agent_instances.agent_count to find agent count by status(for building, idle, lost_contact and missing agents)" do
      controller.stub!(:agent_service).and_return(agent_service = Object.new)
      agent_service.stub(:agents).and_return(agents = [])
      agents.stub(:disabledCount).and_return(9)
      agents.stub(:enabledCount).and_return(10)
      agents.stub(:pendingCount).and_return(8)
      agents.stub(:sortBy).and_return([])
      agents.should_receive(:filter).with(nil).and_return([])
      get :index
      assigns[:agents_disabled].should == 9
      assigns[:agents_pending].should == 8
      assigns[:agents_enabled].should == 10
    end

  end

  describe :edit_agents do

    it "should resolve as /api/agents/bulk_edit" do
      params_from(:post, "/api/agents/edit_agents").should == {:controller => 'api/agents', :action => 'edit_agents', :no_layout => true}
    end

    it "should answer for /agents.json" do
      route_for(:controller => "agents", :action => "index", "format"=>"json").should == "/agents.json"
    end

    it "should redirect to :index maintaining sort" do
      setup_base_urls
      bulk_edit_result = mock('bulk_edit_result')
      controller.should_receive(:bulk_edit).and_return(bulk_edit_result)
      bulk_edit_result.stub(:message).and_return("successfuly managed to edit")
      bulk_edit_result.stub(:canContinue).and_return(false)
      get :edit_agents, :column => "foo", :order => "bar"
      matcher = redirect_to("agents page with sort on right column") #this is done because rspec is too retarted to understand paths(it only gets urls)
      matcher.stub(:expected_url).and_return("/agents?column=foo&order=bar")
      response.should matcher
    end
  end

  describe :resource_selector do
    before(:each) do
      controller.stub!(:agent_service).and_return(@agent_service = Object.new)
      @resources = [ TriStateSelection.new('resource-1', 'remove') ]
      @agent_service.stub(:getResourceSelections)
    end

    it "should load all resources" do
      @agent_service.should_receive(:getResourceSelections).with(["UUID1", "UUID2"]).and_return(@resources)
      post :resource_selector, :selected => ["UUID1", "UUID2"]
      assigns[:selections].should == @resources
    end

    it "should default selected UUIDS to empty list" do
      @agent_service.should_receive(:getResourceSelections).with([]).and_return(@resources)
      post :resource_selector
      assigns[:selections].should == @resources
    end

    it "should render partial resource_selector" do
      controller.should_receive(:render).with(:partial => 'shared/selectors', :locals => {:scope => {}})
      post :resource_selector, :selected => ["UUID1", "UUID2"]
    end

    it "should resolve POST to /agents/resource_selector as a call" do
      params_from(:post, "/agents/resource_selector").should == {:controller => 'agents', :action => 'resource_selector'}
    end
  end



  describe :environments_selector do
    before(:each) do
      controller.stub!(:agent_service).and_return(@agent_service = Object.new)
      @environments = [ TriStateSelection.new('resource-1', 'remove') ]
      @agent_service.stub(:getEnvironmentSelections)
    end

    it "should load all environments" do
      @agent_service.should_receive(:getEnvironmentSelections).with(["UUID1", "UUID2"]).and_return(@environments)
      post :environment_selector, :selected => ["UUID1", "UUID2"]
      assigns[:selections].should == @environments
    end

    it "should default selected UUIDS to empty list" do
      @agent_service.should_receive(:getEnvironmentSelections).with([]).and_return(@resources)
      post :environment_selector
      assigns[:selections].should == @resources
    end

    it "should render partial selectors" do
      controller.should_receive(:render).with(:partial => 'shared/selectors', :locals => {:scope => {}})
      post :environment_selector, :selected => ["UUID1", "UUID2"]
    end

    it "should resolve POST to /agents/resource_selector as a call" do
      params_from(:post, "/agents/environment_selector").should == {:controller => 'agents', :action => 'environment_selector'}
    end
  end

end
