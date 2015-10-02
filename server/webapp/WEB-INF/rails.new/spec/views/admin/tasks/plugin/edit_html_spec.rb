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

describe "/admin/tasks/plugin/edit.html.erb" do
  include GoUtil, TaskMother, FormUI

  before :each do
    assign(:cruise_config, config = BasicCruiseConfig.new)
    set(config, "md5", "abcd1234")

    view.stub(:admin_task_update_path).and_return("task_update_path")
    assign(:task, @task = simple_exec_task)
    assign(:task_view_model, @tvm = vm_for(@task))
    assign(:on_cancel_task_vms, @vms =  java.util.Arrays.asList([vm_for(exec_task('rm')), vm_for(ant_task), vm_for(nant_task), vm_for(rake_task), vm_for(fetch_task)].to_java(TaskViewModel)))
  end

  it "should render what the rendering service returns" do
    render

    Capybara.string(response.body).find("form[action='task_update_path'][method='post']").tap do |form|
      expect(form).to have_selector("input[type='hidden'][name='_method'][value='PUT']")
      expect(form).to have_selector("label", :text => "Command*")
      expect(form).to have_selector("input[name='task[#{com.thoughtworks.go.config.ExecTask::COMMAND}]'][value='ls']")
    end
  end

  it "should render the config md5, form buttons and flash message" do
    render

    expect(response.body).to have_selector("#message_pane")

    Capybara.string(response.body).find("form").tap do |form|
      expect(form).to have_selector("input[id='config_md5'][type='hidden'][value='abcd1234']")
      expect(form).to have_selector("button[type='submit']", :text => "SAVE")
      expect(form).to have_selector("button", :text => "Cancel")
    end
  end

  it "should render the config conflict message" do
    assign(:config_file_conflict, true)

    render

    expect(response.body).to have_selector("#config_save_actions")
  end

  it "should render the required message" do
    render

    expect(response.body).to have_selector(".required .asterisk")
  end
end
