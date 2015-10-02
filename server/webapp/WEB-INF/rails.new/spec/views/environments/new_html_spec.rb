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

describe "environments/new.html.erb" do
  before do
    assign(:available_pipelines, [])
    assign(:unavailable_pipelines, [])
    assign(:agents, [])
    assign(:environment, BasicEnvironmentConfig.new())
    allow(view).to receive(:current_user).and_return(com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo')))
    allow(view).to receive(:security_service).and_return(@security_service = Object.new)
  end

  it "should render the environment name input box" do
    render

    expect(response).to have_selector("#add_new_environment input#environment_name")
    expect(response).to have_selector("#add_new_environment input#environment_name[maxlength='255']")
    expect(response).to have_selector("#add_new_environment .section_title", :text => "Environment Name:")
  end

  it "should have the error message container" do
    render

    expect(response).to have_selector("span#add_error_message")
  end

  it "should render the tab links" do
    render

    expect(response).to have_selector(".tabs a", :text => "Step 1:Name")
    expect(response).to have_selector(".tabs a", :text => "Step 2:Add Pipelines")
    expect(response).to have_selector(".tabs a", :text => "Step 3:Add Agents")
    expect(response).to have_selector(".tabs a", :text => "Step 4:Add Environment Variables")
  end

  it "should render the submit and close buttons" do
    render

    expect(response).to have_selector("div.actions button", :text => "Cancel")
    expect(response).to have_selector("div.actions button[type='submit']", :text => "FINISH")
  end

  it "should show the pipelines title" do
    assign(:available_pipelines, [EnvironmentPipelineModel.new("first"), EnvironmentPipelineModel.new("second")])

    render

    expect(response).to have_selector("h2", :text => "Pipelines to add:")
  end

  it "should show the pipelines that can be added to the environment" do
    assign(:available_pipelines, [EnvironmentPipelineModel.new("first"), EnvironmentPipelineModel.new("second")])

    render

    verify_pipeline_is_present('first')
    verify_pipeline_is_present('second')
  end

  it "should show control to add environment variables" do
    assign(:available_pipelines, [EnvironmentPipelineModel.new("first")])

    render

    expect(response).to have_selector("h2", "Environment Variables (Name = Value)")
    expect(response).to have_selector("div.environment_variables_section .variables")
  end

  it "should show the environment name with pipeline selections while adding a new environment" do
    assign(:available_pipelines, [EnvironmentPipelineModel.new("first")])
    assign(:unavailable_pipelines, [EnvironmentPipelineModel.new("second", "foo-env")])

    render

    expect(response).to have_selector("div.form_content .pipeline_selector input#pipeline_first[name='environment[pipelines][][name]'][type='checkbox']")
    expect(response).to have_selector("div.form_content label[for='pipeline_first']")

    expect(response).to have_selector("div.form_content .unavailable_pipelines label", :text => /second/)
    expect(response).to have_selector("div.form_content .unavailable_pipelines label", :text => /(foo-env)/)
  end

  def verify_pipeline_is_present(pipeline_name)
    expect(response).to have_selector("div.form_content .pipeline_selector input#pipeline_#{pipeline_name}[name='environment[pipelines][][name]'][type='checkbox']")
    expect(response).to have_selector("div.form_content label[for='pipeline_#{pipeline_name}']")
  end
end
