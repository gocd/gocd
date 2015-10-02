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

describe "global errors on popups" do
  it "should escape error message" do
      configErrors = ConfigErrors.new
      configErrors.add("some field", "<h2>")
      assign(:errors, configErrors.getAll())

      render :partial => "admin/shared/global_errors.html.erb"

      expect(response.body).to have_selector("li.error", :text => "<h2>") #<h2> is visible to user.
  end
end
