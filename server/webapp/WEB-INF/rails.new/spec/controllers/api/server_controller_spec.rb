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

describe Api::ServerController do

  before :each do
    controller.stub(:populate_health_messages) do
      stub_server_health_messages_for_controllers
    end
    @system_environment = double('system_environment')
    @go_config_service = double('go_config_service')
    controller.stub(:system_environment).and_return(@system_environment)
    controller.stub(:go_config_service).and_return(@go_config_service)
    @controller.request.env["SERVER_NAME"] = "server_name"
    @controller.request.env["REMOTE_ADDR"] = "client_ip"
    @controller.stub(:request_from_localhost?).and_return(true)
    controller.stub(:populate_config_validity)
  end

  describe "server info" do
    it "should assign server_info variables" do
      @system_environment.should_receive(:getBaseUrlForShine).and_return(:base_url)
      @system_environment.should_receive(:getBaseSslUrlForShine).and_return(:base_ssl_url)
      @system_environment.should_receive(:shineDb).and_return(java.io.File.new("shineDb"))
      @system_environment.should_receive(:configDir).and_return(java.io.File.new("config"))
      @go_config_service.should_receive(:artifactsDir).and_return(java.io.File.new("artifacts"))

      get :info, {:format => "xml", :no_layout => true}

      expect(assigns[:base_url]).to eq(:base_url)
      expect(assigns[:base_ssl_url]).to eq(:base_ssl_url)
      expect(assigns[:artifacts_dir]).to match(/artifacts$/)
      expect(assigns[:shine_db_path]).to  match(/shineDb$/)
      expect(assigns[:config_dir]).to match(/config$/)
    end

    it "should return 401 if request is not from localhost" do
      @controller.should_receive(:request_from_localhost?).and_return(false)
      get :info, {:format => "xml", :no_layout => true}
      expect(response.status).to eq(401)
    end

    it "should answer for /api/server.xml" do
      expect(:get => 'api/server.xml').to route_to(:controller => "api/server", :action => 'info', :format => "xml", :no_layout => true)
      expect(controller.send(:server_path)).to eq("/api/server.xml")
    end
  end
end
