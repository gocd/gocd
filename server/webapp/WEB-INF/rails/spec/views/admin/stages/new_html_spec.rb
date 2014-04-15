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

describe "admin/stages new.html.erb" do
  include GoUtil, FormUI

  NEW_STAGE_PAGE = 'admin/stages/new.html.erb'

  before(:each) do
    @new_job = JobConfig.new(CaseInsensitiveString.new("job-name"), Resources.new, ArtifactPlans.new, Tasks.new([@task = ExecTask.new("ls", "-la", "my_work_dir")].to_java(Task)))
    @stage = StageConfig.new(CaseInsensitiveString.new("stage-name"), JobConfigs.new([@new_job].to_java(JobConfig)))

    assigns[:stage] = @stage

    @pipeline_config = PipelineConfigMother.createPipelineConfig("pipeline-name", "foo", ["build-1"].to_java(java.lang.String))
    assigns[:pipeline_config] = @pipeline_config

    assigns[:cruise_config] = @cruise_config = CruiseConfig.new
    @cruise_config.addPipeline("group-1", @pipeline_config)

    set(@cruise_config, "md5", "abc")
    in_params(:stage_parent => "pipelines", :pipeline_name => "foo_bar", :action => "new", :controller => "admin/stages")

    tvms = java.util.ArrayList.new
    tvms.add(com.thoughtworks.go.presentation.TaskViewModel.new(@task, "admin/tasks/exec/new", "erb"))
    assigns[:task_view_models] = tvms
  end

  it "should render new form" do
    render NEW_STAGE_PAGE

    response.body.should have_tag("#new_stage_container form") do
      with_tag("h3", "Stage Information")
      with_tag("input[type='hidden'][name='config_md5'][value='abc']")
      with_tag("input[type='hidden'][name='current_tab'][value=?]", "stages")
      with_tag(".instructions", "You can add more jobs and tasks to this stage once the stage has been created.")

      with_tag("input[type='text'][name='stage[#{StageConfig::NAME}]'][value='stage-name']")

      with_tag("label[for='auto']", "On Success")
      with_tag("input#auto[type='radio'][name='stage[#{StageConfig::APPROVAL}][#{Approval::TYPE}]'][value='#{Approval::SUCCESS}']")
      with_tag("label[for='manual']", "Manual")
      with_tag("input#manual[type='radio'][name='stage[#{StageConfig::APPROVAL}][#{Approval::TYPE}]'][value='#{Approval::MANUAL}']")
      with_tag("span.stage_approval.contextual_help.has_go_tip_right[title=?]", "'On Success' option will automatically schedule the stage after the preceding stage completes successfully. The 'Manual' option will require a user to manually
                                              trigger the stage. For the first stage in a pipeline, setting type to 'on success' is the same as checking 'Automatic Pipeline Scheduling' on the pipeline config.")

      with_tag("input[name='stage[#{StageConfig::JOBS}][][#{JobConfig::NAME}]'][value='job-name']")

      with_tag("select[name='stage[#{StageConfig::JOBS}][][#{JobConfig::TASKS}][#{Tasks::TASK_OPTIONS}]']") do
        with_tag("option[value='exec'][selected]")
        with_tag("option[value='ant']")
        with_tag("option[value='nant']")
        with_tag("option[value='rake']")
      end

      with_tag("input[type='text'][name='stage[#{StageConfig::JOBS}][][#{JobConfig::TASKS}][exec][#{ExecTask::COMMAND}]'][value='ls']")
      with_tag("input[type='text'][name='stage[#{StageConfig::JOBS}][][#{JobConfig::TASKS}][exec][#{ExecTask::ARGS}]'][value='-la']")
      with_tag("input[type='text'][name='stage[#{StageConfig::JOBS}][][#{JobConfig::TASKS}][exec][#{ExecTask::WORKING_DIR}]'][value='my_work_dir']")
    end
    response.body.should_not have_tag(".fieldWithErrors")
    response.body.should_not have_tag(".form_error")
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

    render NEW_STAGE_PAGE

    response.body.should have_tag("#new_stage_container form") do
      with_tag("div.fieldWithErrors input[type='text'][name='stage[#{StageConfig::NAME}]']")
      with_tag("div.form_error", "The name cannot be duplicated")

      with_tag("div.fieldWithErrors input[type='radio'][name='stage[#{StageConfig::APPROVAL}][#{Approval::TYPE}]'][value='#{Approval::SUCCESS}']")
      with_tag("div.fieldWithErrors input[type='radio'][name='stage[#{StageConfig::APPROVAL}][#{Approval::TYPE}]'][value='#{Approval::MANUAL}']")
      with_tag("div.form_error", "Bad approval")

      with_tag("div.fieldWithErrors input[name='stage[#{StageConfig::JOBS}][][#{JobConfig::NAME}]'][value='job-name']")
      with_tag("div.form_error", "Job name is duplicated")

      with_tag("div.fieldWithErrors input[type='text'][name='stage[#{StageConfig::JOBS}][][#{JobConfig::TASKS}][exec][#{ExecTask::COMMAND}]'][value='ls']")
      with_tag("div.form_error", "Does not have a command")
      with_tag("input[type='text'][name='stage[#{StageConfig::JOBS}][][#{JobConfig::TASKS}][exec][#{ExecTask::ARGS}]'][value='abc']")
      with_tag("div.form_error", "what horrible args?")
      with_tag("div.fieldWithErrors input[type='text'][name='stage[#{StageConfig::JOBS}][][#{JobConfig::TASKS}][exec][#{ExecTask::WORKING_DIR}]'][value='my_work_dir']")
      with_tag("div.form_error", "really?")
    end
  end

  it "should render reload option when the config file MD5 has changed under the message" do
    assigns[:config_file_conflict] = true
    render NEW_STAGE_PAGE
    response.body.should have_tag("#config_save_actions button.reload_config#reload_config", "Reload")
    response.body.should have_tag("#config_save_actions label", "This will refresh the page and you will lose your changes on this page.")
  end

  it "should not render reload option when the config file has not conflicted" do
    render NEW_STAGE_PAGE
    response.body.should_not have_tag("#config_save_actions")
  end

  it "should render reload option when the config file MD5 has changed under the message" do
    assigns[:config_file_conflict] = true
    render NEW_STAGE_PAGE
    response.body.should have_tag("#config_save_actions button.reload_config#reload_config", "Reload")
    response.body.should have_tag("#config_save_actions label", "This will refresh the page and you will lose your changes on this page.")
  end

  it "should not render reload option when the config file has not conflicted" do
    render NEW_STAGE_PAGE
    response.body.should_not have_tag("#config_save_actions")
  end

  it "should render reload option when the config file MD5 has changed under the message" do
    assigns[:config_file_conflict] = true
    render NEW_STAGE_PAGE
    response.body.should have_tag("#config_save_actions button.reload_config#reload_config", "Reload")
    response.body.should have_tag("#config_save_actions label", "This will refresh the page and you will lose your changes on this page.")
  end

  it "should not render reload option when the config file has not conflicted" do
    render NEW_STAGE_PAGE
    response.body.should_not have_tag("#config_save_actions")
  end
end
