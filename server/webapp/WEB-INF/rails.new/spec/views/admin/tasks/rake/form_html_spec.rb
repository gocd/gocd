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


describe "admin/tasks/rake/new.html.erb" do
  include GoUtil, TaskMother, FormUI

  before :each do
    assign(:cruise_config, config = CruiseConfig.new)
    set(config, "md5", "abcd1234")
    assign(:on_cancel_task_vms, @vms =  java.util.Arrays.asList([vm_for(exec_task('rm')), vm_for(ant_task), vm_for(nant_task), vm_for(rake_task), vm_for(fetch_task)].to_java(TaskViewModel)))
    template.stub(:admin_task_create_path).and_return("task_create_path")
    template.stub(:admin_task_update_path).and_return("task_update_path")
  end

  it "should render a simple rake task for new" do
    rake_task = RakeTask.new
    assign(:task, rake_task)
    assign(:task_view_model, Spring.bean("taskViewService").getViewModel(rake_task, 'new'))

    render "/admin/tasks/plugin/new"
    response.body.should have_tag("form[action=?]", "task_create_path") do
      with_tag("div.fieldset") do
        with_tag("label", "Build file")
        with_tag("input[name='task[buildFile]']")
        with_tag("label", "Target")
        with_tag("input[name='task[target]']")
        with_tag("label", "Working directory")
        with_tag("input[name='task[workingDirectory]']")
      end
    end
  end

    it "should render error messages if errors are present on the config" do
    task = rake_task
    task.addError(com.thoughtworks.go.config.BuildTask::WORKING_DIRECTORY, "working directory error")
    assign(:task, task)
    assign(:task_view_model, Spring.bean("taskViewService").getViewModel(task, 'edit'))

    render "/admin/tasks/plugin/edit"
    response.body.should have_tag("form[action=?]", "task_update_path") do
      with_tag("div.fieldset") do
        with_tag("label", "Build file")
        with_tag("input[name='task[buildFile]'][value=?]", task.getBuildFile())
        with_tag("label", "Target")
        with_tag("input[name='task[target]'][value=?]", task.getTarget())
        with_tag("label", "Working directory")
        with_tag("input[name='task[workingDirectory]'][value=?]", task.workingDirectory())
        with_tag("div.fieldWithErrors input[type='text'][name='task[workingDirectory]']")
        with_tag("div.form_error", "working directory error")
      end
    end
  end

  it "should render a simple rake task for edit" do
    rake_task = RakeTask.new
    rake_task.setBuildFile("build.xml")
    rake_task.setWorkingDirectory("blah/foo-bar")
    rake_task.setTarget("compile test and just deploy")
    assign(:task, rake_task)
    assign(:task_view_model, Spring.bean("taskViewService").getViewModel(rake_task, 'edit'))

    render "/admin/tasks/plugin/edit"
    response.body.should have_tag("form[action=?]", "task_update_path") do
      with_tag("div.fieldset") do
        with_tag("label", "Build file")
        with_tag("input[name='task[buildFile]'][value=?]", rake_task.getBuildFile())
        with_tag("label", "Target")
        with_tag("input[name='task[target]'][value=?]", rake_task.getTarget())
        with_tag("label", "Working directory")
        with_tag("input[name='task[workingDirectory]'][value=?]", rake_task.workingDirectory())
      end
    end
  end
end