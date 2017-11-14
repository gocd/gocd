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

describe Admin::ConfigurationController do
  it "view" do
    expect(config_view_path).to eq("/admin/config_xml")
    expect({:get => "/admin/config_xml"}).to route_to(:controller => "admin/configuration", :action => "show")
  end

  it "edit" do
    expect(config_edit_path).to eq("/admin/config_xml/edit")
    expect({:get => "/admin/config_xml/edit"}).to route_to(:controller => "admin/configuration", :action => "edit")
  end

  it "update" do
    expect(config_update_path).to eq("/admin/config_xml")
    expect({:put => "/admin/config_xml"}).to route_to(:controller => "admin/configuration", :action => "update")
  end
end
