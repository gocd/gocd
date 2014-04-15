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

shared_examples_for :environment_variables_form do
  def set_variables
    @variables = EnvironmentVariablesConfig.new()
    @variables.add("env-name", "env-val")
    @variables.add("env-name2", "env-val2")
  end

  before do
    set(@cruise_config, "md5", "abc")
  end

  it "should populate plain text env vars for the pipeline" do
    render @view_file

    response.body.should have_tag("form") do
      with_tag("input[name='#{@object_name}[variables][][name]'][value='env-name']")
      with_tag("input[name='#{@object_name}[variables][][original_name]'][value='env-name']")
      with_tag("input[name='#{@object_name}[variables][][valueForDisplay]'][value='env-val']")

      with_tag("input[name='#{@object_name}[variables][][name]'][value='env-name2']")
      with_tag("input[name='#{@object_name}[variables][][original_name]'][value='env-name2']")
      with_tag("input[name='#{@object_name}[variables][][valueForDisplay]'][value='env-val2']")

      with_tag("input[name='default_as_empty_list[]'][value='#{@object_name}>variables']")
    end
  end

  it "should have correct row templates" do
    render @view_file

    with_tag("textarea#variables_variables_template") do
      with_tag("input[name='#{@object_name}[variables][][valueForDisplay]']")
    end
  end

  it "should show errors" do
    errors = config_errors([EnvironmentVariableConfig::NAME, "bad env var name"], [EnvironmentVariableConfig::VALUE, "bad value"])
    set(@variables.get(0), "configErrors", errors)

    render @view_file

    response.body.should have_tag("form") do
      with_tag("div.fieldWithErrors input[name='#{@object_name}[variables][][name]'][value='env-name']")
      with_tag("div.name_value_error", "bad env var name")
      with_tag("div.fieldWithErrors input[name='#{@object_name}[variables][][valueForDisplay]'][value='env-val']")
      with_tag("div.name_value_error", "bad value")
    end
  end
end
