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
    assign(:pipeline, @pipeline)
    assign(:stage, @pipeline.get(0))
    assign(:jobs, @pipeline.get(0).getJobs())
    assign(:cruise_config, @cruise_config = CruiseConfig.new)
    @cruise_config.addPipeline("group-1", @pipeline)
    set(@cruise_config, "md5", "abc")
    in_params(:stage_parent => "pipelines", :pipeline_name => "pipeline-name", :action => "index", :controller => "admin/jobs", :stage_name => "stage-name")
  end

  it "should show appropriate headers in table" do
    render

    Capybara.string(response.body).find('table.list_table').tap do |table|
      expect(table).to have_selector("th", :text => "Job")
      expect(table).to have_selector("th", :text => "Resources")
      expect(table).to have_selector("th", :text => "Run on all")
      expect(table).to have_selector("th", :text => "Remove")
    end
  end

  it "should show job listing" do
    template.stub(:random_dom_id).and_return("delete_job_random_id")

    render

    Capybara.string(response.body).find('table.list_table').tap do |table|
      table.all("tr") do |trs|
        expect(trs[0]).to have_selector("td", :text => "job-1")
        trs[0].find("td") do |td|
          expect(td).to have_selector("a[href='#{admin_tasks_listing_path(:job_name => "job-1", :current_tab => "tasks")}']")
        end
        expect(trs[0]).to have_selector("td", :text => "resource-1, resource-2")
        expect(trs[0]).to have_selector("td", :text => "Yes")
        expect(trs[0]).to have_selector("td span.icon_remove[id='trigger_delete_job_random_id']")
        expect(trs[0]).to have_selector("script[type='text/javascript']", :text => /Util.escapeDotsFromId\('trigger_delete_job_random_id #warning_prompt'\)/)
        expect(trs[0]).to have_selector("div#warning_prompt[style='display:none;']", :text => /Are you sure you want to delete the job 'job-1' \?/)
      end
      table.all("tr") do |trs|
        expect(trs[1]).to have_selector("td", :text => "job-2")
        trs[1].find("td") do |td|
          expect(td).to have_selector("a[href='#{admin_tasks_listing_path(:job_name => "job-2", :current_tab => "tasks")}']")
        end
        expect(trs[1]).to have_selector("td", :text => "")
        expect(trs[1]).to have_selector("td", :text => "No")
        expect(trs[1]).to have_selector("td span.icon_remove[id='trigger_delete_job_random_id']")
        expect(trs[1]).to have_selector("script[type='text/javascript']", :text => /Util.escapeDotsFromId\('trigger_delete_job_random_id #warning_prompt'\)/)
        expect(trs[1]).to have_selector("div#warning_prompt[style='display:none;']", :text => /Are you sure you want to delete the job 'job-2' \?/)
      end
      table.all("tr") do |trs|
        expect(trs[2]).to have_selector("td", :text => "job-3")
        trs[2].find("td") do |td|
          expect(td).to have_selector("a[href='#{admin_tasks_listing_path(:job_name => "job-3", :current_tab => "tasks")}']")
        end
        expect(trs[2]).to have_selector("td", :text => "")
        expect(trs[2]).to have_selector("td", :text => "No")
        expect(trs[2]).to have_selector("td span.icon_remove[id='trigger_delete_job_random_id']")
        expect(trs[2]).to have_selector("script[type='text/javascript']", :text => /Util.escapeDotsFromId\('trigger_delete_job_random_id #warning_prompt'\)/)
        expect(trs[2]).to have_selector("div#warning_prompt[style='display:none;']", :text => /Are you sure you want to delete the job 'job-3' \?/)
      end
    end

    expect(response.body).not_to have_selector(".field_with_errors")
    expect(response.body).not_to have_selector(".form_error")
  end
end
