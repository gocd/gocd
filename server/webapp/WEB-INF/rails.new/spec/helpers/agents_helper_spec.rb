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


describe AgentsHelper do
  include AgentsHelper
  include AgentMother
  include GoUtil

  before do
    stub_context_path self
  end

  describe :agent_selector do
    before(:each) do
      @selected = ['uuid']
    end

    it "should select agent thats selected in current request" do
      expect(agent_selector('uuid', 'selected[]', @selected)).to eq("<td class='selector'><input type='checkbox' name='selected[]' value='uuid' class='agent_select' checked='true'/></td>")
    end

    it "should not select agent thats not selected in current request" do
      expect(agent_selector('uuid1', 'selected[]', @selected)).to eq("<td class='selector'><input type='checkbox' name='selected[]' value='uuid1' class='agent_select' /></td>")
    end

    describe "when selected is not set" do
      it "should not select agent thats not selected in current request" do
        expect(agent_selector('uuid1', 'selected[]', nil)).to eq("<td class='selector'><input type='checkbox' name='selected[]' value='uuid1' class='agent_select' /></td>")
      end
    end
  end

  it "should generate table cell(td) with title same as inner html" do
    expect(cell_with_title("foo", "class")).to eq("<td class='class' title='foo'><span>foo</span></td>")
  end

  it "should generate table cell(td) with different title and inner html optionally" do
    expect(cell_with_title("foo", "class", "bar")).to eq("<td class='class' title='bar'><span>foo</span></td>")
  end

  describe :piped_cell do
    it "should create pipe seperated cell" do
      expect(self).to receive(:cell_with_title).with("foo | bar", "blah-title")
      piped_cell(["foo","bar"],"default value" ,"blah-title")
    end

    it "should use default value for empty lists" do
      expect(self).to receive(:cell_with_title).with("default value", "title")
      piped_cell([],"default value","title")
    end

  end

  describe :agent_status_cell do
    before do
      @time = java.util.Date.new
    end

    it "should be 'disabled (building)' when agent is disabled while building" do
      expect(agent_status_cell(disabled_agent(:locator => 'locator'))).to match(/>disabled \(building\)</)
    end

    it "should be 'disabled' when agent is disabled while building and locator is not available to whatever reason" do
      expect(agent_status_cell(disabled_agent(:locator => ''))).to match(/>disabled</)
      expect(agent_status_cell(disabled_agent(:locator => nil))).to match(/>disabled</)
    end

    it "should be 'disabled' when agent is disabled" do
      expect(agent_status_cell(disabled_agent)).to match(/>disabled</)
    end

    it "should be 'disabled (building)' when agent is disabled while 'cancelled (building)'" do
      agent = AgentInstanceMother.building()
      agent.cancel()
      agent.deny()
      expect(agent_status_cell(AgentViewModel.new(agent))).to match(/>disabled \(building\)</)
    end

    it "should title status cell with last heard time if agent status is lost contact" do
      should_receive(:cell_with_title).with("lost contact", "status", anything()).and_return do |arg1,arg2,arg3|
        expect(arg3).to match(/lost contact at/)
        "blah"
      end
      expect(agent_status_cell(lost_contact_agent(:locator=>''))).to eq('blah')
    end

    it "should title status cell with status when agent status is other than lost contact" do
      should_receive(:cell_with_title).with('pending', "status").and_return("cell")
      expect(agent_status_cell(pending_agent)).to eq('cell')
    end

    it "should not show link to job detail page when agent is building but user does not have permissions on the pipeline" do
      should_receive(:has_view_or_operate_permission_on_pipeline?).with('foo/bar').and_return(false)
      should_receive(:cell_with_title).with('building', "status").and_return("cell")
      expect(agent_status_cell(building_agent(:locator=>'foo/bar'))).to eq('cell')
    end

    it "should make status inner html a link to build detail when building" do
      should_receive(:has_view_or_operate_permission_on_pipeline?).with('foo/bar').and_return(true)
      should_receive(:cell_with_title).with(link_to('building', build_locator_url("foo/bar")), "status", 'foo/bar').and_return("cell")
      expect(agent_status_cell(building_agent(:locator=>'foo/bar'))).to eq('cell')
    end

    it "should make status inner html a link to build detail when cancelled with locator" do
      should_receive(:cell_with_title).with(link_to('building (cancelled)', build_locator_url("foo/bar")), "status", 'foo/bar').and_return("cell")
      expect(agent_status_cell(cancelled_agent(:locator=>'foo/bar'))).to eq('cell')
    end

    it "should make status inner html show status only when cancelled without locator" do
      should_receive(:cell_with_title).with('building (cancelled)',"status").and_return("cell")
      expect(agent_status_cell(cancelled_agent(:locator=>''))).to eq('cell')
    end
  end


  it "should prepend build_locator with tab/build/detail to make it a valid path" do
    expect(build_locator_url("foo/bar")).to eq("/go/tab/build/detail/foo/bar")
  end

  it "should call security service to check if user has view or operate permission" do
    should_receive(:current_user).and_return(:user)
    should_receive(:security_service).and_return(security_service = Object.new)
    security_service.should_receive(:hasViewOrOperatePermissionForPipeline).with(:user, "uat").and_return(true)

    expect(has_view_or_operate_permission_on_pipeline?("uat/1/dist/2/build")).to eq(true)
  end

  it "should show default label for blank string" do
    expect(label_for(nil, "default text")).to eq("default text")
    expect(label_for("", "default text")).to eq("default text")
    expect(label_for("foo", "default text")).to eq("foo")
    expect(label_for(" ", "default text")).to eq(" ")
  end

  it "should capture sort while paginating" do
    in_params :controller => "agents", :action => "job_run_history", :page => 10, :column => "pipeline", :order => "ASC", :uuid => "boouid"
    expect(job_on_agent_page_handler(com.thoughtworks.go.server.util.Pagination::PageNumber.new(10))).to eq(link_to("10", job_run_history_on_agent_path(:page => 10, :column => "pipeline", :order => "ASC", :uuid => "boouid")))
  end

  it "should give Disabled status if show_only_disabled is true and Agent Status is Disabled" do
    agent_status = get_agent_status_class(true, AgentStatus::Disabled)
    expect(agent_status).to eq(AgentStatus::Disabled)
  end

  it "should give empty status if show_only_disabled is true and Agent Status is anything other than Disabled" do
    agent_status = get_agent_status_class(true, AgentStatus::Cancelled)
    expect(agent_status).to eq("")
  end

  it "should return Agent Status as it is if show_only_disabled is false " do
    agent_status = get_agent_status_class(false, AgentStatus::Building)
    expect(agent_status).to eq(AgentStatus::Building)
  end
end
