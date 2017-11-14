##########################################################################
# Copyright 2017 ThoughtWorks, Inc.
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
##########################################################################

require 'rails_helper'

describe Admin::StagesController do
  it "should resolve index" do
    expect({:get => "/admin/pipelines/dev/stages"}).to route_to(:controller => "admin/stages", :action => "index", :pipeline_name => "dev", :stage_parent => "pipelines")
  end

  it "should resolve new" do
    expect({:get => "/admin/pipelines/dev/stages/new"}).to route_to(:controller => "admin/stages", :action => "new", :pipeline_name => "dev", :stage_parent => "pipelines")
  end

  it "should resolve create" do
    expect({:post => "/admin/pipelines/dev/stages"}).to route_to(:controller => "admin/stages", :action => "create", :pipeline_name => "dev", :stage_parent => "pipelines")
  end

  it "should resolve edit/settings" do
    expect({:get => "/admin/pipelines/dev/stages/test.foo/settings"}).to route_to(:controller => "admin/stages", :action => "edit", :stage_parent => "pipelines", :pipeline_name => "dev", :stage_name => "test.foo", :current_tab => "settings")
    expect({:get => "/admin/templates/dev/stages/test.foo/settings"}).to route_to(:controller => "admin/stages", :action => "edit", :stage_parent => "templates", :pipeline_name => "dev", :stage_name => "test.foo", :current_tab => "settings")
  end

  it "should generate delete" do
    expect(admin_stage_delete_path(:pipeline_name => "foo.bar", :stage_name => "baz.foo", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/baz.foo")
    expect({:delete => "/admin/pipelines/foo.bar/stages/baz.foo"}).to route_to(:controller => "admin/stages", :action => "destroy", :stage_parent => "pipelines", :pipeline_name => "foo.bar", :stage_name => "baz.foo")
  end

  it "should resolve edit/environment_variables" do
    expect({:get => "/admin/pipelines/dev/stages/baz.foo/environment_variables"}).to route_to(:controller => "admin/stages", :action => "edit", :stage_parent => "pipelines", :pipeline_name => "dev", :stage_name => "baz.foo", :current_tab => "environment_variables")
  end

  it "should generate edit/settings" do
    expect(admin_stage_edit_path(:stage_parent => "pipelines", :pipeline_name => "foo.bar", :stage_name => "baz.foo", :current_tab => "settings")).to eq("/admin/pipelines/foo.bar/stages/baz.foo/settings")
  end

  it "should generate edit/environment_variables" do
    expect(admin_stage_edit_path(:stage_parent => "pipelines", :pipeline_name => "foo.bar", :stage_name => "baz.foo", :current_tab => "environment_variables")).to eq("/admin/pipelines/foo.bar/stages/baz.foo/environment_variables")
  end

  it "should resolve update/settings" do
    expect({:put => "/admin/pipelines/dev/stages/baz.foo/settings"}).to route_to(:controller => "admin/stages", :action => "update", :stage_parent => "pipelines", :pipeline_name => "dev", :stage_name => "baz.foo", :current_tab => "settings")
  end

  it "should generate update/settings" do
    expect(admin_stage_update_path(:stage_parent => "pipelines", :pipeline_name => "foo.bar", :stage_name => "baz.foo", :current_tab => "settings")).to eq("/admin/pipelines/foo.bar/stages/baz.foo/settings")
  end

  it "should generate index" do
    expect(admin_stage_listing_path(:pipeline_name => "foo.bar", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages")
    expect({:get => "/admin/pipelines/foo.bar/stages"}).to route_to(:controller => "admin/stages", :action => "index", :stage_parent=>"pipelines", :pipeline_name => "foo.bar")
  end

  it "should generate new" do
    expect(admin_stage_new_path(:pipeline_name => "foo.bar", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/new")
  end

  it "should generate create" do
    expect(admin_stage_create_path(:pipeline_name => "foo.bar", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages")
  end

  it "should generate edit" do
    expect(admin_stage_edit_path(:stage_parent => "pipelines", :pipeline_name => "foo.bar", :stage_name => "my.stage", :current_tab => "settings")).to eq("/admin/pipelines/foo.bar/stages/my.stage/settings")
  end

  it "should generate increment_index" do
    expect(admin_stage_increment_index_path(:pipeline_name => "foo.bar", :stage_name => "baz.foo", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/baz.foo/index/increment")
  end

  it "should generate decrement_index" do
    expect(admin_stage_decrement_index_path(:pipeline_name => "foo.bar", :stage_name => "baz.foo", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/baz.foo/index/decrement")
  end

  it "should generate use template" do
    expect({:put => "/admin/pipelines/foo.bar/stages"}).to route_to(:controller => "admin/stages", :action => "use_template", :stage_parent=>"pipelines", :pipeline_name => "foo.bar")
    expect(admin_stage_use_template_path(:pipeline_name => "foo.bar", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages")
  end
end
