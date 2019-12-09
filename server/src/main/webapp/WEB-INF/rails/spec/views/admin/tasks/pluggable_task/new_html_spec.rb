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

# This tests "admin/tasks/pluggable_task/new.html.erb" through the plugin task template.
describe "admin/tasks/plugin/new.html.erb" do
  include GoUtil
  include TaskMother
  include FormUI

  task_plugin_id = "my.curl.plugin"
  task_plugin_template = "<input ng-model=\"KEY1\" type=\"text\"><input ng-model=\"key2\" type=\"text\">"

  before :each do
    assign(:cruise_config, config = BasicCruiseConfig.new)
    set(config, "md5", "abcd1234")
    @pipeline_config = PipelineConfigMother.createPipelineConfig("pipeline-name", "foo", ["build-1"].to_java(java.lang.String))
    assign(:pipeline, @pipeline_config)

    assign(:on_cancel_task_vms, @vms = java.util.Arrays.asList([vm_for(exec_task('rm')), vm_for(ant_task), vm_for(nant_task), vm_for(rake_task), vm_for(fetch_task_with_exec_on_cancel_task)].to_java(TaskViewModel)))
    allow(view).to receive(:admin_task_create_path).and_return("task_create_path")

    about = GoPluginDescriptor::About.builder.targetOperatingSystems(["linux"]).build

    descriptor = GoPluginDescriptor.builder
                   .id(task_plugin_id).version("1.0")
                   .about(about)
                   .build

    # Fake a plugin loaded into Go's plugin registry.
    @registry = Spring.bean "defaultPluginRegistry"
    @registry.loadPlugin com.thoughtworks.go.plugin.infra.plugininfo.GoPluginBundleDescriptor.new(descriptor)

    # Fake more of its initialisation. Fake the part where Go talks to the plugin and gets its config for caching.
    PluggableTaskConfigStore.store().setPreferenceFor(task_plugin_id, TaskPreference.new(TaskMother::ApiTaskForTest.new({:display_value => "test curl", :template => task_plugin_template})))
  end

  after :each do
    PluggableTaskConfigStore.store().removePreferenceFor(task_plugin_id)
    @registry.clear() if @registry
  end

  it "should render plugin template and data for a new pluggable task" do
    pluggable_task = plugin_task task_plugin_id, [ConfigurationPropertyMother.create("KEY1", false, "value1"), ConfigurationPropertyMother.create("key2", false, "value2")]
    task = assign(:task, pluggable_task)
    assign(:task_view_model, Spring.bean("taskViewService").getViewModel(task, 'new'))

    render

    Capybara.string(response.body).find('div.plugged_task#task_angular_pluggable_task_my_curl_plugin').tap do |div|
      template_text = text_without_whitespace(div.find("div.plugged_task_template"))
      expect(template_text).to eq(task_plugin_template)

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
