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


describe "/agents/index" do
  it "should display number of agents by status" do
    assign(:agents_enabled, 10)
    assign(:agents_disabled, 9)
    assign(:agents_pending, 8)
    render :partial => 'agents/agents_header'

    Capybara.string(response.body).find("ul.agent_counts").tap do |ul|
      expect(ul).to have_selector("li.enabled", :text=> "Enabled: 10")
      expect(ul).to have_selector("li.disabled", :text=> "Disabled: 9")
      expect(ul).to have_selector("li.pending", :text=> "Pending: 8")
    end
  end

end
