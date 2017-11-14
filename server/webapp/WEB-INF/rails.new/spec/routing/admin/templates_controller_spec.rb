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

describe Admin::TemplatesController do
  it "should resolve route to the templates listing page" do
    expect({:get => "/admin/templates"}).to route_to(:controller => "admin/templates", :action => "index")
  end

  it "should generate listing route" do
    expect(templates_url(UrlBuilder.default_url_options)).to eq("http://test.host/admin/templates")
  end

  it "should resolve route to the template delete" do
    expect({:delete => "/admin/templates/template.name"}).to route_to(:controller => "admin/templates", :action => "destroy", :pipeline_name => "template.name")
    expect(delete_template_path(:pipeline_name => "template.name")).to eq("/admin/templates/template.name")
  end

  it "should resolve & generate route to the template edit" do
    expect({:get => "/admin/templates/blah.blah/general"}).to route_to(:controller => "admin/templates", :action => "edit", :stage_parent => "templates", :pipeline_name => "blah.blah", :current_tab => 'general')
    expect(template_edit_path(:pipeline_name => "blah.blah", :current_tab => 'general')).to eq("/admin/templates/blah.blah/general")
  end

  it "should resolve & generate route to the template update" do
    expect({:put => "/admin/templates/blah.blah/general"}).to route_to(:controller => "admin/templates", :action => "update", :stage_parent => "templates", :pipeline_name => "blah.blah", :current_tab => 'general')
    expect(template_update_path(:pipeline_name => "blah.blah", :current_tab => 'general')).to eq("/admin/templates/blah.blah/general")
  end

  it "should resolve & generate route for new" do
    expect({:get => "/admin/templates/new"}).to route_to(:controller => "admin/templates", :action => "new")
    expect(template_new_path).to eq("/admin/templates/new")
  end

  it "should resolve & generate route for create" do
    expect({:post => "/admin/templates/create"}).to route_to(:controller => "admin/templates", :action => "create")
    expect(template_create_path).to eq("/admin/templates/create")
  end

  it "should resolve & generate route for edit permissions" do
    expect({:get => "/admin/templates/template_name/permissions"}).to route_to(:controller => "admin/templates", :action => "edit_permissions", :template_name => "template_name")
    expect(edit_template_permissions_path(:template_name => "foo")).to eq("/admin/templates/foo/permissions")
  end

  it "should resolve & generate route for update permissions" do
    expect({:post => "/admin/templates/template_name/permissions"}).to route_to(:controller => "admin/templates", :action => "update_permissions", :template_name => "template_name")
    expect(update_template_permissions_path(:template_name => "foo")).to eq("/admin/templates/foo/permissions")
  end
end

