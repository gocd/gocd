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

require File.join(File.dirname(__FILE__), "/../../../../spec_helper")

describe "admin/tasks/pluggable_task/edit.html.erb" do
  include TaskMother

  it "should render plugin template and data for existing pluggable task" do
    task = plugin_task "curl.plugin", [ConfigurationPropertyMother.create("KEY1", false, "value1"), ConfigurationPropertyMother.create("key2", true, "encrypted_value")]

    formNameProvider = double("formNameProvider")
    formNameProvider.stub(:form_name_prefix).and_return("form_prefix")
    formNameProvider.stub(:css_id_for).with("angular_pluggable_task_curl_plugin").and_return("angular_plugin_name")
    formNameProvider.stub(:css_id_for).with("data_pluggable_task_curl_plugin").and_return("plugin_data")

    render :template => "admin/tasks/pluggable_task/edit.html.erb", :locals => {
            :scope => {:task => task},
            :local_assigns => {"formNameProvider" => formNameProvider, "template" => "PLUGIN TEMPLATE 1", "data" => "PLUGIN DATA 1"}}

    Capybara.string(response.body).find('div.plugged_task#angular_plugin_name').tap do |div|
      expect(div).to have_selector("div.plugged_task_template", :text => "PLUGIN TEMPLATE 1")
      expect(div).to have_selector("span.plugged_task_data", :text => "PLUGIN DATA 1")
    end
  end
end