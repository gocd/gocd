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

require File.expand_path(File.dirname(__FILE__) + '/../../spec_helper')


describe "/agents/index" do



  it "should display number of agents by status" do
    assigns[:agents_enabled] = 10
    assigns[:agents_disabled] = 9
    assigns[:agents_pending] = 8
    render :partial => 'agents/agents_header'   

    response.should have_tag("ul.agent_counts") do
      with_tag "li.enabled", "Enabled: 10"
      with_tag "li.disabled", "Disabled: 9"
      with_tag "li.pending", "Pending: 8"
    end
  end

end
