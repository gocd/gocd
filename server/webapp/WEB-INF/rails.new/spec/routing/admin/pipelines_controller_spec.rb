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

describe Admin::PipelinesController do
  it "should match /edit" do
    expect({:get => "/admin/pipelines/foo.bar/general"}).to route_to(:controller => "admin/pipelines", :action => "edit", :pipeline_name => 'foo.bar', :current_tab => 'general', :stage_parent => "pipelines")
  end

  it "should match /update" do
    expect({:put => "/admin/pipelines/foo.baz/general"}).to route_to(:controller => "admin/pipelines", :action => "update", :pipeline_name => 'foo.baz', :current_tab => 'general', :stage_parent => "pipelines")
  end

  it "should match /pause_info" do
    expect({:get => "/admin/pipelines/foo.baz/pause_info.json"}).to route_to(:controller => "admin/pipelines", :action => "pause_info", :pipeline_name => 'foo.baz', :format => "json")
    expect(pause_info_refresh_path(:pipeline_name => 'foo.baz')).to eq("/admin/pipelines/foo.baz/pause_info.json")
  end

  it "should match /new" do
    expect({:get => "/admin/pipeline/new"}).to route_to(:controller => "admin/pipelines", :action => "new")
    expect(pipeline_new_path).to eq("/admin/pipeline/new")
    expect(pipeline_new_path(:group => "foo.bar")).to eq("/admin/pipeline/new?group=foo.bar")
  end

  it "should match /create" do
    expect({:post => "/admin/pipelines"}).to route_to(:controller => "admin/pipelines", :action => "create")
    expect(pipeline_create_path).to eq("/admin/pipelines")
  end

  it "should match /clone" do
    expect({:get => "/admin/pipeline/foo.bar/clone"}).to route_to(:controller => "admin/pipelines", :action => "clone", :pipeline_name => 'foo.bar')
    expect(pipeline_clone_path(:pipeline_name => "foo.bar")).to eq("/admin/pipeline/foo.bar/clone")
  end

  it "should match /save_clone" do
    expect({:post => "/admin/pipeline/save_clone"}).to route_to(:controller => "admin/pipelines", :action => "save_clone")
    expect(pipeline_save_clone_path).to eq("/admin/pipeline/save_clone")
  end
end
