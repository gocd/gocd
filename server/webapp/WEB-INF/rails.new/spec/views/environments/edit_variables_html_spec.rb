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
  include GoUtil, FormUI, ReflectiveUtil
  
  before do
    @environment = EnvironmentConfig.new()
    @environment.addEnvironmentVariable("plain_name", "plain_value")
    assigns[:environment] = @environment

    template.stub(:environment_update_path).and_return("update_path")
    template.stub(:cruise_config_md5).and_return("md5")
  end

  it "should display existing variables" do
    render "environments/edit_variables.html"

    response.should have_tag("ul.variables") do
      with_tag("input.environment_variable_name[name='environment[variables][][name]'][value='plain_name']")
      with_tag("input.environment_variable_value[name='environment[variables][][valueForDisplay]'][value='plain_value']")
    end
  end
end