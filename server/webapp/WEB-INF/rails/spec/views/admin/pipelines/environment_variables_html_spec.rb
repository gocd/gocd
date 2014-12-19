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

require File.join(File.dirname(__FILE__), "/../../../spec_helper")
load File.join(File.dirname(__FILE__), "..", "environment_variables_form_example.rb")

describe "admin/pipelines/environment_variables.html.erb" do
  include GoUtil, ReflectiveUtil, FormUI

  before(:each) do
    @variables = EnvironmentVariablesConfig.new()
    @variables.add("env-name", "env-val")
    @variables.add("env-name2", "env-val2")

    @pipeline = PipelineConfigMother.pipelineConfigWithTimer("pipeline-name", "1 1 1 1 1 1 1")

    variables = EnvironmentVariablesConfig.new
    variables.addAll(@variables)
    @encryptedVariable = EnvironmentVariableConfig.new(GoCipher.new, "password", "=%@#SFR", true)
    variables.add(@encryptedVariable)

    @pipeline.setVariables(variables)
    assign(:pipeline, @pipeline)

    assign(:cruise_config, @cruise_config = CruiseConfig.new)
    @cruise_config.addPipeline("group-1", @pipeline)

    in_params(:pipeline_name => "foo_bar", :action => "new", :controller => "admin/pipelines")

    @view_file = "admin/pipelines/environment_variables.html.erb"
    @object_name = 'pipeline'
  end
  
  it_should_behave_like :environment_variables_form

  describe "encrypted_environment_variables" do

    it "should display the secure variables section" do
      render :template => @view_file

      expect(response.body).to have_selector("h3", :text => "Secure Variables");
    end

    it "should populate secure env vars for the pipeline" do
      render :template => @view_file

      Capybara.string(response.body).find('form').tap do |form|
        expect(form).to have_selector("input[name='#{@object_name}[variables][][name]'][value='password']")
        expect(form).to have_selector("input[name='#{@object_name}[variables][][original_name]'][value='password']")
        expect(form).to have_selector("input[name='#{@object_name}[variables][][valueForDisplay]'][value='#{@encryptedVariable.getEncryptedValue()}'][type='password']")
        expect(form).to have_selector("input[name='#{@object_name}[variables][][secure]'][value='true']")

        expect(form).to have_selector("input[name='default_as_empty_list[]'][value='#{@object_name}>variables']")
      end
    end

    it "should have correct row templates for secure section" do
      render :template => @view_file

      Capybara.string(response.body).find('form').tap do |form|
        textarea_content = form.find("textarea#variables_secure_variables_template").text

        expect(textarea_content).to have_selector("input[name='pipeline[variables][][valueForDisplay]'][type='password']")
        expect(textarea_content).to have_selector("input#pipeline_variables__secure")
        expect(textarea_content).to have_selector("input[type='hidden'][name='#{@object_name}[variables][][#{com.thoughtworks.go.config.EnvironmentVariableConfig::ISCHANGED}]'][value='true']")
      end
    end

    it "should show edit link" do
      render :template => @view_file

      Capybara.string(response.body).find('form').tap do |form|
        expect(form).to have_selector("input[name='#{@object_name}[variables][][valueForDisplay]'][value='#{@encryptedVariable.getEncryptedValue()}'][type='password'][readonly='readonly']")
        expect(form).to have_selector("input[name='#{@object_name}[variables][][originalValue]'][value='#{@encryptedVariable.getEncryptedValue()}'][type='hidden']")
        expect(form).to have_selector("input[type='hidden'][name='#{@object_name}[variables][][#{com.thoughtworks.go.config.EnvironmentVariableConfig::ISCHANGED}]'][value='false']")
        expect(form).to have_selector("a.edit.skip_dirty_stop", :text => "Edit")
        expect(form).to have_selector("a.reset.hidden.skip_dirty_stop", :text => "Reset")
      end
    end
  end

end
