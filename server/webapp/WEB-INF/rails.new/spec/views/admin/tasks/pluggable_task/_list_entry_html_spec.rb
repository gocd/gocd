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

describe "admin/tasks/pluggable_task/list_entry.html.erb" do
  include TaskMother

  it "should list pluggable task in the listing of tasks with plugin name" do
    task = plugin_task "curl.plugin", [ConfigurationPropertyMother.create("KEY1", false, "value1"), ConfigurationPropertyMother.create("key2", true, "encrypted_value")]

    render :partial => "admin/tasks/pluggable_task/list_entry.html", :locals => {:scope => {:task_config => task, :tvm => tvm_for(task), :tvm_of_cancel_task => nil}, :modify_onclick_callback => "on-click-callback"}
    capybara_response = "<table><tr>" + response.body + "</tr></table>" # to help Capybara parse html

    expect(capybara_response).to have_selector("td a[href='#']", :text => "Curl - Download")
    expect(capybara_response).not_to have_selector("td a.missing_plugin_link")
    expect(capybara_response).to have_selector("td.run_ifs", :text => "Passed")

    Capybara.string(capybara_response).find('td.properties').tap do |td|
      td.find("ul").tap do |ul|
        ul.find("li.task_property.key1").tap do |li|
          expect(li).to have_selector("span.name", :text => "KEY1:")
          expect(li).to have_selector("span.value", :text => "value1")
        end

        ul.find("li.task_property.key2").tap do |li|
          expect(li).to have_selector("span.name", :text => "key2:")
          expect(li).to have_selector("span.value", :text => "****")
        end
      end
    end

    expect(capybara_response).to have_selector("td.has_on_cancel", :text => "No")
  end

  it "should display a missing icon if the associated plugin for a task is missing" do
    pluginId = "missing.plugin"
    task = plugin_task pluginId, [ConfigurationPropertyMother.create("KEY1", false, "value1")]

    render :partial => "admin/tasks/pluggable_task/list_entry.html", :locals => {:scope => {:task_config => task, :tvm => tvm_for_missing_plugin(task), :tvm_of_cancel_task => nil}, :modify_onclick_callback => "on-click-callback"}
    capybara_response = "<table><tr>" + response.body + "</tr></table>" # to help Capybara parse html

    Capybara.string(capybara_response).all('td').tap do |tds|
      expect(tds[0]).to have_selector("label.missing_plugin_link", :text => pluginId)
    end

    Capybara.string(capybara_response).find('td.properties').tap do |td|
      td.find("ul").tap do |ul|
        ul.find("li.task_property.key1").tap do |li|
          expect(li).to have_selector("span.name", :text => "KEY1:")
          expect(li).to have_selector("span.value", :text => "value1")
        end
      end
    end

    expect(capybara_response).to have_selector("td.has_on_cancel", :text => "No")
  end

  it "should list pluggable task, which has on-cancel task" do
    task_plugin = simple_task_plugin_with_on_cancel_config
    tvm_of_cancel_task = TaskViewModel.new(task_plugin.cancelTask(), "list-entry", "erb")

    render :partial => "admin/tasks/pluggable_task/list_entry.html", :locals => {:scope => {:task_config => task_plugin, :tvm => tvm_for(task_plugin), :tvm_of_cancel_task => tvm_of_cancel_task}, :modify_onclick_callback => "on-click-callback"}
    capybara_response = "<table><tr>" + response.body + "</tr></table>" # to help Capybara parse html

    expect(capybara_response).to have_selector("td.has_on_cancel", :text => "Custom Command")
  end

  def tvm_for(task)
    PluggableTaskViewModel.new task, "admin/tasks/pluggable_task/list_entry", com.thoughtworks.go.plugins.presentation.Renderer::ERB, "Curl - Download" , "Curl - Template"
  end

  def tvm_for_missing_plugin(task)
    MissingPluggableTaskViewModel.new task, "admin/tasks/pluggable_task/list_entry", com.thoughtworks.go.plugins.presentation.Renderer::ERB
  end
end
