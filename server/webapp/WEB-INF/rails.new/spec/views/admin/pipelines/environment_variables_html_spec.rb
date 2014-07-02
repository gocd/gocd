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
    set_variables
    @pipeline = PipelineConfigMother.pipelineConfigWithTimer("pipeline-name", "1 1 1 1 1 1 1")

    variables = EnvironmentVariablesConfig.new
    variables.addAll(@variables)
    @encryptedVariable = EnvironmentVariableConfig.new(GoCipher.new, "password", "=%@#SFR", true)
    variables.add(@encryptedVariable)

    @pipeline.setVariables(variables)
    assigns[:pipeline] = @pipeline

    assigns[:cruise_config] = @cruise_config = CruiseConfig.new
    @cruise_config.addPipeline("group-1", @pipeline)

    in_params(:pipeline_name => "foo_bar", :action => "new", :controller => "admin/pipelines")

    @view_file = "admin/pipelines/environment_variables.html"
    @object_name = 'pipeline'
  end
  
  it_should_behave_like 'environment_variables_form'

  describe "encrypted_environment_variables" do

    it "should display the secure variables section" do
      render @view_file

      response.body.should have_tag("h3", "Secure Variables");
    end

    it "should populate secure env vars for the pipeline" do
      render @view_file

      response.body.should have_tag("form") do
        with_tag("input[name='#{@object_name}[variables][][name]'][value='password']")
        with_tag("input[name='#{@object_name}[variables][][original_name]'][value='password']")
        with_tag("input[name='#{@object_name}[variables][][valueForDisplay]'][value='#{@encryptedVariable.getEncryptedValue()}'][type='password']")
        with_tag("input[name='#{@object_name}[variables][][secure]'][value='true']")

        with_tag("input[name='default_as_empty_list[]'][value='#{@object_name}>variables']")
      end
    end

    it "should have correct row templates for secure section" do
      render @view_file

      response.body.should have_tag("form") do
        with_tag("textarea#variables_secure_variables_template") do
          with_tag("input[name='pipeline[variables][][valueForDisplay]'][type='password']")
          with_tag("input#pipeline_variables__secure")
          with_tag("input[type='hidden'][name='#{@object_name}[variables][][#{com.thoughtworks.go.config.EnvironmentVariableConfig::ISCHANGED}]'][value='true']")
        end
      end
    end

    it "should show edit link" do
      render @view_file

      response.body.should have_tag("form") do
        with_tag("input[name='#{@object_name}[variables][][valueForDisplay]'][value='#{@encryptedVariable.getEncryptedValue()}'][type='password'][readonly='readonly']")
        with_tag("input[name='#{@object_name}[variables][][originalValue]'][value='#{@encryptedVariable.getEncryptedValue()}'][type='hidden']")
        with_tag("input[type='hidden'][name='#{@object_name}[variables][][#{com.thoughtworks.go.config.EnvironmentVariableConfig::ISCHANGED}]'][value='false']")
        with_tag("a.edit.skip_dirty_stop", "Edit")
        with_tag("a.reset.hidden.skip_dirty_stop", "Reset")
      end
    end
  end

end
