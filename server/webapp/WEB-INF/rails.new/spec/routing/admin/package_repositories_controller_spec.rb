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

describe Admin::PackageRepositoriesController do
  it "should resolve route to the new package-repositories page" do
    expect({:get => "/admin/package_repositories/new"}).to route_to(:controller => "admin/package_repositories", :action => "new")
    expect(package_repositories_new_path).to eq("/admin/package_repositories/new")
  end

  it "should resolve route to the list package-repositories page" do
    expect({:get => "/admin/package_repositories/list"}).to route_to(:controller => "admin/package_repositories", :action => "list")
    expect(package_repositories_list_path).to eq("/admin/package_repositories/list")
  end

  it "should resolve route to the create package-repositories page" do
    expect({:post => "/admin/package_repositories"}).to route_to(:controller => "admin/package_repositories", :action => "create")
    expect(package_repositories_create_path).to eq("/admin/package_repositories")
  end

  it "should resolve route to the edit package-repositories page" do
    expect({:get => "/admin/package_repositories/abcd-1234/edit"}).to route_to(:controller => "admin/package_repositories", :action => "edit", :id => "abcd-1234")
    expect(package_repositories_edit_path(:id => "abcd-1234")).to eq("/admin/package_repositories/abcd-1234/edit")
  end

  it "should resolve route to the update package-repositories page" do
    expect({:put => "/admin/package_repositories/abcd-1234"}).to route_to(:controller => "admin/package_repositories", :action => "update", :id => "abcd-1234")
    expect(package_repositories_update_path(:id => "abcd-1234")).to eq("/admin/package_repositories/abcd-1234")
  end

  it "should resolve route to plugin config" do
    expect({:get => "/admin/package_repositories/abcd-1234/config"}).to route_to(:controller => "admin/package_repositories", :action => "plugin_config", :plugin => "abcd-1234")
    expect(package_repositories_plugin_config_path(:plugin => "abcd-1234")).to eq("/admin/package_repositories/abcd-1234/config")
  end

  it "should resolve route to plugin config for repo" do
    expect({:get => "/admin/package_repositories/repoid/pluginid/config"}).to route_to(:controller => "admin/package_repositories", :action => "plugin_config_for_repo", :plugin => "pluginid", :id => "repoid")
    expect(package_repositories_plugin_config_for_repo_path(:plugin => "pluginid", :id => "repoid")).to eq("/admin/package_repositories/repoid/pluginid/config")
  end

  it "should resolve route to check connection for repo" do
    expect_any_instance_of(HeaderConstraint).to receive(:matches?).with(any_args).and_return(true)
    expect({:post => "admin/package_repositories/check_connection"}).to route_to(:controller => "admin/package_repositories", :action => "check_connection")
    expect(package_repositories_check_connection_path).to eq("/admin/package_repositories/check_connection")
  end

  it "should resolve route to deletion of repo" do
    expect({:delete => "/admin/package_repositories/repo"}).to route_to(:controller => "admin/package_repositories", :action => "destroy", :id => "repo")
    expect(package_repositories_delete_path(:id => "repo")).to eq("/admin/package_repositories/repo")
  end

  it "should allow dots in the name of a plugin in the route for a plugin's configuration" do
    expect({:get => "/admin/package_repositories/plugin.id.with.dots/config"}).to route_to(:controller => "admin/package_repositories", :action => "plugin_config", :plugin => "plugin.id.with.dots")
    expect(package_repositories_plugin_config_path(:plugin => "plugin.id.with.dots")).to eq("/admin/package_repositories/plugin.id.with.dots/config")
  end

  it "should allow dots in the name of a plugin in the route for a plugin config for a repository" do
    expect({:get => "/admin/package_repositories/repoid/plugin.id.with.dots/config"}).to route_to(:controller => "admin/package_repositories", :action => "plugin_config_for_repo", :plugin => "plugin.id.with.dots", :id => "repoid")
    expect(package_repositories_plugin_config_for_repo_path(:plugin => "plugin.id.with.dots", :id => "repoid")).to eq("/admin/package_repositories/repoid/plugin.id.with.dots/config")
  end
end
