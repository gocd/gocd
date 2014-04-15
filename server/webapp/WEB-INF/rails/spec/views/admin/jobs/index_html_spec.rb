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

describe "admin/jobs/index.html.erb" do
  include GoUtil, FormUI

  before(:each) do
    @pipeline = PipelineConfigMother.createPipelineConfig("pipeline-name", "stage-name", ["job-1", "job-2", "job-3"].to_java(java.lang.String))
    job_1 = @pipeline.get(0).getJobs().get(0)
    job_1.resources().add(Resource.new("resource-1"))
    job_1.resources().add(Resource.new("resource-2"))
    job_1.setRunOnAllAgents(true)
    assigns[:pipeline] = @pipeline
    assigns[:stage] = @pipeline.get(0)
    assigns[:jobs] = @pipeline.get(0).getJobs()
    assigns[:cruise_config] = @cruise_config = CruiseConfig.new
    @cruise_config.addPipeline("group-1", @pipeline)
    set(@cruise_config, "md5", "abc")
    in_params(:stage_parent => "pipelines", :pipeline_name => "pipeline-name", :action => "index", :controller => "admin/jobs", :stage_name => "stage-name")
  end

  it "should show appropriate headers in table" do
    render 'admin/jobs/index.html.erb'
    response.body.should have_tag("table.list_table") do
      with_tag("th", "Job")
      with_tag("th", "Resources")
      with_tag("th", "Run on all")
      with_tag("th", "Remove")
    end
  end

  it "should show job listing" do
    template.stub(:random_dom_id).and_return("delete_job_random_id")
    render 'admin/jobs/index.html.erb'
    response.body.should have_tag("table.list_table") do
      with_tag("tr") do
        with_tag("td", "job-1") do
          with_tag("a[href=?]", admin_tasks_listing_path(:job_name => "job-1", :current_tab => "tasks"))
        end
        with_tag("td", "resource-1, resource-2")
        with_tag("td", "Yes")
        with_tag("td span.icon_remove[id=?]", 'trigger_delete_job_random_id')
        with_tag("script[type='text/javascript']", /Util.escapeDotsFromId\('trigger_delete_job_random_id #warning_prompt'\)/)
        with_tag("div#warning_prompt[style='display:none;']", /Are you sure you want to delete the job 'job-1' \?/)
      end
      with_tag("tr") do
        with_tag("td", "job-2") do
          with_tag("a[href=?]", admin_tasks_listing_path(:job_name => "job-2", :current_tab => "tasks"))
        end
        with_tag("td", "")
        with_tag("td", "No")
        with_tag("td span.icon_remove[id=?]", 'trigger_delete_job_random_id')
        with_tag("script[type='text/javascript']", /Util.escapeDotsFromId\('trigger_delete_job_random_id #warning_prompt'\)/)
        with_tag("div#warning_prompt[style='display:none;']", /Are you sure you want to delete the job 'job-2' \?/)
      end
      with_tag("tr") do
        with_tag("td", "job-3")do
          with_tag("a[href=?]", admin_tasks_listing_path(:job_name => "job-3", :current_tab => "tasks"))
        end
        with_tag("td", "")
        with_tag("td", "No")
        with_tag("td span.icon_remove[id=?]", 'trigger_delete_job_random_id')
        with_tag("script[type='text/javascript']", /Util.escapeDotsFromId\('trigger_delete_job_random_id #warning_prompt'\)/)
        with_tag("div#warning_prompt[style='display:none;']", /Are you sure you want to delete the job 'job-3' \?/)
      end
    end
    response.body.should_not have_tag(".fieldWithErrors")
    response.body.should_not have_tag(".form_error")
  end
end
