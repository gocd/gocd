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

module Api
  describe FeatureToggleAPIModel do
    describe "initialization" do
      it "should populate correct data" do
        toggles = FeatureToggleMother.someToggles().all()

        toggle_api_model = FeatureToggleAPIModel.new(toggles[0])

        expect(toggle_api_model.key).to eq("key1")
        expect(toggle_api_model.description).to eq("desc1")
        expect(toggle_api_model.value).to eq(true)
        expect(toggle_api_model.has_changed).to eq(false)
      end
    end
  end
end
