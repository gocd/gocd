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
load File.join(File.dirname(__FILE__), "..", "environment_variables_form_example.rb")

describe "admin/jobs/tabs.html.erb" do
  include GoUtil, ReflectiveUtil, FormUI

  before(:each) do
    pipeline = PipelineConfigMother.createPipelineConfig("pipeline-name", "stage-name", ["job-name"].to_java(java.lang.String))
    stage = pipeline.get(0)
    @job = stage.getJobs().get(0)
    @job.addTab("tab1", "path1")
    @job.addTab("tab2", "path2")
    assign(:pipeline, pipeline)
    assign(:stage, stage)
    assign(:job, @job)

    assign(:cruise_config, @cruise_config = BasicCruiseConfig.new)
    @cruise_config.addPipeline("group-1", pipeline)
    set(@cruise_config, "md5", "abc")
    in_params(:stage_parent => "pipelines", :pipeline_name => "pipline-name", :stage_name => "stage-name", :action => "edit", :controller => "admin/jobs", :job_name => "job-name", :current_tab => "tabs")

  end

  it "should populate env vars for the pipeline" do
    render

    Capybara.string(response.body).find('form').tap do |form|
      expect(form).to have_selector("input[name='job[tabs][][name]'][value='tab1']")
      expect(form).to have_selector("input[name='job[tabs][][original_name]'][value='tab1']")
      expect(form).to have_selector("input[name='job[tabs][][path]'][value='path1']")

      expect(form).to have_selector("input[name='job[tabs][][name]'][value='tab2']")
      expect(form).to have_selector("input[name='job[tabs][][original_name]'][value='tab2']")
      expect(form).to have_selector("input[name='job[tabs][][path]'][value='path2']")

      expect(form).to have_selector("input[name='default_as_empty_list[]'][value='job>tabs']")
    end
  end

#  it "should show errors" do
#    errors = config_errors([EnvironmentVariableConfig::NAME, "bad env var name"], [EnvironmentVariableConfig::VALUE, "bad value"])
#    set(@variables.get(0), "configErrors", errors)
#
#    render
#
#    Capybara.string(response.body).find('form').tap do |form|
#      expect(form).to have_selector("div.field_with_errors input[name='job[variables][][name]'][value='env-name']")
#      expect(form).to have_selector("div.name_value_error", :text => "bad env var name")
#      expect(form).to have_selector("div.field_with_errors input[name='job[variables][][value]'][value='env-val']")
#      expect(form).to have_selector("div.name_value_error", :text => "bad value")
#    end
#  end

end
