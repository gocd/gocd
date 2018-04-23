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

describe AgentDetailsController do
  describe "routes" do
    it "should resolve the route to an agent" do
      expect(:get => "/agents/uuid").to route_to({:controller => "agent_details", :action => 'show',:uuid => "uuid"})
      expect(:get => agent_detail_path(uuid: "uuid")).to route_to({:controller => "agent_details", :action => 'show', :uuid => "uuid"})
    end

    it "should resolve the route to an job run history for an agent" do
      expect(:get => "/agents/uuid/job_run_history").to route_to({:controller => "agent_details", :action => 'job_run_history',:uuid => "uuid"})
      expect(:get => job_run_history_on_agent_path(uuid: "uuid")).to route_to({:controller => "agent_details", :action => 'job_run_history',:uuid => "uuid"})
    end
  end
end
