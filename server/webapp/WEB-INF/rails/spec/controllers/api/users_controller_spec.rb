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
require 'rexml/document'
require 'rexml/xpath'

describe Api::UsersController do
  include JavaImports

  describe "destroy" do
    describe :routes do
      it "should resolve route" do
        params_from(:delete, "/api/users/user.name").should == {:controller => "api/users", :action => "destroy", :username => "user.name", :no_layout => true}
      end
    end

    describe :destroy do
      before :each do
        @user_service = mock('user service')
        @user = mock('user')
        @security_service = mock('security service')
        controller.stub!(:user_service).and_return(@user_service)
        controller.stub!(:security_service).and_return(@security_service)
        controller.stub!(:current_user).and_return(@user)
      end

      it "should return 401 when user is not admin" do
        @security_service.should_receive(:isUserAdmin).with(@user).and_return(false)
        localizer = mock('localizer')
        localizer.should_receive(:string).with("API_ACCESS_UNAUTHORIZED").and_return('blah blah')
        controller.stub!(:l).and_return(localizer)
        delete :destroy, {:username => 'random', :no_layout => true}
        response.status.should == '401 Unauthorized'
        response.body.should == 'blah blah'
      end

      it "should return error code from result in case user is not found" do
        @security_service.should_receive(:isUserAdmin).with(@user).and_return(true)
        username = "user.name"
        @user_service.should_receive(:deleteUser).with(username, an_instance_of(HttpLocalizedOperationResult)) do |username, result|
          result.notFound(LocalizedMessage.string("USER_NOT_FOUND", [username].to_java(java.lang.String)), HealthStateType.general(HealthStateScope::GLOBAL))
        end
        delete :destroy, {:username => username, :no_layout => true}
        response.status.should == '404 Not Found'
        response.body.should == "User '#{username}' not found."
      end

      it "should return error code from result in case user is not disabled" do
        @security_service.should_receive(:isUserAdmin).with(@user).and_return(true)
        username = "user.name"
        @user_service.should_receive(:deleteUser).with(username, an_instance_of(HttpLocalizedOperationResult)) do |username, result|
          result.badRequest(LocalizedMessage.string("USER_NOT_DISABLED", [username].to_java(java.lang.String)))
        end
        delete :destroy, {:username => username, :no_layout => true}
        response.status.should == '400 Bad Request'
        response.body.should == "User '#{username}' is not disabled."
      end

      it "should return success when user is deleted" do
        @security_service.should_receive(:isUserAdmin).with(@user).and_return(true)
        username = "user.name"
        @user_service.should_receive(:deleteUser).with(username, an_instance_of(HttpLocalizedOperationResult)) do |username, result|
          result.setMessage(LocalizedMessage.string("USER_DELETE_SUCCESSFUL", [username].to_java(java.lang.String)))
        end
        delete :destroy, {:username => username, :no_layout => true}
        response.status.should == '200 OK'
        response.body.should == "User '#{username}' was deleted successfully."
      end
    end
  end

  describe "index" do

    before :each do
      @user_service = mock("user_service")
      controller.stub(:user_service).and_return(@user_service)
      controller.stub(:set_current_user)
    end

    it "should answer to /api/users.xml" do
      params_from(:get, "/api/users.xml").should == {:action => "index", :controller => 'api/users', :format=>"xml", :no_layout => true}
    end

    it "should return a document with a users element at the root level" do
      @user_service.should_receive(:allUsers).and_return([])
      get :index, :format => "xml", :no_layout => true
      assert_equal 1, match_response_path("/users").size
    end

    it "should return a document with a user element for every user in the system" do
      @user_service.should_receive(:allUsers).and_return([User.new("admin"), User.new("jay"), User.new("silent_bob")])
      get :index, :format => "xml", :no_layout => true
      assert_equal 3, match_response_path("/users/user").size
    end

    it "should return all relevant user information for each user" do
      silent_bob = User.new("silent_bob", "Kevin Smith", ["bob", "kevin"].to_java(java.lang.String), "ksmith@example.com", true)
      silent_bob.disable
      @user_service.should_receive(:allUsers).and_return([silent_bob])

      get :index, :format => "xml", :no_layout => true
      assert_equal "silent_bob", match_response_path("/users/user/name/text()").join
      assert_equal "Kevin Smith", match_response_path("/users/user/displayName/text()").join
      assert_equal "ksmith@example.com", match_response_path("/users/user/email/text()").join
      assert_equal "bob,kevin", match_response_path("/users/user/matcher/text()").join
      assert_equal "true", match_response_path("/users/user/emailMe/text()").join
      assert_equal "false", match_response_path("/users/user/enabled/text()").join
    end

    def match_response_path(path)
      assert_not_nil assigns[:doc]
      response_document = REXML::Document.new(as_xml(assigns[:doc]))
      REXML::XPath.match(response_document, path)
    end

    def as_xml(document)
      stream = ByteArrayOutputStream.new
      XMLWriter.new(stream, OutputFormat.createPrettyPrint()).write(document)
      stream.toString()
    end

  end
end