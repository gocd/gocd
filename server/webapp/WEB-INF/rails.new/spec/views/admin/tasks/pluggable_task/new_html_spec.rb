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

# This tests "admin/tasks/pluggable_task/new.html.erb" through the plugin task template.
describe "admin/tasks/plugin/new.html.erb" do
  include GoUtil, TaskMother, FormUI

  TASK_PLUGIN_ID = "my.curl.plugin"
  TASK_PLUGIN_TEMPLATE = "<input ng-model=\"KEY1\" type=\"text\"><input ng-model=\"key2\" type=\"text\">"

  before :each do
    assign(:cruise_config, config = BasicCruiseConfig.new)
    set(config, "md5", "abcd1234")

    assign(:on_cancel_task_vms, @vms =  java.util.Arrays.asList([vm_for(exec_task('rm')), vm_for(ant_task), vm_for(nant_task), vm_for(rake_task), vm_for(fetch_task_with_exec_on_cancel_task)].to_java(TaskViewModel)))
    view.stub(:admin_task_create_path).and_return("task_create_path")

    # Fake a plugin loaded into Go's plugin registry.
    @registry = Spring.bean "defaultPluginRegistry"
    @registry.loadPlugin GoPluginDescriptor.new(TASK_PLUGIN_ID, "1.0", GoPluginDescriptor::About.new(nil, nil, nil, nil, nil, ["Linux"]), nil, nil, false)

    # Fake more of its initialisation. Fake the part where Go talks to the plugin and gets its config for caching.
    PluggableTaskConfigStore.store().setPreferenceFor(TASK_PLUGIN_ID, TaskPreference.new(TaskMother::ApiTaskForTest.new({:display_value => "test curl", :template => TASK_PLUGIN_TEMPLATE})))
  end

  after :each do
    PluggableTaskConfigStore.store().removePreferenceFor(TASK_PLUGIN_ID)
    @registry.unloadAll() if @registry
  end

  it "should render plugin template and data for a new pluggable task" do
    pluggable_task = plugin_task TASK_PLUGIN_ID, [ConfigurationPropertyMother.create("KEY1", false, "value1"), ConfigurationPropertyMother.create("key2", false, "value2")]
    task = assign(:task, pluggable_task)
    assign(:task_view_model, Spring.bean("taskViewService").getViewModel(task, 'new'))

    render

    Capybara.string(response.body).find('div.plugged_task#task_angular_pluggable_task_my_curl_plugin').tap do |div|
      template_text = text_without_whitespace(div.find("div.plugged_task_template"))
      expect(template_text).to eq(TASK_PLUGIN_TEMPLATE)

      data_for_template = JSON.parse(div.find("span.plugged_task_data", :visible => false).text)
      expect(data_for_template.keys.sort).to eq(["KEY1", "key2"])
      expect(data_for_template["KEY1"]).to eq({"value" => "value1"})
      expect(data_for_template["key2"]).to eq({"value" => "value2"})
    end
  end

  def text_without_whitespace element
    element.native.inner_html.gsub(/^[\n ]*/, '').gsub(/[\n ]*$/, '')
  end
end
