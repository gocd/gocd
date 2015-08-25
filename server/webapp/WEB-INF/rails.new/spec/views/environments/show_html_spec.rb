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

describe 'environments/show.html.erb' do
  before(:each) do
    @environment = EnvironmentConfigMother.environment("foo")
    @environment.addPipeline(CaseInsensitiveString.new "another-pipeline")
    @environment.addEnvironmentVariable("ENV1", "VAL1")
    @environment.addEnvironmentVariable("ENV2", "VAL2")
    @environment.getVariables().add(EnvironmentVariableConfig.new(GoCipher.new, "SECURE_VAR", "SECURE_VALUE", true))

    @agent_details = AgentsViewModelMother.getTwoAgents()

    assign(:environment, @environment)
    assign(:agent_details, @agent_details)

    render
  end

  it "should have a pipelines section listing all pipelines and an edit button" do
    Capybara.string(response.body).find(".environment.show_environment .added_item.added_pipelines").tap do |pipelines_section|
      all_pipeline_names = pipelines_section.all("ul li").collect {|node| node.text}.sort

      expect(all_pipeline_names).to eq(["another-pipeline", "foo-pipeline"])
      expect(pipelines_section).to have_selector("h3", text: "PIPELINES")
      expect(pipelines_section).to have_selector("h3 button#edit_pipelines", text: "Edit")
    end
  end

  it "should have an agents section listing all agents and an edit button" do
    Capybara.string(response.body).find(".environment.show_environment .added_item.added_agents").tap do |agents_section|
      all_agent_names = agents_section.all("ul li").collect {|node| node.text}.sort

      expect(all_agent_names).to eq(["CCeDev01 (10.18.5.1)", "CCeDev01 (10.18.5.1)"])
      expect(agents_section).to have_selector("h3", text: "AGENTS")
      expect(agents_section).to have_selector("h3 button#edit_agents", text: "Edit")
    end
  end

  it "should have a variables section listing all agents and an edit button" do
    Capybara.string(response.body).find(".environment.show_environment .added_item.added_environment_variables").tap do |variables_section|
      all_variables = variables_section.all("ul li").collect {|node| node.text}.sort

      expect(all_variables).to eq(["ENV1 = VAL1", "ENV2 = VAL2", "SECURE_VAR = ****"])
      expect(variables_section).to have_selector("h3", text: "Environment Variables")
      expect(variables_section).to have_selector("h3 button#edit_environment_variables", text: "Edit")
    end
  end

  it "should hookup every edit button to a modal box for editing" do
    expect(response).to match /jQuery\('#edit_pipelines'\).*\n.*ajax_modal\("\/environments\/foo\/edit\/pipelines".*title: "Pipelines"/
    expect(response).to match /jQuery\('#edit_agents'\).*\n.*ajax_modal\("\/environments\/foo\/edit\/agents".*title: "Agents"/
    expect(response).to match /jQuery\('#edit_environment_variables'\).*\n.*ajax_modal\("\/environments\/foo\/edit\/variables".*title: "Environment Variables"/
  end
end
