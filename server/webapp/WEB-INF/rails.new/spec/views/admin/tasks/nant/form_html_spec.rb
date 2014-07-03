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


describe "admin/tasks/nant/new.html.erb" do
  include GoUtil, TaskMother, FormUI
  include Admin::TaskHelper

  before :each do
    assign(:cruise_config, config = CruiseConfig.new)
    assign(:on_cancel_task_vms, @vms =  java.util.Arrays.asList([vm_for(exec_task('rm')), vm_for(ant_task), vm_for(nant_task), vm_for(rake_task), vm_for(fetch_task)].to_java(TaskViewModel)))
    set(config, "md5", "abcd1234")
    template.stub(:admin_task_create_path).and_return("task_create_path")
    template.stub(:admin_task_update_path).and_return("task_update_path")
  end

  it "should render a simple nant task for new" do
    task = NantTask.new
    assigns[:task] = task
    assigns[:task_view_model] = Spring.bean("taskViewService").getViewModel(task, 'new')

    render "/admin/tasks/plugin/new"
    response.body.should have_tag("form[action=?]", "task_create_path") do
      with_tag("div.fieldset") do
        with_tag("label", "Build file")
        with_tag("input[name='task[buildFile]']")
        with_tag("div[class='contextual_help has_go_tip_right build_file'][title=?]", "Relative path to a NAnt build file. If not specified, the path defaults to ‘default.build’.")
        with_tag("label", "Target")
        with_tag("input[name='task[target]']")
        with_tag("div[class='contextual_help has_go_tip_right target'][title=?]", "NAnt target(s) to run. If not specified, defaults to the default target of the build file.")
        with_tag("label", "Working directory")
        with_tag("input[name='task[workingDirectory]']")
        with_tag("div[class='contextual_help has_go_tip_right working_directory'][title=?]", "The directory from where NAnt is invoked.")
        with_tag("label", "Nant path")
        with_tag("input[name='task[nantPath]']")
        with_tag("div[class='contextual_help has_go_tip_right nant_path'][title=?]", "Path of the directory in which NAnt is installed. By default Go will assume that NAnt is in the system path.")
      end
    end
  end

  it "should render a simple nant task for edit" do
    task = nant_task
    task.setNantPath(File.dirname(__FILE__))
    assigns[:task] = task
    assigns[:task_view_model] = Spring.bean("taskViewService").getViewModel(task, 'edit')

    render "/admin/tasks/plugin/edit"
    response.body.should have_tag("form[action=?]", "task_update_path") do
      with_tag("div.fieldset") do
        with_tag("label", "Build file")
        with_tag("input[name='task[buildFile]'][value=?]", task.getBuildFile())
        with_tag("div[class='contextual_help has_go_tip_right build_file'][title=?]", "Relative path to a NAnt build file. If not specified, the path defaults to ‘default.build’.")
        with_tag("label", "Target")
        with_tag("input[name='task[target]'][value=?]", task.getTarget())
        with_tag("div[class='contextual_help has_go_tip_right target'][title=?]", "NAnt target(s) to run. If not specified, defaults to the default target of the build file.")
        with_tag("label", "Working directory")
        with_tag("input[name='task[workingDirectory]'][value=?]", task.workingDirectory())
        with_tag("div[class='contextual_help has_go_tip_right working_directory'][title=?]", "The directory from where NAnt is invoked.")
        with_tag("label", "Nant path")
        with_tag("input[name='task[nantPath]'][value=?]", task.getNantPath())
        with_tag("div[class='contextual_help has_go_tip_right nant_path'][title=?]", "Path of the directory in which NAnt is installed. By default Go will assume that NAnt is in the system path.")
      end
    end
  end

  it "should render error messages if errors are present on the config" do
    task = nant_task
    task.addError(com.thoughtworks.go.config.BuildTask::BUILD_FILE, "build file error")
    task.addError(com.thoughtworks.go.config.BuildTask::WORKING_DIRECTORY, "working directory error")
    task.addError(com.thoughtworks.go.config.BuildTask::TARGET, "target error")
    task.addError(com.thoughtworks.go.config.NantTask::NANT_PATH, "nant path error")
    assign(:task, task)
    assign(:task_view_model, Spring.bean("taskViewService").getViewModel(task, 'new'))

    render "/admin/tasks/plugin/new"
    response.body.should have_tag("form[action=?]", "task_create_path") do
      with_tag("div.fieldset") do
        with_tag("div.fieldWithErrors input[type='text'][name='task[buildFile]']")
        with_tag("div.form_error", "build file error")
        with_tag("div.fieldWithErrors input[type='text'][name='task[target]']")
        with_tag("div.form_error", "target error")
        with_tag("div.fieldWithErrors input[type='text'][name='task[workingDirectory]']")
        with_tag("div.form_error", "working directory error")
        with_tag("div.fieldWithErrors input[type='text'][name='task[nantPath]']")
        with_tag("div.form_error", "nant path error")
      end
    end
  end

  it "should render a simple nant task for edit with on cancel Exec Task" do
    task = nant_task("blah", "blah", "blah")
    task.setNantPath(File.dirname(__FILE__))
    oncancel = nant_task
    oncancel.setNantPath(File.dirname(__FILE__))
    task.setCancelTask(oncancel)

    assigns[:on_cancel_task_vms] = @vms =  java.util.Arrays.asList([vm_for(exec_task('rm')), vm_for(ant_task), vm_for(task.cancelTask()), vm_for(rake_task), vm_for(fetch_task)].to_java(TaskViewModel))
    assigns[:task] = task
    assigns[:task_view_model] = Spring.bean("taskViewService").getViewModel(task, 'edit')

    render "/admin/tasks/plugin/edit"
    response.body.should have_tag("form[action=?]", "task_update_path") do
      with_tag("option[selected='selected'][value=?]", "nant")
      with_tag("label", "Build file")
      with_tag("input[name='task[onCancelConfig][nantOnCancel][buildFile]'][value=?]", oncancel.getBuildFile())
      with_tag("div[class='contextual_help has_go_tip_right build_file'][title=?]", "Relative path to a NAnt build file. If not specified, the path defaults to ‘default.build’.")
      with_tag("label", "Target")
      with_tag("input[name='task[onCancelConfig][nantOnCancel][target]'][value=?]", oncancel.getTarget())
      with_tag("div[class='contextual_help has_go_tip_right target'][title=?]", "NAnt target(s) to run. If not specified, defaults to the default target of the build file.")
      with_tag("label", "Working directory")
      with_tag("input[name='task[onCancelConfig][nantOnCancel][workingDirectory]'][value=?]", oncancel.workingDirectory())
      with_tag("div[class='contextual_help has_go_tip_right working_directory'][title=?]", "The directory from where NAnt is invoked.")
      with_tag("label", "Nant path")
      with_tag("input[name='task[onCancelConfig][nantOnCancel][nantPath]'][value=?]", java.io.File.new(File.dirname(__FILE__)).to_s)
      with_tag("div[class='contextual_help has_go_tip_right nant_path'][title=?]", "Path of the directory in which NAnt is installed. By default Go will assume that NAnt is in the system path.")
    end
  end
end