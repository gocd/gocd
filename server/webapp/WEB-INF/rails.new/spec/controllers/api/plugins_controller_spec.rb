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

describe Api::PluginsController do

  before :each do
    @system_environment = double('system_environment')
    controller.stub(:system_environment).and_return(@system_environment)
  end

  it "should answer to /api/plugins/status" do
    expect(:get => '/api/plugins/status').to route_to(:action => "status", :controller => 'api/plugins', :no_layout => true)
  end

  it "should return plugin status as false when not set" do
    @system_environment.should_receive(:pluginStatus).and_return(GoConstants::ENABLE_PLUGINS_RESPONSE_FALSE)
    get :status, {:no_layout => true}
    expect(response.body).to eq("disabled")
    end

  it "should return plugin status as true when set" do
    @system_environment.should_receive(:pluginStatus).and_return(GoConstants::ENABLE_PLUGINS_RESPONSE_TRUE)
    get :status, {:no_layout => true}
    expect(response.body).to eq("enabled")
  end
end
