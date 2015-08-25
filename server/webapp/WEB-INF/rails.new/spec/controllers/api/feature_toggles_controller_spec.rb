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

require 'spec_helper'

describe Api::FeatureTogglesController do
  before do
    @feature_toggle_service = stub_service(:feature_toggle_service)
  end

  describe :route do
    it "should resolve route to list all feature toggles" do
      {:get => "/api/admin/feature_toggles"}.should route_to(:controller => "api/feature_toggles", :action => "index", :no_layout => true, :format => :json)
      api_admin_feature_toggles_path.should == "/api/admin/feature_toggles"
    end

    it "should resolve route to update the value of feature toggle" do
      {:post => "/api/admin/feature_toggles/toggle.key"}.should route_to(:controller => "api/feature_toggles", :action => "update", :toggle_key => "toggle.key",
                                                                     :no_layout => true, :format => :json)
      api_admin_feature_toggle_update_path("abc").should == "/api/admin/feature_toggles/abc"
    end
  end

  describe :index do
    it "should list existing feature toggles in JSON format" do
      @feature_toggle_service.stub(:allToggles).and_return(FeatureToggleMother.someToggles())

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
      @feature_toggle_service.stub(:allToggles).and_return(FeatureToggleMother.noToggles())

      get :index

      toggles = JSON.parse response.body
      expect(toggles.length).to eq(0)
    end
  end

  describe :update do
    it "should update the value of a specified key to true when sent 'on'" do
      expect(@feature_toggle_service).to receive(:changeValueOfToggle).with("key.to.toggle", true)

      post :update, :toggle_key => "key.to.toggle", :toggle_value => "on"

      output = JSON.parse response.body
      expect(response.status).to eq(200)
      expect(output["message"]).to eq("success")
    end

    it "should update the value of a specified key to false when sent 'off'" do
      expect(@feature_toggle_service).to receive(:changeValueOfToggle).with("key.to.toggle", false)

      post :update, :toggle_key => "key.to.toggle", :toggle_value => "off"

      output = JSON.parse response.body
      expect(response.status).to eq(200)
      expect(output["message"]).to eq("success")
    end

    it "should fail when the value is not provided" do
      expect(@feature_toggle_service).to_not receive(:changeValueOfToggle)

      post :update, :toggle_key => "key.to.toggle"

      output = JSON.parse response.body
      expect(response.status).to eq(422)
      expect(output["message"]).to eq("Value of property 'toggle_value' is invalid. Valid values are: 'on' and 'off'.")
    end

    it "should fail when the value is not 'on' or 'off'" do
      expect(@feature_toggle_service).to_not receive(:changeValueOfToggle)

      post :update, :toggle_key => "key.to.toggle", :toggle_value => "not_on_or_off"

      output = JSON.parse response.body
      expect(response.status).to eq(422)
      expect(output["message"]).to eq("Value of property 'toggle_value' is invalid. Valid values are: 'on' and 'off'.")
    end

    it "should fail when the updation fails with a runtime exception" do
      expect(@feature_toggle_service).to receive(:changeValueOfToggle).and_raise(java.lang.RuntimeException.new "Ouch. Something failed.")

      post :update, :toggle_key => "key.to.toggle", :toggle_value => "on"

      output = JSON.parse response.body
      expect(response.status).to eq(500)
      expect(output["message"]).to eq("Failed to change value of toggle. Message: Ouch. Something failed.")
    end

    it "should fail when the updation fails with anything else" do
      expect(@feature_toggle_service).to receive(:changeValueOfToggle).and_raise("Ouch. Something failed again.")

      post :update, :toggle_key => "key.to.toggle", :toggle_value => "on"

      output = JSON.parse response.body
      expect(response.status).to eq(500)
      expect(output["message"]).to eq("Failed to change value of toggle. Message: Ouch. Something failed again.")
    end
  end
end
