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

describe GoErrorsController do
  describe "inactive" do
    it "should resolve" do
      expect(get: "/errors/inactive").to route_to(controller: "go_errors", action: "inactive")
    end

    it 'should show the error page, with a message' do
      get :inactive

      expect(assigns[:message]).to start_with("Sorry, that operation is not allowed at this time")
      expect(assigns[:status]).to eq(503)
      expect(response).to render_template("shared/error")
    end
  end
end
