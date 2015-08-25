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

describe "admin/jobs/new.html.erb" do
  include GoUtil, FormUI
  include Admin::AdminHelper
  include Admin::ConfigContextHelper
  include MockRegistryModule

  before :each do
    view.stub(:url_for).and_return("url_for_new_job")
    assign(:cruise_config, @cruise_config = BasicCruiseConfig.new)
    @cruise_config.addPipeline("group-1", @pipeline)
    set(@cruise_config, "md5", "abc")
    in_params(:pipeline_name => "pipeline-name", :action => "index", :controller => "admin/jobs", :stage_name => "stage-name")
    tvms = java.util.ArrayList.new
    tvms.add(com.thoughtworks.go.presentation.TaskViewModel.new(com.thoughtworks.go.config.AntTask.new, "template", "erb"))
    assign(:task_view_models, tvms)
    assign(:config_context, create_config_context(MockRegistryModule::MockRegistry.new))

    view.stub(:render_pluggable_form_template).and_return("template")
  end

  it "should render form with name and id for angular binding" do
    assign(:job, JobConfig.new)

    render

    expect(response.body).to have_selector("form[name='pipeline_edit_form'][id='pipeline_edit_form']")
  end

  it "should render job name and hidden current tab field" do
    assign(:job, JobConfig.new)

    render

    Capybara.string(response.body).find("form[method='post'][action='url_for_new_job']").tap do |form|
      expect(form).to have_selector("input[type='hidden'][name='current_tab'][value='jobs']")
    end

    Capybara.string(response.body).all("#new_job_container .form_item_block").tap do |blocks|
      expect(blocks[0]).to have_selector("label", :text => "Job Name*")
      expect(blocks[0]).to have_selector("input[type='text'][name='job[#{com.thoughtworks.go.config.JobConfig::NAME}]']")

      expect(blocks[0]).not_to have_selector("input[type='text'][name='job[#{com.thoughtworks.go.config.JobConfig::NAME}]'][readonly='readonly']")
    end
  end

  it "should render textbox for timeout" do
    @cruise_config.server().setJobTimeout("42")
    assign(:job, JobConfig.new)

    render

    Capybara.string(response.body).all("#new_job_container .form_item").tap do |blocks|
      expect(blocks[0]).to have_selector("label", :text => "Use default (42 minute(s))")
      expect(blocks[0]).to have_selector("input[type='radio'][name='job[timeoutType]'][value='defaultTimeout'][checked='checked']")
      expect(blocks[0]).to have_selector("label", :text => "Cancel after")
      expect(blocks[0]).to have_selector("label", :text => "minute(s) of inactivity")
      expect(blocks[0]).to have_selector("input[type='radio'][name='job[timeoutType]'][value='overrideTimeout']")
      expect(blocks[0]).to have_selector("label", :text => "Never")
      expect(blocks[0]).to have_selector("input[type='radio'][name='job[timeoutType]'][value='neverTimeout']")
      expect(blocks[0]).to have_selector("label", :text => "Job Timeout")
      expect(blocks[0]).to have_selector("input[type='text'][name='job[#{com.thoughtworks.go.config.JobConfig::TIMEOUT}]']")
      expect(blocks[0]).to have_selector("div.contextual_help[title='If this job is inactive for more than the specified period (in minutes), Go will cancel it.']")
    end
  end

  it "should render text 'Never' beside default radio button" do
    @cruise_config.server().setJobTimeout("0")
    assign(:job, JobConfig.new)

    render

    Capybara.string(response.body).all("#new_job_container .form_item").tap do |blocks|
      expect(blocks[0]).to have_selector("label", :text => "Use default (Never)")
      expect(blocks[0]).to have_selector("input[type='radio'][name='job[timeoutType]'][value='defaultTimeout']")
    end
  end

  it "should render job resources" do
    job_config = JobConfig.new
    job_config.setRunInstanceCount(2)
    assign(:job, job_config)

    render

    Capybara.string(response.body).all("#new_job_container .form_item_block").tap do |blocks|
      expect(blocks[1]).to have_selector("label", :text => "Resources")
      expect(blocks[1]).to have_selector("input[type='text'][class='resources_auto_complete'][name='job[#{com.thoughtworks.go.config.JobConfig::RESOURCES}]']")
    end
  end

  it "should render run on all agents checkbox, run multiple instance" do
    job_config = JobConfig.new
    job_config.setRunInstanceCount(10)
    assign(:job, job_config)

    render

    Capybara.string(response.body).all("#new_job_container .form_item").tap do |blocks|
      expect(blocks[2]).to have_selector("label", :text => "Run Type")

      expect(blocks[2]).to have_selector("label", :text => "Run one instance")
      expect(blocks[2]).to have_selector("input[type='radio'][name='job[runType]'][value='runSingleInstance']")

      expect(blocks[2]).to have_selector("label", :text => "Run on all agents")
      expect(blocks[2]).to have_selector("input[type='radio'][name='job[runType]'][value='runOnAllAgents']")

      expect(blocks[2]).to have_selector("input[type='radio'][name='job[runType]'][value='runMultipleInstance'][checked='checked']")
      expect(blocks[2]).to have_selector("label", :text => "Run")
      expect(blocks[2]).to have_selector("input[type='text'][name='job[#{com.thoughtworks.go.config.JobConfig::RUN_INSTANCE_COUNT}]'][value='10']")
      expect(blocks[2]).to have_selector("label", :text => "instances")
    end
  end

  it "should render errors on the job name" do
    assign(:job, job = JobConfig.new)
    set(job, "errors", config_error(JobConfig::NAME, "Name cannot be duplicated"))

    render

    Capybara.string(response.body).all("#new_job_container .form_item_block").tap do |blocks|
      expect(blocks[0]).to have_selector("div.field_with_errors input[type='text'][name='job[#{JobConfig::NAME}]']")
      expect(blocks[0]).to have_selector("div.form_error", :text => "Name cannot be duplicated")
    end
  end

  it "should render job tasks" do
    assign(:job, JobConfig.new(CaseInsensitiveString.new(""), Resources.new, ArtifactPlans.new, com.thoughtworks.go.config.Tasks.new([ExecTask.new].to_java(com.thoughtworks.go.domain.Task))))

    render

    response.should render_template(:partial => 'admin/shared/_job_tasks.html')
  end

  it "should render job task instructions" do
    assign(:job, JobConfig.new(CaseInsensitiveString.new(""), Resources.new, ArtifactPlans.new, com.thoughtworks.go.config.Tasks.new([ExecTask.new].to_java(com.thoughtworks.go.domain.Task))))

    render

    Capybara.string(response.body).find("#new_job_container").tap do |new_job_container|
      expect(new_job_container).to have_selector("div.instructions", :text => "This job requires at least one task. You can add more tasks once this job has been created")
    end
  end
end
