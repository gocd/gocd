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

describe Admin::PipelineGroupsController do
  include ApiHeaderSetupForRouting

  it "should resolve route to the pipeline groups listing page" do
    expect({:get => "/admin/pipelines"}).to route_to(:controller => "admin/pipeline_groups", :action => "index")
  end

  it "should generate listing route" do
    expect(pipeline_groups_url(UrlBuilder.default_url_options)).to eq("http://test.host/admin/pipelines")
  end

  it "should resolve route to move" do
    expect({:put => "/admin/pipelines/move/pipeline.name"}).to route_to(:controller => "admin/pipeline_groups", :action => "move", :pipeline_name => "pipeline.name")
  end

  it "should generate move route" do
    expect(move_pipeline_to_group_url(UrlBuilder.default_url_options.merge(:pipeline_name => "pipeline.name"))).to eq("http://test.host/admin/pipelines/move/pipeline.name")
  end

  it "should resolve route to delete of pipeline" do
    expect({:delete => "/admin/pipelines/pipeline.name"}).to route_to(:controller => "admin/pipeline_groups", :action => "destroy", :pipeline_name => "pipeline.name")
  end

  it "should generate group edit route" do
    expect(pipeline_group_edit_path(:group_name => "foo.group")).to eq("/admin/pipeline_group/foo.group/edit")
  end

  it "should resolve route to edit pipeline group" do
    expect({:get => "/admin/pipeline_group/foo.group/edit"}).to route_to(:controller => "admin/pipeline_groups", :action => "edit", :group_name => "foo.group")
  end

  it "should resolve route to show pipeline group" do
    expect({:get => "/admin/pipeline_group/foo.group"}).to route_to(:controller => "admin/pipeline_groups", :action => "show", :group_name => "foo.group")
  end

  it "should resolve route to new pipeline group" do
    expect({:get => "/admin/pipeline_group/new"}).to route_to(:controller => "admin/pipeline_groups", :action => "new")
  end

  it "should resolve /possible_groups" do
    expect({:get => "/admin/pipelines/possible_groups/my_pipeline/my_md5"}).to route_to(:controller => "admin/pipeline_groups", :action => "possible_groups", :pipeline_name => "my_pipeline", :config_md5 => "my_md5")
    expect(possible_groups_path(:pipeline_name => "my_pipeline", :config_md5 => "my_md5")).to eq("/admin/pipelines/possible_groups/my_pipeline/my_md5")
  end

  it "should generate group update route" do
    expect(pipeline_group_update_path(:group_name => "foo.group")).to eq("/admin/pipeline_group/foo.group")
  end

  it "should resolve route to update pipeline group" do
    expect({:put => "/admin/pipeline_group/foo.group"}).to route_to(:controller => "admin/pipeline_groups", :action => "update", :group_name => "foo.group")
  end

  it "should generate delete pipeline route" do
    expect(delete_pipeline_url(UrlBuilder.default_url_options.merge(:pipeline_name => "pipeline.name"))).to eq("http://test.host/admin/pipelines/pipeline.name")
  end

  it "should generate new pipeline group route" do
    expect(pipeline_group_new_url(UrlBuilder.default_url_options)).to eq("http://test.host/admin/pipeline_group/new")
  end

  it "should generate new pipeline group route" do
    expect(pipeline_group_create_url(UrlBuilder.default_url_options)).to eq("http://test.host/admin/pipeline_group")
  end

  it "should generate route for destroy of group" do
    expect(pipeline_group_delete_path(:group_name => "group.foo")).to eq("/admin/pipeline_group/group.foo")
    expect({:delete => "/admin/pipeline_group/foo.group"}).to route_to(:controller => "admin/pipeline_groups", :action => "destroy_group", :group_name => "foo.group")
  end
end
