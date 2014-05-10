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

require File.expand_path(File.dirname(__FILE__) + '/../spec_helper')

describe AgentAPIModel do
  before(:each) do
    @agent_view_model = double(AgentViewModel)
    @agent_view_model.stub!(:getUuid).and_return("uuid3")
    @agent_view_model.stub!(:getHostname).and_return("CCeDev01")
    @agent_view_model.stub!(:getIpAddress).and_return("127.0.0.1")
    @agent_view_model.stub!(:getLocation).and_return("/var/lib/go-server")
    @agent_view_model.stub!(:getStatusForDisplay).and_return("Idle")
    @agent_view_model.stub!(:buildLocator).and_return("/pipeline/1/stage/1/job")
    @agent_view_model.stub!(:getOperatingSystem).and_return("Linux")
    disk_space = DiskSpace.new(0)
    @agent_view_model.stub!(:freeDiskSpace).and_return(disk_space)
    resources_arr = Array.new
    resources_arr << "java"
    @agent_view_model.stub!(:getResources).and_return(resources_arr)
    environments_arr = Array.new
    environments_arr << "foo"
    @agent_view_model.stub!(:getEnvironments).and_return(environments_arr)
  end

  describe "should initialize correctly" do
    it "should populate correct data" do
      agent_api = AgentAPIModel.new(@agent_view_model)
      agent_api.uuid.should == "uuid3"
      agent_api.agent_name.should == "CCeDev01"
      agent_api.ip_address.should == "127.0.0.1"
      agent_api.sandbox.should == "/var/lib/go-server"
      agent_api.status.should == "Idle"
      agent_api.build_locator.should == "/pipeline/1/stage/1/job"
      agent_api.os.should == "Linux"
      agent_api.free_space.should == "0 bytes"
      agent_api.resources[0].should == "java"
      agent_api.environments[0].should == "foo"
    end
  end

  describe "should convert to json correctly" do
    it "should have all fields correctly" do
      agents_api_arr = Array.new
      agents_api_arr << AgentAPIModel.new(@agent_view_model)
      ActiveSupport::JSON.decode(agents_api_arr.to_json).should == [
        {
          "agent_name" => "CCeDev01",
          "free_space"=> "0 bytes",
          "uuid" => "uuid3",
          "sandbox" => "/var/lib/go-server",
          "status" => "Idle",
          "environments" => ["foo"],
          "os" => "Linux",
          "resources" => ["java"],
          "ip_address" => "127.0.0.1",
          "build_locator" => "/pipeline/1/stage/1/job"
        }
      ]
    end
  end
end