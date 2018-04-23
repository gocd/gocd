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

describe AgentAutocompleteController do

  before :each do
    allow(controller).to receive(:go_config_service).and_return(@go_config_service = double("go_config_service"))
    allow(controller).to receive(:environment_config_service).and_return(@environment_config_service = double("environment_config_service"))
    allow(controller).to receive(:agent_service).and_return(@agent_service = double("agent_service"))
  end

  describe "actions" do
    before do
      allow(@go_config_service).to receive(:checkConfigFileValid).and_return(com.thoughtworks.go.config.validation.GoConfigValidity.valid())
    end

    describe "resource" do
      it "should return all resources starting with given query string" do
        expect(@go_config_service).to receive(:getResourceList).and_return(java.util.Arrays.asList(["linux", "windows"].to_java(java.lang.String)))
        get :resource, params: { :q => "li" }

        expect(response.body).to eq("linux")
      end
    end

    describe "status" do
      it "should return all types of agent status" do
        get :status, params: { :q => "" }
        expect(response.body).to eq("pending\nlostcontact\nmissing\nbuilding\ncancelled\nidle\ndisabled")
      end

      it "should return agent status starting with given query string" do
        get :status, params: { :q => "bu" }
        expect(response.body).to eq("building")
      end

      it "should return case insensitive status starting with given query string" do
        get :status, params: { :q => "bUi" }
        expect(response.body).to eq("building")
      end
    end

    describe "environment" do
      it "should return all environments" do
        env_list = java.util.Arrays.asList([CaseInsensitiveString.new("prod"), CaseInsensitiveString.new("testing"), CaseInsensitiveString.new("staging")].to_java(CaseInsensitiveString))
        expect(@environment_config_service).to receive(:environmentNames).and_return(env_list)
        get :environment, params: { :q => "" }
        expect(response.body).to eq("prod\ntesting\nstaging")
      end

      it "should return all environments starting with a query string" do
        env_list = java.util.Arrays.asList([CaseInsensitiveString.new("prod"), CaseInsensitiveString.new("testing"), CaseInsensitiveString.new("staging")].to_java(CaseInsensitiveString))
        expect(@environment_config_service).to receive(:environmentNames).and_return(env_list)
        get :environment, params: { :q => "test" }
        expect(response.body).to eq("testing")
      end
    end

    describe "name" do
      it "should return all unique agent hostnames" do
        expect(@agent_service).to receive(:getUniqueAgentNames).and_return(java.util.Arrays.asList(["dev-agent", "test-linux-agent", "test-win-agent"].to_java(java.lang.String)))

        get :name, params: { :q => "" }
        expect(response.body).to eq("dev-agent\ntest-linux-agent\ntest-win-agent")
      end

      it "should return all unique agent hostnames starting with given query string" do
        expect(@agent_service).to receive(:getUniqueAgentNames).and_return(java.util.Arrays.asList(["dev-agent", "test-linux-agent", "test-win-agent"].to_java(java.lang.String)))

        get :name, params: { :q => "te" }
        expect(response.body).to eq("test-linux-agent\ntest-win-agent")
      end
    end

    describe "ip" do
      it "should return all agent ip addresses" do
        expect(@agent_service).to receive(:getUniqueIPAddresses).and_return(java.util.Arrays.asList(["10.11.12.13", "11.13.14.15"].to_java(java.lang.String)))

        get :ip, params: { :q => "" }
        expect(response.body).to eq("10.11.12.13\n11.13.14.15")
      end

      it "should return all agent ip addresses starting with a given query string" do
        expect(@agent_service).to receive(:getUniqueIPAddresses).and_return(java.util.Arrays.asList(["10.11.12.13", "11.13.14.15"].to_java(java.lang.String)))

        get :ip, params: { :q => "11." }
        expect(response.body).to eq("11.13.14.15")
      end
    end

    describe "os" do
      it "should return all agent operating systems" do
        expect(@agent_service).to receive(:getUniqueAgentOperatingSystems).and_return(java.util.Arrays.asList(["linux", "windows"].to_java(java.lang.String)))

        get :os, params: { :q => "" }
        expect(response.body).to eq("linux\nwindows")
      end

      it "should return all agent operating systems starting with a given query string" do
        expect(@agent_service).to receive(:getUniqueAgentOperatingSystems).and_return(java.util.Arrays.asList(["linux", "windows"].to_java(java.lang.String)))

        get :os, params: { :q => "li" }
        expect(response.body).to eq("linux")
      end
    end
  end
end
