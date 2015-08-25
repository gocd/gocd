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


describe "admin/tasks/nant/new.html.erb" do
  include GoUtil, TaskMother, FormUI
  include Admin::TaskHelper

  before :each do
    assign(:cruise_config, config = BasicCruiseConfig.new)
    assign(:on_cancel_task_vms, @vms =  java.util.Arrays.asList([vm_for(exec_task('rm')), vm_for(ant_task), vm_for(nant_task), vm_for(rake_task), vm_for(fetch_task)].to_java(TaskViewModel)))
    set(config, "md5", "abcd1234")
    view.stub(:admin_task_create_path).and_return("task_create_path")
    view.stub(:admin_task_update_path).and_return("task_update_path")
  end

  it "should render a simple nant task for new" do
    task = NantTask.new
    assign(:task, task)
    assign(:task_view_model, Spring.bean("taskViewService").getViewModel(task, 'new'))

    render :template => "admin/tasks/plugin/new.html.erb"

    Capybara.string(response.body).find("form[action='task_create_path']").tap do |form|
      form.all("div.fieldset") do |divs|
        expect(divs[0]).to have_selector("label", :text => "Build file")
        expect(divs[0]).to have_selector("input[name='task[buildFile]']")
        expect(divs[0]).to have_selector("div[class='contextual_help has_go_tip_right build_file'][title='Relative path to a NAnt build file. If not specified, the path defaults to &#39;default.build'.&#39;]")
        expect(divs[0]).to have_selector("label", :text => "Target")
        expect(divs[0]).to have_selector("input[name='task[target]']")
        expect(divs[0]).to have_selector("div[class='contextual_help has_go_tip_right target'][title='NAnt target(s) to run. If not specified, defaults to the default target of the build file.']")
        expect(divs[0]).to have_selector("label", :text => "Working directory")
        expect(divs[0]).to have_selector("input[name='task[workingDirectory]']")
        expect(divs[0]).to have_selector("div[class='contextual_help has_go_tip_right working_directory'][title='The directory from where NAnt is invoked.']")
        expect(divs[0]).to have_selector("label", :text => "Nant path")
        expect(divs[0]).to have_selector("input[name='task[nantPath]']")
        expect(divs[0]).to have_selector("div[class='contextual_help has_go_tip_right nant_path'][title='Path of the directory in which NAnt is installed. By default Go will assume that NAnt is in the system path.']")
      end
    end
  end

  it "should render a simple nant task for edit" do
    task = nant_task
    task.setNantPath(File.dirname(__FILE__))
    assign(:task, task)
    assign(:task_view_model, Spring.bean("taskViewService").getViewModel(task, 'edit'))

    render :template => "admin/tasks/plugin/edit.html.erb"

    Capybara.string(response.body).find("form[action='task_update_path']").tap do |form|
      form.all("div.fieldset") do |divs|
        expect(divs[0]).to have_selector("label", :text => "Build file")
        expect(divs[0]).to have_selector("input[name='task[buildFile]'][value='#{task.getBuildFile()}']")
        expect(divs[0]).to have_selector("div[class='contextual_help has_go_tip_right build_file'][title='Relative path to a NAnt build file. If not specified, the path defaults to &#39;default.build&#39;.']")
        expect(divs[0]).to have_selector("label", :text => "Target")
        expect(divs[0]).to have_selector("input[name='task[target]'][value='#{task.getTarget()}']")
        expect(divs[0]).to have_selector("div[class='contextual_help has_go_tip_right target'][title='NAnt target(s) to run. If not specified, defaults to the default target of the build file.']")
        expect(divs[0]).to have_selector("label", :text => "Working directory")
        expect(divs[0]).to have_selector("input[name='task[workingDirectory]'][value='#{task.workingDirectory()}']")
        expect(divs[0]).to have_selector("div[class='contextual_help has_go_tip_right working_directory'][title='The directory from where NAnt is invoked.']")
        expect(divs[0]).to have_selector("label", :text => "Nant path")
        expect(divs[0]).to have_selector("input[name='task[nantPath]'][value='#{task.getNantPath()}']")
        expect(divs[0]).to have_selector("div[class='contextual_help has_go_tip_right nant_path'][title='Path of the directory in which NAnt is installed. By default Go will assume that NAnt is in the system path.']")
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

    render :template => "admin/tasks/plugin/new.html.erb"

    Capybara.string(response.body).find("form[action='task_create_path']").tap do |form|
      form.all("div.fieldset") do |divs|
        expect(divs[0]).to have_selector("div.field_with_errors input[type='text'][name='task[buildFile]']")
        expect(divs[0]).to have_selector("div.form_error", :text => "build file error")
        expect(divs[0]).to have_selector("div.field_with_errors input[type='text'][name='task[target]']")
        expect(divs[0]).to have_selector("div.form_error", :text => "target error")
        expect(divs[0]).to have_selector("div.field_with_errors input[type='text'][name='task[workingDirectory]']")
        expect(divs[0]).to have_selector("div.form_error", :text => "working directory error")
        expect(divs[0]).to have_selector("div.field_with_errors input[type='text'][name='task[nantPath]']")
        expect(divs[0]).to have_selector("div.form_error", :text => "nant path error")
      end
    end
  end

  it "should render a simple nant task for edit with on cancel Exec Task" do
    task = nant_task("blah", "blah", "blah")
    task.setNantPath(File.dirname(__FILE__))
    oncancel = nant_task
    oncancel.setNantPath(File.dirname(__FILE__))
    task.setCancelTask(oncancel)

    assign(:on_cancel_task_vms, @vms =  java.util.Arrays.asList([vm_for(exec_task('rm')), vm_for(ant_task), vm_for(task.cancelTask()), vm_for(rake_task), vm_for(fetch_task)].to_java(TaskViewModel)))
    assign(:task, task)
    assign(:task_view_model, Spring.bean("taskViewService").getViewModel(task, 'edit'))

    render :template => "admin/tasks/plugin/edit.html.erb"

    Capybara.string(response.body).find("form[action='task_update_path']").tap do |form|
      expect(form).to have_selector("option[selected='selected'][value='nant']")
      expect(form).to have_selector("label", :text => "Build file")
      expect(form).to have_selector("input[name='task[onCancelConfig][nantOnCancel][buildFile]'][value='#{oncancel.getBuildFile()}']")
      expect(form.all("div[class='contextual_help has_go_tip_right build_file']")[0]['title']).to eq("Relative path to a NAnt build file. If not specified, the path defaults to 'default.build'.")
      expect(form).to have_selector("label", :text => "Target")
      expect(form).to have_selector("input[name='task[onCancelConfig][nantOnCancel][target]'][value='#{oncancel.getTarget()}']")
      expect(form.all("div[class='contextual_help has_go_tip_right target']")[0]['title']).to eq("NAnt target(s) to run. If not specified, defaults to the default target of the build file.")
      expect(form).to have_selector("label", :text => "Working directory")
      expect(form).to have_selector("input[name='task[onCancelConfig][nantOnCancel][workingDirectory]'][value='#{oncancel.workingDirectory()}']")
      expect(form.all("div[class='contextual_help has_go_tip_right working_directory']")[0]['title']).to eq("The directory from where NAnt is invoked.")
      expect(form).to have_selector("label", :text => "Nant path")
      expect(form).to have_selector("input[name='task[onCancelConfig][nantOnCancel][nantPath]'][value='#{java.io.File.new(File.dirname(__FILE__)).to_s}']")
      expect(form.all("div[class='contextual_help has_go_tip_right nant_path']")[0]['title']).to eq("Path of the directory in which NAnt is installed. By default Go will assume that NAnt is in the system path.")
    end
  end
end
