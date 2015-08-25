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

describe "admin/stages/new.html.erb" do
  include GoUtil, FormUI

  before(:each) do
    @new_job = JobConfig.new(CaseInsensitiveString.new("job-name"), Resources.new, ArtifactPlans.new, com.thoughtworks.go.config.Tasks.new([@task = ExecTask.new("ls", "-la", "my_work_dir")].to_java(Task)))
    @stage = StageConfig.new(CaseInsensitiveString.new("stage-name"), JobConfigs.new([@new_job].to_java(JobConfig)))

    assign(:stage, @stage)

    @pipeline_config = PipelineConfigMother.createPipelineConfig("pipeline-name", "foo", ["build-1"].to_java(java.lang.String))
    assign(:pipeline_config, @pipeline_config)

    assign(:cruise_config, @cruise_config = BasicCruiseConfig.new)
    @cruise_config.addPipeline("group-1", @pipeline_config)

    set(@cruise_config, "md5", "abc")
    in_params(:stage_parent => "pipelines", :pipeline_name => "foo_bar", :action => "new", :controller => "admin/stages")

    tvms = java.util.ArrayList.new
    tvms.add(com.thoughtworks.go.presentation.TaskViewModel.new(@task, "admin/tasks/exec/new", "erb"))
    assign(:task_view_models, tvms)
  end

  it "should render form with name and id for angular binding" do
    render

    expect(response.body).to have_selector("form[name='pipeline_edit_form'][id='pipeline_edit_form']")
  end

  it "should render new form" do
    render

    Capybara.string(response.body).find('#new_stage_container form').tap do |form|
      expect(form).to have_selector("h3", :text => "Stage Information")
      expect(form).to have_selector("input[type='hidden'][name='config_md5'][value='abc']")
      expect(form).to have_selector("input[type='hidden'][name='current_tab'][value='stages']")
      expect(form).to have_selector(".instructions", :text => "You can add more jobs and tasks to this stage once the stage has been created.")

      expect(form).to have_selector("input[type='text'][name='stage[#{StageConfig::NAME}]'][value='stage-name']")

      expect(form).to have_selector("label[for='auto']", :text => "On Success")
      expect(form).to have_selector("input#auto[type='radio'][name='stage[#{StageConfig::APPROVAL}][#{Approval::TYPE}]'][value='#{Approval::SUCCESS}']")
      expect(form).to have_selector("label[for='manual']", :text => "Manual")
      expect(form).to have_selector("input#manual[type='radio'][name='stage[#{StageConfig::APPROVAL}][#{Approval::TYPE}]'][value='#{Approval::MANUAL}']")
      expect(form.find("span.stage_approval.contextual_help.has_go_tip_right")['title']).to eq("'On Success' option will automatically schedule the stage after the preceding stage completes successfully. The 'Manual' option will require a user to manually trigger the stage. For the first stage in a pipeline, setting type to 'on success' is the same as checking 'Automatic Pipeline Scheduling' on the pipeline config.")

      expect(form).to have_selector("input[name='stage[#{StageConfig::JOBS}][][#{JobConfig::NAME}]'][value='job-name']")

      form.find("select[name='stage[#{StageConfig::JOBS}][][#{JobConfig::TASKS}][#{com.thoughtworks.go.config.Tasks::TASK_OPTIONS}]']") do |select|
        expect(select).to have_selector("option[value='exec'][selected]")
        expect(select).to have_selector("option[value='ant']")
        expect(select).to have_selector("option[value='nant']")
        expect(select).to have_selector("option[value='rake']")
      end

      expect(form).to have_selector("input[type='text'][name='stage[#{StageConfig::JOBS}][][#{JobConfig::TASKS}][exec][#{ExecTask::COMMAND}]'][value='ls']")
      expect(form).to have_selector("input[type='text'][name='stage[#{StageConfig::JOBS}][][#{JobConfig::TASKS}][exec][#{ExecTask::ARGS}]'][value='-la']")
      expect(form).to have_selector("input[type='text'][name='stage[#{StageConfig::JOBS}][][#{JobConfig::TASKS}][exec][#{ExecTask::WORKING_DIR}]'][value='my_work_dir']")
    end

    expect(response.body).not_to have_selector(".field_with_errors")
    expect(response.body).not_to have_selector(".form_error")
  end

  it "should render errors" do
    set(@stage.getApproval(), com.thoughtworks.go.config.Approval::TYPE, "foo_bar_approval")

    set(@stage, "errors", config_error(StageConfig::NAME, "The name cannot be duplicated"))
    set(@stage.getApproval(), "errors", config_error(Approval::TYPE, "Bad approval"))
    job = @stage.getJobs().get(0)
    set(job, "errors", config_error(JobConfig::NAME, "Job name is duplicated"))
    error = config_error(ExecTask::COMMAND, "Does not have a command")
    error.add(ExecTask::WORKING_DIR, "really?")
    error.add(ExecTask::ARGS, "what horrible args?")
    task = job.getTasks().get(0)
    set(task, "errors", error)
    set(task, "args", "abc")

    render

    Capybara.string(response.body).find('#new_stage_container form').tap do |form|
      expect(form).to have_selector("div.field_with_errors input[type='text'][name='stage[#{StageConfig::NAME}]']")
      expect(form).to have_selector("div.form_error", :text => "The name cannot be duplicated")

      expect(form).to have_selector("div.field_with_errors input[type='radio'][name='stage[#{StageConfig::APPROVAL}][#{Approval::TYPE}]'][value='#{Approval::SUCCESS}']")
      expect(form).to have_selector("div.field_with_errors input[type='radio'][name='stage[#{StageConfig::APPROVAL}][#{Approval::TYPE}]'][value='#{Approval::MANUAL}']")
      expect(form).to have_selector("div.form_error", :text => "Bad approval")

      expect(form).to have_selector("div.field_with_errors input[name='stage[#{StageConfig::JOBS}][][#{JobConfig::NAME}]'][value='job-name']")
      expect(form).to have_selector("div.form_error", :text => "Job name is duplicated")

      expect(form).to have_selector("div.field_with_errors input[type='text'][name='stage[#{StageConfig::JOBS}][][#{JobConfig::TASKS}][exec][#{ExecTask::COMMAND}]'][value='ls']")
      expect(form).to have_selector("div.form_error", :text => "Does not have a command")
      expect(form).to have_selector("input[type='text'][name='stage[#{StageConfig::JOBS}][][#{JobConfig::TASKS}][exec][#{ExecTask::ARGS}]'][value='abc']")
      expect(form).to have_selector("div.form_error", :text => "what horrible args?")
      expect(form).to have_selector("div.field_with_errors input[type='text'][name='stage[#{StageConfig::JOBS}][][#{JobConfig::TASKS}][exec][#{ExecTask::WORKING_DIR}]'][value='my_work_dir']")
      expect(form).to have_selector("div.form_error", :text => "really?")
    end
  end

  it "should render reload option when the config file MD5 has changed under the message" do
    assign(:config_file_conflict, true)

    render

    expect(response.body).to have_selector("#config_save_actions button.reload_config#reload_config", :text => "Reload")
    expect(response.body).to have_selector("#config_save_actions label", :text => "This will refresh the page and you will lose your changes on this page.")
  end

  it "should not render reload option when the config file has not conflicted" do
    render

    expect(response.body).not_to have_selector("#config_save_actions")
  end

  it "should render reload option when the config file MD5 has changed under the message" do
    assign(:config_file_conflict, true)

    render

    expect(response.body).to have_selector("#config_save_actions button.reload_config#reload_config", :text => "Reload")
    expect(response.body).to have_selector("#config_save_actions label", :text => "This will refresh the page and you will lose your changes on this page.")
  end

  it "should not render reload option when the config file has not conflicted" do
    render

    expect(response.body).not_to have_selector("#config_save_actions")
  end

  it "should render reload option when the config file MD5 has changed under the message" do
    assign(:config_file_conflict, true)

    render

    expect(response.body).to have_selector("#config_save_actions button.reload_config#reload_config", :text => "Reload")
    expect(response.body).to have_selector("#config_save_actions label", :text => "This will refresh the page and you will lose your changes on this page.")
  end

  it "should not render reload option when the config file has not conflicted" do
    render

    expect(response.body).not_to have_selector("#config_save_actions")
  end
end
