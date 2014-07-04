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

describe "admin/jobs/new.html.erb" do
  include GoUtil, FormUI
  include Admin::AdminHelper
  include Admin::ConfigContextHelper
  include MockRegistryModule

  before :each do
    template.stub(:url_for).and_return("url_for_new_job")
    assign(:cruise_config, @cruise_config = CruiseConfig.new)
    @cruise_config.addPipeline("group-1", @pipeline)
    set(@cruise_config, "md5", "abc")
    in_params(:pipeline_name => "pipeline-name", :action => "index", :controller => "admin/jobs", :stage_name => "stage-name")
    tvms = java.util.ArrayList.new
    tvms.add(com.thoughtworks.go.presentation.TaskViewModel.new(com.thoughtworks.go.config.AntTask.new, "template", "erb"))
    assign(:task_view_models, tvms)
    assign(:config_context, create_config_context(MockRegistryModule::MockRegistry.new))

    template.stub(:render_pluggable_form_template).and_return("template")
  end

  it "should render form with name and id for angular binding" do
    assign(:job, JobConfig.new)

    render

    response.body.should have_tag("form[name='pipeline_edit_form'][id='pipeline_edit_form']")
  end

  it "should render job name and hidden current tab field" do
    assign(:job, JobConfig.new)

    render

    response.body.should have_tag("form[method='post'][action='url_for_new_job']") do
      with_tag("input[type='hidden'][name='current_tab'][value=?]", "jobs")
    end

    response.body.should have_tag("#new_job_container .form_item_block") do
      with_tag("label", "Job Name*")
      with_tag("input[type='text'][name='job[#{com.thoughtworks.go.config.JobConfig::NAME}]']")
      without_tag("input[type='text'][name='job[#{com.thoughtworks.go.config.JobConfig::NAME}]'][readonly='readonly']")
    end
  end

  it "should render textbox for timeout" do
    @cruise_config.server().setJobTimeout("42")
    assign(:job, JobConfig.new)

    render

    response.body.should have_tag("#new_job_container .form_item") do
      with_tag("label", "Use default (42 minute(s))")
      with_tag("input[type='radio'][name='job[timeoutType]'][value='defaultTimeout'][checked='checked']")
      with_tag("label", "Cancel after")
      with_tag("label", "minute(s) of inactivity")
      with_tag("input[type='radio'][name='job[timeoutType]'][value='overrideTimeout']")
      with_tag("label", "Never")
      with_tag("input[type='radio'][name='job[timeoutType]'][value='neverTimeout']")
      with_tag("label", "Job Timeout")
      with_tag("input[type='text'][name='job[#{com.thoughtworks.go.config.JobConfig::TIMEOUT}]']")
      with_tag("div.contextual_help[title=?]", "If this job is inactive for more than the specified period (in minutes), Go will cancel it.")
    end
  end

  it "should render text 'Never' beside default radio button" do
    @cruise_config.server().setJobTimeout("0")
    assign(:job, JobConfig.new)

    render

    response.body.should have_tag("#new_job_container .form_item") do
      with_tag("label", "Use default (Never)")
      with_tag("input[type='radio'][name='job[timeoutType]'][value='defaultTimeout']")
    end
  end

  it "should render job resources and run on all agents checkbox" do
    assign(:job, JobConfig.new)

    render

    response.body.should have_tag("#new_job_container .form_item_block") do
      with_tag("label", "Resources")
      with_tag("input[type='text'][class='resources_auto_complete'][name='job[#{com.thoughtworks.go.config.JobConfig::RESOURCES}]']")

      with_tag("label", "Run on all agents")
      with_tag("input[type='checkbox'][name='job[#{com.thoughtworks.go.config.JobConfig::RUN_ON_ALL_AGENTS}]']")
    end
  end

  it "should render errors on the job name" do
    assign(:job, job = JobConfig.new)
    set(job, "errors", config_error(JobConfig::NAME, "Name cannot be duplicated"))

    render

    response.body.should have_tag("#new_job_container .form_item_block") do
      with_tag("div.fieldWithErrors input[type='text'][name='job[#{JobConfig::NAME}]']")
      with_tag("div.form_error", "Name cannot be duplicated")
    end
  end

  it "should render job tasks" do
    assign(:job, JobConfig.new(CaseInsensitiveString.new(""), Resources.new, ArtifactPlans.new, com.thoughtworks.go.config.Tasks.new([ExecTask.new].to_java(com.thoughtworks.go.domain.Task))))
    template.should_receive(:render).with(:partial => "admin/shared/job_tasks.html", :locals => instance_of(Hash))

    render
  end

  it "should render job task instructions" do
    assign(:job, JobConfig.new(CaseInsensitiveString.new(""), Resources.new, ArtifactPlans.new, com.thoughtworks.go.config.Tasks.new([ExecTask.new].to_java(com.thoughtworks.go.domain.Task))))

    render

    response.body.should have_tag("#new_job_container") do
      with_tag("div.instructions", "This job requires at least one task. You can add more tasks once this job has been created")
    end
  end
end