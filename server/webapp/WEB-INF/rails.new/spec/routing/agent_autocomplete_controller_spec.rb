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

describe AgentAutocompleteController do
  describe "routes" do
    it "should resolve the path" do
      expect(:get => '/agents/filter_autocomplete/resource').to route_to(:controller => "agent_autocomplete", :action => 'resource')
      expect(agent_filter_autocomplete_os_path(:action => "os")).to eq("/agents/filter_autocomplete/os")
    end

    it "should accept only the defined actions" do
      expect(:get => "agents/filter_autocomplete/foo").to route_to(:controller => "application", :action => 'unresolved', :url => "agents/filter_autocomplete/foo")
    end
  end
end
