#
# Copyright 2019 ThoughtWorks, Inc.
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
#

require 'rails_helper'
require_relative '../environment_variables_form_example'

describe "admin/stages/environment_variables.html.erb" do
  include GoUtil
  include ReflectiveUtil
  include FormUI

  before(:each) do
    @variables = EnvironmentVariablesConfig.new()
    @variables.add("env-name", "env-val")
    @variables.add("env-name2", "env-val2")
    @encryptedVariable = EnvironmentVariableConfig.new(GoCipher.new, "password", "=%@#SFR", true)
    @variables.add(@encryptedVariable)

    pipeline = PipelineConfigMother.createPipelineConfig("pipeline-name", "stage-name", ["job-name"].to_java(java.lang.String))
    @stage = pipeline.get(0)
    @stage.setVariables(@variables)
    assign(:pipeline, pipeline)
    assign(:stage, @stage)

    @cruise_config = BasicCruiseConfig.new
    assign(:cruise_config, @cruise_config)
    @cruise_config.addPipeline("group-1", pipeline)

    in_params(stage_parent: "pipelines", pipeline_name: "foo_bar", stage_name: "stage-name", action: "edit", controller: "admin/stages", current_tab: "environment_variables")

    @view_file = "admin/stages/environment_variables.html.erb"
    @object_name = 'stage'
  end

  it_should_behave_like :environment_variables_form
  it_should_behave_like :secure_environment_variables_form
end
