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

describe Api::ServerController do

  before :each do
    controller.stub(:populate_health_messages) do
      stub_server_health_messages
    end
    @system_environment = mock('system_environment')
    @go_config_service = mock('go_config_service')
    controller.stub!(:system_environment).and_return(@system_environment)
    controller.stub!(:go_config_service).and_return(@go_config_service)
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
      
      assigns[:base_url].should == :base_url
      assigns[:base_ssl_url].should == :base_ssl_url
      assigns[:artifacts_dir].should =~ /artifacts$/
      assigns[:shine_db_path].should =~ /shineDb$/
      assigns[:config_dir].should =~ /config$/
    end

    it "should return 401 if request is not from localhost" do
      @controller.should_receive(:request_from_localhost?).and_return(false)
      get :info, {:format => "xml", :no_layout => true}
      response.status.should == "401 Unauthorized"
    end

    it "should answer for /api/server/server.xml" do
      route_for(:controller => "api/server", :action => "info", "format"=>"xml", "no_layout"=>true).should == "/api/server.xml"
    end

    it "should answer for /api/server/server.xml" do
      params_from(:get, "/api/server.xml").should == {:controller => "api/server", :action => 'info', :format => "xml", :no_layout => true}
    end
  end

end
