##########################GO-LICENSE-START################################
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
##########################GO-LICENSE-END##################################

require 'rails_helper'

describe Api::FeatureTogglesController do
  describe "route" do
    it "should resolve route to list all feature toggles" do
      expect({:get => "/api/admin/feature_toggles"}).to route_to(:controller => "api/feature_toggles", :action => "index", :no_layout => true, :format => :json)
      expect(api_admin_feature_toggles_path).to eq("/api/admin/feature_toggles")
    end

    it "should resolve route to update the value of feature toggle" do
      expect_any_instance_of(HeaderConstraint).to receive(:matches?).with(any_args).and_return(true)
      expect({:post => "/api/admin/feature_toggles/toggle.key"}).to route_to(:controller => "api/feature_toggles", :action => "update", :toggle_key => "toggle.key",
                                                                             :no_layout => true, :format => :json)
      expect(api_admin_feature_toggle_update_path("abc")).to eq("/api/admin/feature_toggles/abc")
    end
  end
end
