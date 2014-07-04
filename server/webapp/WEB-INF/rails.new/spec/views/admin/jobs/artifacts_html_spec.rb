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

describe "admin/jobs/artifacts.html.erb" do
  include GoUtil, FormUI

  before(:each) do
    pipeline = PipelineConfigMother.createPipelineConfig("pipeline-name", "stage-name", ["job-name"].to_java(java.lang.String))
    stage = pipeline.get(0)
    @job = stage.getJobs().get(0)
    assign(:pipeline, pipeline)
    assign(:stage, stage)
    assign(:job, @job)

    assign(:cruise_config, @cruise_config = CruiseConfig.new)
    @cruise_config.addPipeline("group-1", pipeline)

    in_params(:stage_parent => "pipelines", :pipeline_name => "foo_bar", :stage_name => "stage-name", :action => "edit", :controller => "admin/stages", :job_name => "foo_bar_baz", :current_tab => "environment_variables")
  end

  it "should include a hidden field used to find out when all the artifacts are deleted" do
    render

    response.body.should have_tag("form input[type='hidden'][name='default_as_empty_list[]'][value='job>artifactPlans']")
  end
  
  it "should have a heading as Artifacts with a title tooltip" do
    render

    response.body.should have_tag("h3", "Artifacts")
    response.body.should have_tag("span.has_go_tip_right[title='Publish build artifacts to the artifact repository']")
  end

  it "should have a headings for source and destination with a title tooltip" do
    render

    response.body.should have_tag("h4.src", "Source")
    response.body.should have_tag("th span.has_go_tip_right[title='The file or folders to publish to the server. Go will only upload files that are in the working directory of the job. You can use wildcards to specify the files and folders to upload (** means any path, * means any file or folder name).']")
    response.body.should have_tag("h4.dest", "Destination")
    response.body.should have_tag("th span.has_go_tip_right[title='The destination is relative to the artifacts folder of the current instance on the server side. If it is not specified, the artifact will be stored in the root of the artifacts directory']")
    response.body.should have_tag("h4.type", "Type")
    response.body.should have_tag("th span.has_go_tip_right[title='When 'Test Artifact' is selected, Go will use this artifact to generate a test report. Test information is placed in the Failures and Test sub-tabs. Test results from multiple jobs are aggregated on the stage detail pages. This allows you to see the results of tests from both functional and unit tests even if they are run in different jobs.']")
  end

end