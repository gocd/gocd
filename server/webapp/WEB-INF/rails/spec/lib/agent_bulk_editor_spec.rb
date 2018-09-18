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

describe "agent_bulk_editor" do
  include AgentBulkEditor

  before :each do
    allow(self).to receive(:agent_service).and_return(@agent_service = double('agent-service'))
    allow(self).to receive(:current_user).and_return(@user = Object.new)
    @params = {}
  end

  def params
    @params
  end

  it "should enable selected agents" do
    expect(@agent_service).to receive(:enableAgents).with(@user, anything(), ["UUID1", "UUID2"]) do |user, result, uuids|
      result.ok("Accepted")
    end

    @params =  {:operation => 'Enable', :selected => ["UUID1", "UUID2"], :no_layout => true}
    bulk_edit
  end

  it "should disable selected agents" do
    expect(@agent_service).to receive(:disableAgents).with(@user, anything(), ["UUID1", "UUID2"]) do |user, result, uuids|
      result.ok("Accepted")
    end

    @params = {:operation => 'Disable', :selected => ["UUID1", "UUID2"], :no_layout => true}
    bulk_edit
  end

  it "should delete selected agents" do
    expect(@agent_service).to receive(:deleteAgents).with(@user, anything(), ["UUID1", "UUID2"]) do |user, result, uuids|
      result.ok("Accepted")
    end

    @params = {:operation => 'Delete', :selected => ["UUID1", "UUID2"], :no_layout => true}
    bulk_edit
  end

  it "should add resources to selected agents" do
    selections = [TriStateSelection.new("new-resource", 'add')]
    expect(@agent_service).to receive(:modifyResources).with(@user, anything(), ["UUID1", "UUID2"], selections) do |user, result, uuids|
      result.ok("Accepted")
    end

    @params =  {:operation => 'Add_Resource', :selected => ["UUID1", "UUID2"], :add_resource => "new-resource", :no_layout => true}
    bulk_edit
  end

  it "should add multiple resources to selected agents" do
    selections = [TriStateSelection.new("new-resource", 'add'), TriStateSelection.new("old-resource", 'remove')]
    expect(@agent_service).to receive(:modifyResources).with(@user, anything(), ["UUID1", "UUID2"], selections) do |user, result, uuids|
      result.ok("Accepted")
    end

    @params = {:operation => 'Apply_Resource', :selected => ["UUID1", "UUID2"], :selections => {"new-resource" => 'add', "old-resource" => "remove"}, :no_layout => true}
    bulk_edit
  end

  it "should add/remove agents from environments" do
    selections = [TriStateSelection.new("uat", 'add'), TriStateSelection.new("prod", 'remove')]
    expect(@agent_service).to receive(:modifyEnvironments).with(@user, anything(), ["UUID1", "UUID2"], selections) do |user, result, uuids|
      result.ok("Accepted")
    end

    @params =  {:operation => 'Apply_Environment', :selected => ["UUID1", "UUID2"], :selections => {"uat" => 'add', "prod" => "remove"}, :no_layout => true}
    bulk_edit
  end

  it "should show message if there is a problem" do
    expect(@agent_service).to receive(:enableAgents).with(@user, anything(), ["UUID1", "UUID2"]) do |user, result, uuids|
      result.notAcceptable("Error message", HealthStateType.general(HealthStateScope::GLOBAL))
    end

    @params = { :operation => 'Enable', :selected => ["UUID1", "UUID2"], :no_layout => true }
    expect(bulk_edit.message()).to eq("Error message")
  end

  it "should show message for a successful bulk_edit" do
    expect(@agent_service).to receive(:enableAgents).with(@user, anything(), ["UUID1", "UUID2"]) do |user, result, uuids|
      result.ok("Enabled 3 agent(s)")
    end

    @params =  {:operation => 'Enable', :selected => ["UUID1", "UUID2"], :no_layout => true}
    expect(bulk_edit.message()).to eq("Enabled 3 agent(s)")
  end

  it "should show message for an unrecognised operation" do
    @params =  {:operation => 'BAD_OPERATION', :selected => ["UUID1", "UUID2"], :no_layout => true}

    expect(bulk_edit.message()).to eq("The operation BAD_OPERATION is not recognized.")
  end

  it "should show error if selected parameter is omitted" do
    @params = {:operation => 'Enable', :no_layout => true}

    expect(bulk_edit.message()).to eq("No agents were selected. Please select at least one agent and try again.")
  end

  it "should show error if no agents are selected" do
    @params = {:operation => 'Enable', :selected => [], :no_layout => true}

    expect(bulk_edit.message()).to eq("No agents were selected. Please select at least one agent and try again.")
  end
end

