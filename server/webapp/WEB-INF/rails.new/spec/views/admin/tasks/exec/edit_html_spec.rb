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

require File.join(File.dirname(__FILE__), "../../../../spec_helper")

describe "admin/tasks/edit_exec.html.erb" do
  include GoUtil, TaskMother, FormUI

  before :each do
    assign(:cruise_config, config = CruiseConfig.new)
    set(config, "md5", "abcd1234")

    assign(:on_cancel_task_vms, @vms =  java.util.Arrays.asList([vm_for(exec_task('rm')), vm_for(ant_task), vm_for(nant_task), vm_for(rake_task), vm_for(fetch_task)].to_java(TaskViewModel)))
    template.stub(:admin_task_update_path).and_return("task_edit_path")
  end

  it "should render a simple exec task for edit" do
    task = assign(:task, simple_exec_task)

    assign(:task_view_model, Spring.bean("taskViewService").getViewModel(task, 'edit'))

    render "/admin/tasks/plugin/edit"

    response.body.should have_tag("form") do
      with_tag("div.fieldset" ) do
        with_tag("label", "Command*")
        with_tag("input[name='task[command]']")
        with_tag("label", "Arguments")
        with_tag("input[type='text'][name='task[args]']")
        with_tag("label", "Working Directory")
        with_tag("input[name='task[workingDirectory]']")
      end
    end
  end

  it "should render an exec task with args list" do
    task = assign(:task, simple_exec_task_with_args_list)

    assign(:task_view_model, Spring.bean("taskViewService").getViewModel(task, 'edit'))

    render "/admin/tasks/plugin/edit"

    response.body.should have_tag("form") do
      with_tag("div.fieldset" ) do
        with_tag("label", "Arguments")
        with_tag("textarea[name='task[argListString]']", "-l\n-a")
      end
    end
  end

  it "should render an on cancel exec task" do
    task = assign(:task, with_run_if(RunIfConfig::FAILED, exec_task))
    assign(:on_cancel_task_vms, @vms =  java.util.Arrays.asList([vm_for(task.cancelTask), vm_for(ant_task), vm_for(nant_task), vm_for(rake_task), vm_for(fetch_task)].to_java(TaskViewModel)))
    assign(:task_view_model, Spring.bean("taskViewService").getViewModel(task, 'edit'))

    render "/admin/tasks/plugin/edit"

    response.body.should have_tag("form") do
      with_tag("h3", "Advanced Options")
      with_tag(".on_cancel" ) do
        with_tag("select[class='on_cancel_type'][name='task[#{com.thoughtworks.go.config.AbstractTask::ON_CANCEL_CONFIG}][#{com.thoughtworks.go.config.OnCancelConfig::ON_CANCEL_OPTIONS}]']") do
          with_tag("option", "More...")
          with_tag("option", "Rake")
          with_tag("option", "NAnt")
          with_tag("option", "Ant")
        end

        #All the exec attributes
        with_tag("label", "Command*")
        with_tag("input[name='task[#{com.thoughtworks.go.config.AbstractTask::ON_CANCEL_CONFIG}][#{com.thoughtworks.go.config.OnCancelConfig::EXEC_ON_CANCEL}][command]'][value='echo']")
        with_tag("label", "Arguments")
        with_tag("input[type='text'][name='task[#{com.thoughtworks.go.config.AbstractTask::ON_CANCEL_CONFIG}][#{com.thoughtworks.go.config.OnCancelConfig::EXEC_ON_CANCEL}][args]'][value=?]", "'failing'")
        with_tag("label", "Working Directory")
        with_tag("input[name='task[#{com.thoughtworks.go.config.AbstractTask::ON_CANCEL_CONFIG}][#{com.thoughtworks.go.config.OnCancelConfig::EXEC_ON_CANCEL}][workingDirectory]'][value='oncancel_working_dir']")
        end
    end
  end

  it "should not render on cancel task when not configured" do
    task = assign(:task, simple_exec_task)

    assign(:task_view_model, Spring.bean("taskViewService").getViewModel(task, 'edit'))

    render "/admin/tasks/plugin/edit"

    response.body.should have_tag("form") do
      with_tag("h3", "Advanced Options")
      with_tag(".on_cancel") do
        with_tag("select.on_cancel_type")
        with_tag("input[type='checkbox'][name='task[hasCancelTask]']")
        without_tag("input[type='checkbox'][name='task[hasCancelTask]'][checked='checked']")
      end
    end
  end

  it "should not render exec on cancel task when its not exec" do
    simple_task = simple_exec_task
    simple_task.setCancelTask(rake_task)
    assign(:task, simple_task)

    assign(:task_view_model, Spring.bean("taskViewService").getViewModel(simple_task, 'edit'))

    render "/admin/tasks/plugin/edit"

    response.body.should have_tag("form") do
      with_tag(".on_cancel") do
        with_tag("input[type='checkbox'][name='task[hasCancelTask]'][checked='checked'][value='1']")
        with_tag("select")
        with_tag(".on_cancel_task .exec.hidden")
      end
    end
  end

  it "should not render on cancel task form when there is no 'on cancel task' specified" do
    simple_task = simple_exec_task
    assign(:task, simple_task)

    assign(:task_view_model, Spring.bean("taskViewService").getViewModel(simple_task, 'edit'))

    render "/admin/tasks/plugin/edit"

    response.body.should have_tag("form") do
      with_tag(".on_cancel") do
        with_tag(".on_cancel_task.hidden")
      end
    end
  end

  it "should hide all 'on cancel task' on load" do
    simple_task = simple_exec_task
    assign(:task, simple_task)

    assign(:task_view_model, Spring.bean("taskViewService").getViewModel(simple_task, 'edit'))

    render "/admin/tasks/plugin/edit"

    response.body.should have_tag("form") do
      with_tag(".on_cancel") do
        with_tag(".exec.hidden")
        with_tag(".ant.hidden")
        with_tag(".nant.hidden")
        with_tag(".rake.hidden")
      end
    end
  end

  it "should render an exec task with runif for edit" do
    task = assign(:task, with_run_if(RunIfConfig::FAILED, simple_exec_task))

    assign(:task_view_model, Spring.bean("taskViewService").getViewModel(task, 'edit'))

    render "/admin/tasks/plugin/edit"

    response.body.should have_tag("form") do
      with_tag("label", "Run if conditions*")
      id = ''
      with_tag("div.form_item_block" ) do
        with_tag("label[for=?]", /runif_passed_[a-f0-9-]{36}/) do |tags|
          label = tags[0]
          label.children[0].to_s.should == "Passed"
          id = label.attributes['for']
        end
        with_tag("input[type='checkbox'][name='task[#{com.thoughtworks.go.config.ExecTask::RUN_IF_CONFIGS_PASSED}]'][id='#{id}']")

        with_tag("label[for=?]", /runif_failed_[a-f0-9-]{36}/) do |tags|
          label = tags[0]
          label.children[0].to_s.should == "Failed"
          id = label.attributes['for']
        end
        with_tag("input[type='checkbox'][name='task[#{com.thoughtworks.go.config.ExecTask::RUN_IF_CONFIGS_FAILED}]'][id='#{id}']")

        with_tag("label[for=?]", /runif_any_[a-f0-9-]{36}/) do |tags|
          label = tags[0]
          label.children[0].to_s.should == "Any"
          id = label.attributes['for']
        end
        with_tag("input[type='checkbox'][name='task[#{com.thoughtworks.go.config.ExecTask::RUN_IF_CONFIGS_ANY}]'][id='#{id}']")

      end
    end
  end
end