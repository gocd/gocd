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

describe HomeController do
  describe "index" do
    before :each do
      @system_environment = double('system environment')
      allow(@system_environment).to receive(:landingPage).and_return('/landingPage')
      allow(controller).to receive(:system_environment).and_return(@system_environment)
    end
    it "should resolve" do
      expect({:get => "/home"}).to route_to(:controller => "home", :action => "index")
    end

    it 'should redirect to landing page' do
      allow(controller).to receive(:url_for_path).with('/landingPage').and_return('/go/landingPage')
      get :index
      expect(response).to redirect_to('/go/landingPage')
    end

  end
end
