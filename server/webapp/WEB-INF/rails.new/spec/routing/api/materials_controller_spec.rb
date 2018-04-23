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

describe Api::MaterialsController do
  include ApiHeaderSetupForRouting

  describe "notify" do
    it "should generate the route" do
      expect(material_notify_path(:post_commit_hook_material_type => 'svn')).to eq("/api/material/notify/svn")
    end

    it "should resolve" do
      stub_confirm_header
      expect(:post => "/api/material/notify/svn").to route_to(:controller => "api/materials", :action => "notify", :no_layout => true, :post_commit_hook_material_type => "svn")
    end
  end

  describe "list_materials_config" do
    it "should resolve" do
      expect(:get => "/api/config/materials").to route_to(:controller => "api/materials", :action => "list_configs", :no_layout => true)
    end
  end

  describe "list_material_modifications" do
    it "should resolve" do
      expect(:get => "/api/materials/fingerprint/modifications").to route_to(:controller => "api/materials", :action => "modifications", :fingerprint => "fingerprint", :offset => "0", :no_layout => true)
      expect(:get => "/api/materials/fingerprint/modifications/1").to route_to(:controller => "api/materials", :action => "modifications", :fingerprint => "fingerprint", :offset => "1", :no_layout => true)
    end
  end
end
