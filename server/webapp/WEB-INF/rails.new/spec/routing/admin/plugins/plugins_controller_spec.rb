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

describe Admin::Plugins::PluginsController do
  it "should resolve the route_for_index" do
    expect({:get => "/admin/old_plugins"}).to route_to(:controller => "admin/plugins/plugins", :action => "index")
    expect(plugins_listing_path).to eq("/admin/old_plugins")
  end

  it "should resolve_the_route_for_upload" do
    expect({:post => "/admin/old_plugins"}).to route_to(:controller => "admin/plugins/plugins", :action => "upload")
    expect(upload_plugin_path).to eq("/admin/old_plugins")
  end

  it "should resolve_the_route_for_get plugin settings" do
    expect({:get => "/admin/old_plugins/settings/plugin.id"}).to route_to(:controller => "admin/plugins/plugins", :action => "edit_settings", :plugin_id => "plugin.id")
    expect(edit_settings_path(:plugin_id => 'plugin.id')).to eq("/admin/old_plugins/settings/plugin.id")
  end

  it "should resolve_the_route_for_update plugin settings" do
    expect({:post => "/admin/old_plugins/settings/plugin.id"}).to route_to(:controller => "admin/plugins/plugins", :action => "update_settings", :plugin_id => "plugin.id")
    expect(update_settings_path(:plugin_id => 'plugin.id')).to eq("/admin/old_plugins/settings/plugin.id")
  end
end
