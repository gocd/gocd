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

describe "/agents/index" do
  include AgentMother

  before :each do
    @agent1 = idle_agent(:hostname => 'host1', :location => '/var/lib/cruise-agent', :operating_system => "Linux", :uuid => "UUID_host1")
    @agent2 = disabled_agent
    assign(:agents, AgentsViewModel.new([@agent1, @agent2].to_java(AgentViewModel)))
    view.stub(:has_operate_permission_for_agents?).and_return(true)
    view.stub(:url_for).and_return("url")
  end

  it "should not link to agent details for a pending agent" do
    assign(:agents, AgentsViewModel.new([pending_agent].to_java(AgentViewModel)))

    render :partial => "agents/agents_table.html",:locals => {:scope => {:sortable_columns => false}}

    Capybara.string(response.body).find("#agent_details").tap do |agent_details|
      expect(agent_details).to_not have_selector("a", :text => "CCeDev03")
      expect(agent_details).to have_selector("span", :text => "CCeDev03")
    end
  end

  it "should not make the column sortable if sortable is false" do
    render :partial => "agents/agents_table.html",:locals => {:scope => {:sortable_columns => false}}
    expect(response.body).to have_selector("th span", :text => "Agent Name")
  end

  it "should make the column sortable if sortable is true" do
    render :partial => "agents/agents_table.html",:locals => {:scope => {:sortable_columns => true}}

    expect(response.body).to have_selector("th a span", :text => "Agent Name")
  end

  it "should show status only for disabled agents if show_only_disabled is true" do
    render :partial => "agents/agents_table.html",:locals => {:scope => {:show_only_disabled => true}}

    expect(response.body).to_not have_selector("tr.Idle")
    expect(response.body).to have_selector("tr.Disabled")
  end

  it "should show all status by default if show_only_disabled is not given or is false" do
    render :partial => "agents/agents_table.html",:locals => {:scope => {}}

    expect(response.body).to have_selector("tr.Idle")
    expect(response.body).to have_selector("tr.Disabled")
  end

  it "should sanitize agent details before showing them on the UI" do
    dangerous_hostname_value = '<script>HOST</script>'
    dangerous_location_value = '<script>LOCATION</script>'
    dangerous_os_value = "<script>OS</script>"
    dangerous_uuid_value = "<script>UUID</script>"
    dangerous_resources_value = "<script>RESOURCES</script>"
    dangerous_environments_value = "<script>ENVIRONMENTS</script>"
    agent_with_dangerous_values = idle_agent(
        :hostname => dangerous_hostname_value,
        :location => dangerous_location_value,
        :operating_system => dangerous_os_value,
        :uuid => dangerous_uuid_value,
        :resources => dangerous_resources_value,
        :environments => [dangerous_environments_value]
    )

    assign(:agents, AgentsViewModel.new([agent_with_dangerous_values].to_java(AgentViewModel)))


    render :partial => "agents/agents_table.html",:locals => {:scope => {}}


    sanitize = proc {|val| val.gsub('<', '&lt;').gsub('>', '&gt;')}
    expect(response.body).to_not include(dangerous_hostname_value)
    expect(response.body).to include(sanitize.call dangerous_hostname_value)

    expect(response.body).to_not match(dangerous_location_value)
    expect(response.body).to include(sanitize.call dangerous_location_value)

    expect(response.body).to_not match(dangerous_os_value)
    expect(response.body).to include(sanitize.call dangerous_os_value)

    expect(response.body).to_not match(dangerous_resources_value)
    expect(response.body).to include(sanitize.call dangerous_resources_value)

    expect(response.body).to_not match(dangerous_environments_value)
    expect(response.body).to include(sanitize.call dangerous_environments_value)
  end
end
