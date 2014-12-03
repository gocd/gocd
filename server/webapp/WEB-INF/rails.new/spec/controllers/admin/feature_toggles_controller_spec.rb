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

require File.join(File.dirname(__FILE__), "..", "..", "spec_helper")

describe Admin::FeatureTogglesController do
  describe :route do
    it "should resolve route to list all feature toggles" do
      {:get => "/admin/feature_toggles"}.should route_to(:controller => "admin/feature_toggles", :action => "index", :no_layout => true)
      feature_toggles_path.should == "/admin/feature_toggles"
    end
  end

  describe :index do
    it "should list existing feature toggles in JSON format" do
      feature_toggle_service = stub_service(:feature_toggle_service)
      feature_toggle_service.stub(:allToggles).and_return(FeatureToggleMother.someToggles())

      get :index

      toggles = JSON.parse response.body
      expect(toggles.length).to eq(2)

      expect(toggles[0]["key"]).to eq("key1")
      expect(toggles[0]["description"]).to eq("desc1")
      expect(toggles[0]["value"]).to eq(true)
      expect(toggles[0]["has_changed"]).to eq(false)

      expect(toggles[1]["key"]).to eq("key2")
      expect(toggles[1]["description"]).to eq("desc2")
      expect(toggles[1]["value"]).to eq(false)
      expect(toggles[1]["has_changed"]).to eq(true)
    end

    it "should be empty JSON when there are no toggles" do
      feature_toggle_service = stub_service(:feature_toggle_service)
      feature_toggle_service.stub(:allToggles).and_return(FeatureToggleMother.noToggles())

      get :index

      toggles = JSON.parse response.body
      expect(toggles.length).to eq(0)
    end
  end
end