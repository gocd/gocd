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

describe Admin::PackageDefinitionsController do
  it "should resolve route to the new package_definitions page" do
    expect({:get => "/admin/package_definitions/repoid/new"}).to route_to(:controller => "admin/package_definitions", :action => "new", :repo_id => "repoid")
    expect(package_definitions_new_path(:repo_id => "repoid")).to eq("/admin/package_definitions/repoid/new")
  end

  it "should resolve route to the new package_definitions page" do
    expect({:get => "/admin/package_definitions/repoid/new_for_new_pipeline_wizard"}).to route_to(:controller => "admin/package_definitions", :action => "new_for_new_pipeline_wizard", :repo_id => "repoid")
    expect(package_definitions_new_for_new_pipeline_wizard_path(:repo_id => "repoid")).to eq("/admin/package_definitions/repoid/new_for_new_pipeline_wizard")
  end

  it "should route to package_config action" do
    expect({:get => "/admin/package_definitions/repoid/packageid"}).to route_to(:controller => "admin/package_definitions", :action => "show", :repo_id => "repoid", :package_id => "packageid")
    expect(package_definitions_show_path(:repo_id => "repoid", :package_id => "packageid")).to eq("/admin/package_definitions/repoid/packageid")
  end

  it "should route to package_config action" do
    expect({:get => "/admin/package_definitions/repoid/packageid/for_new_pipeline_wizard"}).to route_to(:controller => "admin/package_definitions", :action => "show_for_new_pipeline_wizard", :repo_id => "repoid", :package_id => "packageid")
    expect(package_definitions_show_for_new_pipeline_wizard_path(:repo_id => "repoid", :package_id => "packageid")).to eq("/admin/package_definitions/repoid/packageid/for_new_pipeline_wizard")
  end

  it "should route to package_config action with repository listing" do
    expect({:get => "/admin/package_definitions/repoid/packageid/with_repository_list"}).to route_to(:controller => "admin/package_definitions", :action => "show_with_repository_list", :repo_id => "repoid", :package_id => "packageid")
    expect(package_definitions_show_with_repository_list_path(:repo_id => "repoid", :package_id => "packageid")).to eq("/admin/package_definitions/repoid/packageid/with_repository_list")
  end

  it "should route to pipeline used in" do
    expect({:get => "/admin/package_definitions/repoid/packageid/pipelines_used_in"}).to route_to(:controller => "admin/package_definitions", :action => "pipelines_used_in", :repo_id => "repoid", :package_id => "packageid")
    expect(pipelines_used_in_path(:repo_id => "repoid", :package_id => "packageid")).to eq("/admin/package_definitions/repoid/packageid/pipelines_used_in")
  end

  it "should route to delete package" do
    expect({:delete => "/admin/package_definitions/repoid/packageid"}).to route_to(:controller => "admin/package_definitions", :action => "destroy", :repo_id => "repoid", :package_id => "packageid")
    expect(package_definition_delete_path(:repo_id => "repoid", :package_id => "packageid")).to eq("/admin/package_definitions/repoid/packageid")
  end

  it "should resolve route to check connection for repo" do
    expect_any_instance_of(HeaderConstraint).to receive(:matches?).with(any_args).and_return(true)
    expect({:post => "admin/package_definitions/check_connection"}).to route_to(:controller => "admin/package_definitions", :action => "check_connection")
    expect(package_definition_check_connection_path).to eq("/admin/package_definitions/check_connection")
  end
end
