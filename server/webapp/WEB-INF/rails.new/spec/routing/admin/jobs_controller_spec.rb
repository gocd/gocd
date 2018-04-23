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

require 'rails_helper'

describe Admin::JobsController do
  it "should resolve new" do
    expect({:get => "/admin/pipelines/dev/stages/test.1/jobs/new"}).to route_to(:controller => "admin/jobs", :action => "new", :pipeline_name => "dev", :stage_name => "test.1", :stage_parent => "pipelines")
    expect({:get => "/admin/templates/dev/stages/test.1/jobs/new"}).to route_to(:controller => "admin/jobs", :action => "new", :pipeline_name => "dev", :stage_name => "test.1", :stage_parent => "templates")
    expect(admin_job_new_path(:pipeline_name => "foo.bar", :stage_name => "test.1", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/test.1/jobs/new")
    expect(admin_job_new_path(:pipeline_name => "foo.bar", :stage_name => "test.1", :stage_parent => "templates")).to eq("/admin/templates/foo.bar/stages/test.1/jobs/new")
  end

  it "should resolve create" do
    expect({:post => "/admin/pipelines/dev/stages/test.1/jobs"}).to route_to(:controller => "admin/jobs", :action => "create", :pipeline_name => "dev", :stage_name => "test.1", :stage_parent => "pipelines")
    expect(admin_job_create_path(:pipeline_name => "foo.bar", :stage_name => "test.1", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/test.1/jobs")
  end

  it "should resolve destroy" do
    expect({:delete => "/admin/pipelines/dev/stages/test.1/job/job.1"}).to route_to(:controller => "admin/jobs", :action => "destroy", :pipeline_name => "dev", :stage_name => "test.1", :job_name => "job.1", :stage_parent => "pipelines")
  end

  it "should generate index" do
    expect({:get => "/admin/pipelines/dev/stages/test.1/jobs"}).to route_to(:controller => "admin/jobs", :action => "index", :pipeline_name => "dev", :stage_name => "test.1", :stage_parent => "pipelines")
    expect(admin_job_listing_path(:pipeline_name => "foo.bar", :stage_name => "test.1", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/test.1/jobs")
  end

  it "should generate destroy" do
    # Cannot have route_for for DELETE as route_for does not honor the :method => :delete attribute
    expect(admin_job_delete_path(:pipeline_name => "foo.bar", :stage_name => "test.1", :job_name => "job.1", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/test.1/job/job.1")
  end

  it "should generate route for tabs" do
    expect({:get => "/admin/pipelines/dev/stages/test.1/job/job.1/tabs"}).to route_to(:controller => "admin/jobs", :action => "edit", :pipeline_name => "dev", :stage_name => "test.1", :job_name => "job.1", :stage_parent => "pipelines", :current_tab => "tabs")
    expect(admin_job_edit_path(:pipeline_name => "foo.bar", :stage_name => "foo.bar", :job_name => "foo.bar", :current_tab => "tabs", :stage_parent => "templates")).to eq("/admin/templates/foo.bar/stages/foo.bar/job/foo.bar/tabs")
    expect(admin_job_edit_path(:pipeline_name => "foo.bar", :stage_name => "foo.bar", :job_name => "foo.bar", :current_tab => "tabs", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/foo.bar/job/foo.bar/tabs")
  end
end
