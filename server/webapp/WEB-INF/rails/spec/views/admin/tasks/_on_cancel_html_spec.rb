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

require File.join(File.dirname(__FILE__), "/../../../spec_helper")

describe "admin/tasks/_on_cancel.html.erb" do
  include TaskMother

  before(:each) do
    @store = stub(:PluggableTaskConfigStore)
  end

  it "should display error message when the on cancel task plugin is missing" do
    @task = simple_task_with_pluggable_on_cancel_task
    assigns[:on_cancel_task_vms] = []
    fields_for(:task, @task) do |f|
      @form = f
    end
    @store.stub(:preferenceFor).with("curl.plugin").and_return(nil)


    render :partial => "admin/tasks/on_cancel.html", :locals => {:scope => {:task => @task, :form => @form, :config_store => @store}}
    response.body.should have_tag("div.on_cancel") do
      with_tag("div#plugin_missing_error .warning", "Associated plugin 'curl.plugin' not found. Please contact the Go admin to install the plugin.")
    end
  end

  it "should not display error message when the on cancel is a regular task like rake" do
    @task = task_with_on_cancel_task
    assigns[:on_cancel_task_vms] = []
    fields_for(:task, @task) do |f|
      @form = f
    end

    render :partial => "admin/tasks/on_cancel.html", :locals => {:scope => {:task => @task, :form => @form, :config_store => @store}}
    response.body.should have_tag("div.on_cancel") do
      without_tag("div#plugin_missing_error")
    end
  end

  it "should not display plugin missing error message if pluggable on cancel task is not missing" do
    assigns[:on_cancel_task_vms] = []
    fields_for(:task, @task) do |f|
      @form = f
    end
    @task = simple_task_with_pluggable_on_cancel_task
    @store.stub(:preferenceFor).with("curl.plugin").and_return(stub(:Preference))
    render :partial => "admin/tasks/on_cancel.html", :locals => {:scope => {:task => @task, :form => @form, :config_store => @store}}
    response.body.should have_tag("div.on_cancel") do
      without_tag("div#plugin_missing_error", "Associated plugin 'curl.plugin' not found. Please contact the Go admin to install the plugin.")
    end
  end
end