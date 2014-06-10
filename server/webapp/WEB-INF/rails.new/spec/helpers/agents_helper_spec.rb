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


describe AgentsHelper do
  include AgentsHelper
  include SortableTableHelper
  include AgentMother
  include GoUtil

  before do
    stub_context_path self
  end
  
  describe :table_sort_params do
    it "should generate sort link params to sort ASC the first time" do
      table_sort_params('hostname').should == { :column => 'hostname', :order => 'ASC', :filter => nil}
      table_sort_params('ip_address').should == { :column => 'ip_address', :order => 'ASC', :filter => nil}
    end

    it "should generate sort link params to switch to DESC when accending" do
      params[:column] = 'hostname'
      params[:order] = 'ASC'
      table_sort_params('hostname').should == { :column => 'hostname', :order => 'DESC', :filter => nil}
    end

    it "should generate sort link params to switch to ASC when decending" do
      params[:column] = 'hostname'
      params[:order] = 'DESC'
      table_sort_params('hostname').should == { :column => 'hostname', :order => 'ASC', :filter => nil}
    end

    it "should sorround with span" do
      surround_with_span("Resources").should == "<span>Resources</span>"
    end
  end

  describe :agent_selector do
    before(:each) do
      @selected = ['uuid']
    end

    it "should select agent thats selected in current request" do
      agent_selector('uuid', 'selected[]', @selected).should == "<td class='selector'><input type='checkbox' name='selected[]' value='uuid' class='agent_select' checked='true'/></td>"
    end

    it "should not select agent thats not selected in current request" do
      agent_selector('uuid1', 'selected[]', @selected).should == "<td class='selector'><input type='checkbox' name='selected[]' value='uuid1' class='agent_select' /></td>"
    end

    describe "when selected is not set" do
      it "should not select agent thats not selected in current request" do
        agent_selector('uuid1', 'selected[]', nil).should == "<td class='selector'><input type='checkbox' name='selected[]' value='uuid1' class='agent_select' /></td>"
      end
    end
  end

  describe :sortable_column_status do
    it "should return no options for unsorted column" do
      sortable_column_status('hostname').should == { }
    end

    it "should add css class 'sorted_asc' for column sorted asc" do
      params[:column] = 'hostname'
      params[:order] = 'ASC'
      sortable_column_status('hostname').should == { :class => 'sorted_asc' }
    end

    it "should add css class 'sorted_desc' for column sorted desc" do
      params[:column] = 'hostname'
      params[:order] = 'DESC'
      sortable_column_status('hostname').should == { :class => 'sorted_desc' }
    end

    it "should return no options for unsorted column even with another one being sorted" do
      params[:column] = 'hostname'
      params[:order] = 'DESC'
      sortable_column_status('location').should == { }
    end
  end

  it "should generate table cell(td) with title same as inner html" do
    cell_with_title("foo", "class").should == "<td class='class' title='foo'><span>foo</span></td>"
  end

  it "should generate table cell(td) with different title and inner html optionally" do
    cell_with_title("foo", "class", "bar").should == "<td class='class' title='bar'><span>foo</span></td>"
  end

  describe :piped_cell do
    it "should create pipe seperated cell" do
      should_receive(:cell_with_title).with("foo | bar", "blah-title")
      piped_cell(["foo","bar"],"default value" ,"blah-title")
    end

    it "should use default value for empty lists" do
      should_receive(:cell_with_title).with("default value", "title")
      piped_cell([],"default value","title")
    end

  end

  describe :agent_status_cell do
    before do
      @time = java.util.Date.new
    end

    it "should be 'disabled (building)' when agent is disabled while building" do

      agent_status_cell(disabled_agent(:locator => 'locator')).should =~ />disabled \(building\)</
    end

    it "should be 'disabled' when agent is disabled while building and locator is not available to whatever reason" do
      agent_status_cell(disabled_agent(:locator => '')).should =~ />disabled</
      agent_status_cell(disabled_agent(:locator => nil)).should =~ />disabled</
    end

    it "should be 'disabled' when agent is disabled" do
      agent_status_cell(disabled_agent).should =~ />disabled</
    end

    it "should be 'disabled (building)' when agent is disabled while 'cancelled (building)'" do
      agent = AgentInstanceMother.building()
      agent.cancel()
      agent.deny()
      agent_status_cell(AgentViewModel.new(agent)).should =~ />disabled \(building\)</
    end

    it "should title status cell with last heard time if agent status is lost contact" do
      should_receive(:cell_with_title).with("lost contact", "status", anything()).and_return do |arg1,arg2,arg3|
        arg3.should =~ /lost contact at/
        "blah"
      end
      agent_status_cell(lost_contact_agent(:locator=>'')).should == 'blah'
    end

    it "should title status cell with status when agent status is other than lost contact" do
      should_receive(:cell_with_title).with('pending', "status").and_return("cell")
      agent_status_cell(pending_agent).should == 'cell'
    end

    it "should not show link to job detail page when agent is building but user does not have permissions on the pipeline" do
      should_receive(:has_view_or_operate_permission_on_pipeline?).with('foo/bar').and_return(false)
      should_receive(:cell_with_title).with('building', "status").and_return("cell")
      agent_status_cell(building_agent(:locator=>'foo/bar')).should == 'cell'
    end

    it "should make status inner html a link to build detail when building" do
      should_receive(:has_view_or_operate_permission_on_pipeline?).with('foo/bar').and_return(true)
      should_receive(:cell_with_title).with(link_to('building', build_locator_url("foo/bar")), "status", 'foo/bar').and_return("cell")
      agent_status_cell(building_agent(:locator=>'foo/bar')).should == 'cell'
    end

    it "should make status inner html a link to build detail when cancelled with locator" do
      should_receive(:cell_with_title).with(link_to('building (cancelled)', build_locator_url("foo/bar")), "status", 'foo/bar').and_return("cell")
      agent_status_cell(cancelled_agent(:locator=>'foo/bar')).should == 'cell'
    end

    it "should make status inner html show status only when cancelled without locator" do
      should_receive(:cell_with_title).with('building (cancelled)',"status").and_return("cell")
      agent_status_cell(cancelled_agent(:locator=>'')).should == 'cell'
    end

  end

  it "should prepend build_locator with tab/build/detail to make it a valid path" do
    build_locator_url("foo/bar").should == "/go/tab/build/detail/foo/bar"
  end

  it "should call security service to check if user has view or operate permission" do
    should_receive(:current_user).and_return(:user)
    should_receive(:security_service).and_return(security_service = Object.new)
    security_service.should_receive(:hasViewOrOperatePermissionForPipeline).with(:user, "uat").and_return(true)
    has_view_or_operate_permission_on_pipeline?("uat/1/dist/2/build").should == true
  end

  it "should show default label for blank string" do
    label_for(nil, "default text").should == "default text"
    label_for("", "default text").should == "default text"
    label_for("foo", "default text").should == "foo"
    label_for(" ", "default text").should == " "
  end

  it "should capture sort while paginating" do
    in_params :controller => "agents", :action => "job_run_history", :page => 10, :column => "pipeline", :order => "ASC", :uuid => "boouid"
    job_on_agent_page_handler(com.thoughtworks.go.server.util.Pagination::PageNumber.new(10)).should == link_to("10", job_run_history_on_agent_path(:page => 10, :column => "pipeline", :order => "ASC", :uuid => "boouid"))
  end

  it "should give Disabled status if show_only_disabled is true and Agent Status is Disabled" do
    agent_status = get_agent_status_class(true, AgentStatus::Disabled)
    agent_status.should == AgentStatus::Disabled
  end

  it "should give empty status if show_only_disabled is true and Agent Status is anything other than Disabled" do
    agent_status = get_agent_status_class(true, AgentStatus::Cancelled)
    agent_status.should == ""
  end

  it "should return Agent Status as it is if show_only_disabled is false " do
    agent_status = get_agent_status_class(false, AgentStatus::Building)
    agent_status.should == AgentStatus::Building
  end
end
