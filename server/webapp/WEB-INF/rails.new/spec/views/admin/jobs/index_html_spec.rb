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

describe "admin/jobs/index.html.erb" do
  include GoUtil, FormUI

  before(:each) do
    @pipeline = PipelineConfigMother.createPipelineConfig("pipeline-name", "stage-name", ["job-1", "job-2", "job-3"].to_java(java.lang.String))
    job_1 = @pipeline.get(0).getJobs().get(0)
    job_1.resources().add(Resource.new("resource-1"))
    job_1.resources().add(Resource.new("resource-2"))
    job_1.setRunOnAllAgents(true)
    job_2 = @pipeline.get(0).getJobs().get(1)
    job_2.setRunInstanceCount(2)
    assign(:pipeline, @pipeline)
    assign(:stage, @pipeline.get(0))
    assign(:jobs, @pipeline.get(0).getJobs())
    assign(:cruise_config, @cruise_config = BasicCruiseConfig.new)
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
    view.stub(:random_dom_id).and_return("delete_job_random_id")

    render

    Capybara.string(response.body).all('table.list_table tbody tr td').tap do |tds|
      verify_rows(tds, 0, "job-1", "a[href='#{admin_tasks_listing_path(:job_name => "job-1", :current_tab => "tasks")}']", "resource-1, resource-2", "Yes", "No")

      verify_rows(tds, 5, "job-2", "a[href='#{admin_tasks_listing_path(:job_name => "job-2", :current_tab => "tasks")}']", "", "No", "Yes")

      verify_rows(tds, 10, "job-3", "a[href='#{admin_tasks_listing_path(:job_name => "job-3", :current_tab => "tasks")}']", "", "No", "No")
    end

    expect(response.body).not_to have_selector(".field_with_errors")
    expect(response.body).not_to have_selector(".form_error")
  end

  def verify_rows(tds, start_index, job_name, job_edit_path, resources, run_on_all, run_multiple_instance)
    expect(tds[start_index].text).to eq(job_name)
    expect(tds[start_index]).to have_selector(job_edit_path)
    expect(tds[start_index+1].text).to eq(resources)
    expect(tds[start_index+2].text).to eq(run_on_all)
    expect(tds[start_index+3].text).to eq(run_multiple_instance)
    expect(tds[start_index+4]).to have_selector("span.icon_remove[id='trigger_delete_job_random_id']")
    expect(tds[start_index+4]).to have_selector("script[type='text/javascript']", :visible => false, :text => /Util.escapeDotsFromId\('trigger_delete_job_random_id #warning_prompt'\)/)
    expect(tds[start_index+4]).to have_selector("div#warning_prompt[style='display:none;']", :visible => false, :text => /Are you sure you want to delete the job '#{job_name}' \?/)
  end
end
