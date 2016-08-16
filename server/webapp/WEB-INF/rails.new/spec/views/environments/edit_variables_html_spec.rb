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

describe "environments/edit_variables.html.erb" do
  include GoUtil, FormUI, ReflectiveUtil

  before do
    @environment = BasicEnvironmentConfig.new()
    @environment.addEnvironmentVariable("plain_name", "plain_value")
    assign(:environment, @environment)

    view.stub(:environment_update_path).and_return("update_path")
    view.stub(:cruise_config_md5).and_return("foo_bar_baz")

    render
  end

  it "should display existing variables" do
    Capybara.string(response.body).find("div.plain-text-variables table.variables").tap do |variables|
      expect(variables).to have_selector("input.environment_variable_name[name='environment[variables][][name]'][value='plain_name']")
      expect(variables).to have_selector("input.environment_variable_value[name='environment[variables][][valueForDisplay]'][value='plain_value']")
    end
  end

  it "should have cruise_config_md5 as part of output" do
    expect(response.body).to have_selector("form input[type='hidden'][name='cruise_config_md5'][value='foo_bar_baz']")
  end

  it "should have a template for newly added environment variables" do
    Capybara.string(response.body).find('div.plain-text-variables tbody.template', visible: false).tap do |template|
      expect(template).to have_selector("input.environment_variable_name[name='environment[variables][][name]']", visible: false)
      expect(template).to have_selector("input.environment_variable_value[name='environment[variables][][valueForDisplay]']", visible: false)
    end
  end
end
