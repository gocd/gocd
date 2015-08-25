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
load File.join(File.dirname(__FILE__), "..", "environment_variables_form_example.rb")

describe "admin/pipelines/environment_variables.html.erb" do
  include GoUtil, ReflectiveUtil, FormUI

  before(:each) do
    @variables = EnvironmentVariablesConfig.new()
    @variables.add("env-name", "env-val")
    @variables.add("env-name2", "env-val2")
    @encryptedVariable = EnvironmentVariableConfig.new(GoCipher.new, "password", "=%@#SFR", true)
    @variables.add(@encryptedVariable)

    @pipeline = PipelineConfigMother.pipelineConfigWithTimer("pipeline-name", "1 1 1 1 1 1 1")

    @pipeline.setVariables(@variables)
    assign(:pipeline, @pipeline)

    @cruise_config = BasicCruiseConfig.new

    assign(:cruise_config, @cruise_config)
    @cruise_config.addPipeline("group-1", @pipeline)

    in_params(pipeline_name: "foo_bar", action: "new", controller: "admin/pipelines")

    @view_file = "admin/pipelines/environment_variables.html.erb"
    @object_name = 'pipeline'
  end

  it_should_behave_like :environment_variables_form
  it_should_behave_like :secure_environment_variables_form
end
