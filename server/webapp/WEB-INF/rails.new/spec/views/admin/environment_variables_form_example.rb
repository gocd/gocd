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
  before do
    set(@cruise_config, "md5", "abc")
  end

  it "should populate plain text env vars for the pipeline" do
    render :template => @view_file

    Capybara.string(response.body).find('form').tap do |form|
      expect(form).to have_selector("input[name='#{@object_name}[variables][][name]'][value='env-name']")
      expect(form).to have_selector("input[name='#{@object_name}[variables][][original_name]'][value='env-name']")
      expect(form).to have_selector("input[name='#{@object_name}[variables][][valueForDisplay]'][value='env-val']")

      expect(form).to have_selector("input[name='#{@object_name}[variables][][name]'][value='env-name2']")
      expect(form).to have_selector("input[name='#{@object_name}[variables][][original_name]'][value='env-name2']")
      expect(form).to have_selector("input[name='#{@object_name}[variables][][valueForDisplay]'][value='env-val2']")

      expect(form).to have_selector("input[name='default_as_empty_list[]'][value='#{@object_name}>variables']")
    end
  end

  it "should have correct row templates" do
    render :template => @view_file

    textarea_content = Capybara.string(response.body).find('textarea#variables_variables_template').text

    expect(textarea_content).to have_selector("input[name='#{@object_name}[variables][][valueForDisplay]']")
  end

  it "should show errors" do
    errors = config_errors([EnvironmentVariableConfig::NAME, "bad env var name"], [EnvironmentVariableConfig::VALUE, "bad value"])
    set(@variables.get(0), "configErrors", errors)

    render :template => @view_file

    Capybara.string(response.body).find('form').tap do |form|
      expect(form).to have_selector("div.field_with_errors input[name='#{@object_name}[variables][][name]'][value='env-name']")
      expect(form).to have_selector("div.name_value_error", :text => "bad env var name")
      expect(form).to have_selector("div.field_with_errors input[name='#{@object_name}[variables][][valueForDisplay]'][value='env-val']")
      expect(form).to have_selector("div.name_value_error", :text => "bad value")
    end
  end
end
