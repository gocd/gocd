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

require File.expand_path(File.dirname(__FILE__) + '/../../spec_helper')

describe "environments/new.html" do
  before do
    assigns[:available_pipelines] = []
    assigns[:unavailable_pipelines] = []
    assigns[:agents] = []
    assigns[:environment] = EnvironmentConfig.new()
    template.stub!(:current_user).and_return(com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new('user_foo')))
    template.stub!(:security_service).and_return(@security_service = Object.new)
  end

  it "should render the environment name input box" do
    render "environments/new.html"

    response.body.should have_tag("#add_new_environment input#environment_name")
    response.body.should have_tag("#add_new_environment input#environment_name[maxLength='255']")
    response.body.should have_tag("#add_new_environment .section_title", "Environment Name:")
  end

  it "should have the error message container" do
    render "environments/new.html"

    response.body.should have_tag("span#add_error_message")
  end

  it "should render the tab links" do
    render "environments/new.html"

    response.body.should have_tag(".tabs a", "Step 1:Name")
    response.body.should have_tag(".tabs a", "Step 2:Add Pipelines")
    response.body.should have_tag(".tabs a", "Step 3:Add Agents")
    response.body.should have_tag(".tabs a", "Step 4:Add Environment Variables")
  end

  it "should render the submit and close buttons" do
    render "environments/new.html"

    body = response.body
    body.should have_tag("div.actions button", "Cancel")
    body.should have_tag("div.actions button[type='submit']", "FINISH")
  end

  it "should show the pipelines title" do
    assigns[:available_pipelines] = [EnvironmentPipelineModel.new("first"), EnvironmentPipelineModel.new("second")]

    render "environments/new.html"

    response.body.should have_tag("h2", "Pipelines to add:")
  end

  it "should show the pipelines that can be added to the environment" do
    assigns[:available_pipelines] = [EnvironmentPipelineModel.new("first"), EnvironmentPipelineModel.new("second")]

    render "environments/new.html"
    
    verify_pipeline_is_present('first')
    verify_pipeline_is_present('second')
  end

  it "should show control to add environment variables" do
    assigns[:available_pipelines] = [EnvironmentPipelineModel.new("first")]

    render "environments/new.html"

    response.body.should have_tag("h2", "Environment Variables (Name = Value)")
    response.body.should have_tag('div.environment_variables_section') do
      with_tag("ul.variables")
    end
  end

  it "should show the environment name with pipeline selections while adding a new environment" do
    assigns[:available_pipelines] = [EnvironmentPipelineModel.new("first")]
    assigns[:unavailable_pipelines] = [EnvironmentPipelineModel.new("second", "foo-env")]
    render "environments/new.html"

    response.body.should have_tag("div.form_content .pipeline_selector input#pipeline_first[name='environment[pipelines][][name]'][type='checkbox']")
    response.body.should have_tag("div.form_content label[for='pipeline_first']")

    response.body.should have_tag("div.form_content .unavailable_pipelines label", /second/)
    response.body.should have_tag("div.form_content .unavailable_pipelines label", /(foo-env)/)
  end

  def verify_pipeline_is_present(pipeline_name)
    response.body.should have_tag("div.form_content .pipeline_selector input#pipeline_#{pipeline_name}[name='environment[pipelines][][name]'][type='checkbox']")
    response.body.should have_tag("div.form_content label[for='pipeline_#{pipeline_name}']")
  end
end
  