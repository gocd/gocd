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

describe AgentsController do

  before do
    controller.stub(:set_locale)
    controller.stub(:populate_config_validity)
  end

  describe "GET 'index'" do

    before(:all) do
      config = ConfigMigrator.migrate(ConfigFileFixture::WITH_VARITY_OF_AGENTS)
      cachedGoConfig = Spring.bean("cachedFileGoConfig")
      cachedGoConfig.save(config, false)
    end

    it "should answer for /agents" do
      expect(:get => "/agents").to route_to({:controller => "agents", :action => 'index',:format => "html"})
      expect(:get => "/agents.html").to route_to({:controller => "agents", :action => 'index', :format => "html"})
      expect(:get => "/agents.json").to route_to({:controller => "agents", :action => 'index', :format => "json"})
      expect(controller.send(:agents_path)).to eq("/agents")
    end

    it "should filter agents based on parameters" do
      get :index, {:filter => 'resource:filtering_resource'}
      expect(assigns(:agents).size).to eq(1)
    end

    it "should set current tab" do
     get :index
     expect(assigns(:current_tab_name)).to eq("agents")
    end

    it "should show all agents" do
      get 'index'

      response.should be_success
      expect(assigns(:agents).size).to eq(6)
      assert_template layout: :application
    end

    it "should sortBy DESC on fields" do
      get "index", :column => 'hostname', :order => 'DESC'
      assigns(:agents).inject(nil) do |previous, agent|
        expect(previous.getHostname()).to be > agent.getHostname() if previous
        agent
      end
    end

    it "should sortBy ASC on fields" do
      get "index", :column => "ip_address", :order => "ASC"
      assigns(:agents).inject(nil) do |previous, agent|
        expect(previous.getIpAddress()).to be < agent.getIpAddress() if previous
        agent
      end
    end

    it "should sort on status if no sort requested" do
      controller.stub(:agent_service).and_return(agent_service = Object.new)
      agent_service.stub(:agents).and_return(agents = [])
      agents.should_receive(:sortBy).with(AgentsController::AgentViewModel.STATUS_COMPARATOR, com.thoughtworks.go.server.ui.SortOrder::ASC).and_return([])
      agents.should_receive(:filter).with(nil).and_return([])
      agents.stub(:disabledCount).and_return(0)
      agents.stub(:enabledCount).and_return(10)
      agents.stub(:pendingCount).and_return(0)
      get "index"
      expect(assigns(:agents)).to eq []
    end

    describe :apply_default_sort_unless_sorted do
      before do
        controller.stub(:params).and_return(@params = { })
      end
      it "should apply default sort if not sorted" do
        controller.send(:apply_default_sort_unless_sorted)
        expect(@params[:column]).to eq "status"
        expect(@params[:order]).to eq("ASC")
      end

      it "should not override applied sort" do
        @params[:column] = "foo"
        @params[:order] = "DESC"
        controller.send(:apply_default_sort_unless_sorted)
        expect(@params[:column]).to eq("foo")
        expect(@params[:order]).to eq("DESC")
      end

      it "should be applied on GET index" do
        controller.should_receive(:apply_default_sort_unless_sorted)
        get :index
      end
    end

    it "should use agent_instances.agent_count to find agent count by status(for building, idle, lost_contact and missing agents)" do
      controller.stub(:agent_service).and_return(agent_service = Object.new)
      agent_service.stub(:agents).and_return(agents = [])
      agents.stub(:disabledCount).and_return(9)
      agents.stub(:enabledCount).and_return(10)
      agents.stub(:pendingCount).and_return(8)
      agents.stub(:sortBy).and_return([])
      agents.should_receive(:filter).with(nil).and_return([])
      get :index
      expect(assigns(:agents_disabled)).to eq 9
      expect(assigns(:agents_pending)).to eq 8
      expect(assigns(:agents_enabled)).to eq 10
    end

  end

  describe :edit_agents do

    it "should resolve routes" do
      expect(:post => "agents/edit_agents").to route_to({:controller => "agents", :action => 'edit_agents'})
      expect(controller.send(:edit_agents_path)).to eq("/agents/edit_agents")
    end

    it "should redirect to :index maintaining sort" do
      setup_base_urls
      bulk_edit_result = double('bulk_edit_result')
      controller.should_receive(:bulk_edit).and_return(bulk_edit_result)
      bulk_edit_result.stub(:message).and_return("successfuly managed to edit")
      bulk_edit_result.stub(:canContinue).and_return(false)
      get :edit_agents, :column => "foo", :order => "bar", :filter => "criteria"
      expect(response).to redirect_to("/agents?column=foo&filter=criteria&order=bar")
    end
  end

  describe :resource_selector do
    before(:each) do
      controller.stub(:agent_service).and_return(@agent_service = Object.new)
      @resources = [ TriStateSelection.new('resource-1', 'remove') ]
      @agent_service.stub(:getResourceSelections)
    end

    it "should resolve routes" do
      expect(controller.send(:agent_grouping_data_path, {:action => "resource_selector"})).to eq("/agents/resource_selector")
      expect(:post => "/agents/resource_selector").to route_to({:controller => "agents", :action => 'resource_selector'})
    end

    it "should load all resources" do
      @agent_service.should_receive(:getResourceSelections).with(["UUID1", "UUID2"]).and_return(@resources)
      post :resource_selector, :selected => ["UUID1", "UUID2"]
      expect(assigns(:selections)).to eq(@resources)
    end

    it "should default selected UUIDS to empty list" do
      @agent_service.should_receive(:getResourceSelections).with([]).and_return(@resources)
      post :resource_selector
      expect(assigns(:selections)).to eq(@resources)
    end

    it "should render partial resource_selector" do
      post :resource_selector, :selected => ["UUID1", "UUID2"]
      expect(response).to render_template("resource_selector")
      assert_template :layout => false
    end
  end

  describe :environments_selector do
    before(:each) do
      controller.stub(:agent_service).and_return(@agent_service = Object.new)
      @environments = [ TriStateSelection.new('resource-1', 'remove') ]
      @agent_service.stub(:getEnvironmentSelections)
    end

    it "should resolve routes" do
      expect(controller.send(:agent_grouping_data_path, {:action => "environment_selector"})).to eq("/agents/environment_selector")
      expect(:post => "/agents/environment_selector").to route_to({:controller => "agents", :action => 'environment_selector'})
    end

    it "should load all environments" do
      @agent_service.should_receive(:getEnvironmentSelections).with(["UUID1", "UUID2"]).and_return(@environments)
      post :environment_selector, :selected => ["UUID1", "UUID2"]
      expect(assigns(:selections)).to eq @environments
    end

    it "should default selected UUIDS to empty list" do
      @agent_service.should_receive(:getEnvironmentSelections).with([]).and_return(@resources)
      post :environment_selector
      expect(assigns(:selections)).to eq @resources
    end

    it "should render partial selectors" do
      post :environment_selector, :selected => ["UUID1", "UUID2"]
      expect(response).to render_template("environment_selector")
      assert_template :layout => false
    end
  end
end
