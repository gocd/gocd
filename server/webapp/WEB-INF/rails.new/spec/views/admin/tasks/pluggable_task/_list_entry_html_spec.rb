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

describe "admin/tasks/pluggable_task/list_entry.html.erb" do
  include TaskMother

  it "should list pluggable task in the listing of tasks with plugin name" do
    task = plugin_task "curl.plugin", [ConfigurationPropertyMother.create("KEY1", false, "value1"), ConfigurationPropertyMother.create("key2", true, "encrypted_value")]

    render "admin/tasks/pluggable_task/_list_entry", :locals => {:scope => {:task_config => task, :tvm => tvm_for(task), :tvm_of_cancel_task => nil}, :modify_onclick_callback => "on-click-callback"}

    response.body.should have_tag("td a[href='#']", "Curl - Download")
    response.body.should_not have_tag("td a.missing_plugin_link")
    response.body.should have_tag("td.run_ifs", "Passed")
    response.body.should have_tag("td.properties") do
      with_tag("ul") do
        with_tag("li.task_property.key1") do
          with_tag("span.name", "KEY1:")
          with_tag("span.value", "value1")
        end

        with_tag("li.task_property.key2") do
          with_tag("span.name", "key2:")
          with_tag("span.value", "****")
        end
      end
    end
    response.body.should have_tag("td.has_on_cancel", "No")
  end


  it "should display a missing icon if the associated plugin for a task is missing" do
    pluginId = "missing.plugin"
    task = plugin_task pluginId, [ConfigurationPropertyMother.create("KEY1", false, "value1")]

    render "admin/tasks/pluggable_task/_list_entry", :locals => {:scope => {:task_config => task, :tvm => tvm_for_missing_plugin(task), :tvm_of_cancel_task => nil}, :modify_onclick_callback => "on-click-callback"}

    response.body.should have_tag("td label", pluginId)
    response.body.should have_tag("td label.missing_plugin_link", pluginId)
    response.body.should have_tag("td.properties") do
      with_tag("ul") do
        with_tag("li.task_property.key1") do
          with_tag("span.name", "KEY1:")
          with_tag("span.value", "value1")
        end
      end
    end
    response.body.should have_tag("td.has_on_cancel", "No")
  end

  it "should list pluggable task, which has on-cancel task" do
    task_plugin = simple_task_plugin_with_on_cancel_config
    tvm_of_cancel_task = TaskViewModel.new(task_plugin.cancelTask(), "list-entry", "erb")

    render "admin/tasks/pluggable_task/_list_entry", :locals => {:scope => {:task_config => task_plugin, :tvm => tvm_for(task_plugin), :tvm_of_cancel_task => tvm_of_cancel_task}, :modify_onclick_callback => "on-click-callback"}

    response.body.should have_tag("td.has_on_cancel", "Custom Command")
  end

  def tvm_for(task)
    PluggableTaskViewModel.new task, "admin/tasks/pluggable_task/list_entry", com.thoughtworks.go.plugins.presentation.Renderer::ERB, "Curl - Download" , "Curl - Template"
  end

  def tvm_for_missing_plugin(task)
    MissingPluggableTaskViewModel.new task, "admin/tasks/pluggable_task/list_entry", com.thoughtworks.go.plugins.presentation.Renderer::ERB
  end
end