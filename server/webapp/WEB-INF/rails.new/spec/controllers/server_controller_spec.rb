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

describe ServerController do
  before :each do
    controller.stub(:populate_config_validity)
  end

  it "should resolve json url for messages" do
    expect({:get => "/server/messages.json"}).to route_to(:controller => "server", :action => "messages", :format => "json")
  end

  it "should obtain the error and warning counts" do
    first = ServerHealthState.error("first error", "first description", HealthStateType.invalidConfig())
    second = ServerHealthState.error("second error", "second description", HealthStateType.invalidConfig())
    third = ServerHealthState.warning("first warning", "third description", HealthStateType.artifactsDirChanged())
    states = ServerHealthStates.new([first, second, third])
    config = BasicCruiseConfig.new()

    @server_health_service = double('server health service')
    @go_config_service = double('go config service')
    controller.stub(:server_health_service).and_return(@server_health_service)
    controller.stub(:go_config_service).and_return(@go_config_service)

    @go_config_service.should_receive(:getCurrentConfig).and_return(config)
    @server_health_service.should_receive(:getAllValidLogs).with(config).and_return(states)

    get 'messages', :format => 'json'

    assigns[:current_server_health_states].should == states
  end
end
