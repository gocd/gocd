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

describe "/admin/tasks/plugin/edit.html.erb" do
  include GoUtil, TaskMother, FormUI

  before :each do
    assign(:cruise_config, config = CruiseConfig.new)
    set(config, "md5", "abcd1234")

    template.stub(:admin_task_update_path).and_return("task_update_path")
    assign(:task, @task = simple_exec_task)
    assign(:task_view_model, @tvm = vm_for(@task))
    assign(:on_cancel_task_vms, @vms =  java.util.Arrays.asList([vm_for(exec_task('rm')), vm_for(ant_task), vm_for(nant_task), vm_for(rake_task), vm_for(fetch_task)].to_java(TaskViewModel)))
  end

  it "should render what the rendering service returns" do
    render "admin/tasks/plugin/edit.html.erb"

    response.body.should have_tag("form[action=?][method='post']", 'task_update_path') do
        with_tag("input[type='hidden'][name='_method'][value='PUT']")
        with_tag("label", "Command*")
        with_tag("input[name='task[#{com.thoughtworks.go.config.ExecTask::COMMAND}]'][value='ls']")
    end
  end

  it "should render the config md5, form buttons and flash message" do
    render "admin/tasks/plugin/edit.html.erb"

    response.body.should have_tag("#message_pane")

    response.body.should have_tag("form") do
        with_tag("input[id='config_md5'][type='hidden'][value='abcd1234']")
        with_tag("button[type='submit']", "SAVE")
        with_tag("button", "Cancel")
    end
  end

  it "should render the config conflict message" do
    assign(:config_file_conflict, true)

    render "admin/tasks/plugin/edit.html.erb"

    response.body.should have_tag("#config_save_actions")
  end

  it "should render the required message" do
    render "admin/tasks/plugin/edit.html.erb"

    response.body.should have_tag(".required .asterisk")
  end
end