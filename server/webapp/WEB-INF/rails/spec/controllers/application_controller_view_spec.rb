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

describe NonApiController do
  before do
    draw_test_controller_route
  end

  describe "ParamEncoder" do
    it "should add before filter for relevant actions" do
      get :encoded_param_user_action, :decodable_param => "Zm9vL2Jhcg%3D%3D%0A"
      assigns[:decodable_param].should == "foo/bar"
    end

    it "should not add before filter for excluded actions" do
      get :non_encoded_param_user_action, :decodable_param => "Zm9vL2Jhcg%3D%3D%0A"
      assigns[:decodable_param].should == "Zm9vL2Jhcg%3D%3D%0A"
    end
  end

  describe "with view" do
    integrate_views
    it "should render erroneous responses" do
      get :not_found_action
      response.status.should == "404 Not Found"
      response.should have_tag("div.biggest", ":(")
      response.should have_tag("h3", "it was not found { description }")
      response.body.should have_tag('head title', "HTTP ERROR 404 - Go")
    end

    it "should render erroneous responses" do
      get :localized_not_found_action
      response.status.should == "404 Not Found"
      response.should have_tag("div.biggest", ":(")
      response.should have_tag("h3", "You do not have view permissions for pipeline 'mingle'.")
    end

    it "should render error message with status code for exceptions" do
      get :exception_out
      response.should have_tag("div.biggest", ":(")
      response.body.should have_tag("h3", "Server error occured. Check log for details.")
      response.status.should == "500 Internal Server Error"
      response.body.should have_tag('head title', "HTTP ERROR 500 - Go")
    end

    it "should render once and ignore second render" do
      get :double_render
      response.body.should == "first render"
      response.status.should == "200 OK"
    end

    it "should honor render and ignore redirect after that" do
      get :redirect_after_render
      response.body.should == "render before redirect"
      response.status.should == "200 OK"
      controller.instance_variable_get('@exception_in_action').should be_nil
    end

    it "should fail with double_render error when not rendering error" do
      lambda do
        get :double_render_without_error
      end.should raise_error
    end
  end

  it "should render error using shared/error template" do
    controller.should_receive_render_with('shared/error')
    get :not_found_action
  end

  it "should render error using custom template if chosen" do
    controller.error_template_for_request = 'crazy_error_template'
    controller.should_receive_render_with('crazy_error_template')
    get :not_found_action
  end
end

describe Api::TestController do
  integrate_views

  before do
    draw_test_controller_route
  end

  describe :disable_auto_refresh do
    it "should propagate autoRefresh=false" do
      get :auto_refresh, "autoRefresh" => "false"
      response.body.should == "http://test.host/?autoRefresh=false"
    end
  end

  describe :render_operation_result do
    it "should render 404 responses in error template" do
      get :not_found_action, :no_layout=>true
      response.status.should == "404 Not Found"
      response.body.should == "it was not found { description }\n"
    end

    it "should render 401 responses in error template" do
      get :unauthorized_action, :no_layout=>true
      response.status.should == "401 Unauthorized"
      response.body.should == "you are not allowed { description }\n"
    end
  end

  describe :render_operation_result_if_failure do
    it "should render 404 responses in error template" do
      get :another_not_found_action, :no_layout => true

      response.status.should == "404 Not Found"
      response.body.should == "it was again not found { description }\n"
    end
  end

  it "should render erroneous responses" do
    get :localized_not_found_action, :no_layout=>true
    response.status.should == "404 Not Found"
    response.body.should == "You do not have view permissions for pipeline 'mingle'." + "\n"
  end

  it "should render erroneous responses without appending an extra new-line in the end, if one already exists" do
    get :localized_not_found_action_with_message_ending_in_newline, :no_layout=>true
    response.status.should == "404 Not Found"
    response.body.should == "Message with newline.\n"
  end

  it "should render responses when given operation-result without any message" do
    get :localized_operation_result_without_message, :no_layout=>true
    response.status.should == "200 OK"
    response.body.should == ' '
  end

  describe :unresolved do
    it "should resolve as action for any unmatched url" do
      params_from(:get, "/cruise/foo/bar/baz/quux/hell/yeah?random=junk").should == {:controller => 'application', :action => "unresolved", :url => %w{cruise foo bar baz quux hell yeah}, :random => "junk"}
    end

    it "should render a pretty payload with message" do
      @controller.stub(:url_for).and_return("foo/bar")
      get :unresolved
      response.should have_tag("h3", "The url [ foo/bar ] you are trying to reach doesn't appear to be correct.")
    end

    it "should render a 404" do
      get :unresolved
      response.status.should == "404 Not Found"
    end

    it "should show the status in the response" do
      @controller.stub(:url_for).and_return("foo/bar")
      get :unresolved
      response.should have_tag("div.biggest", ":(")
    end

  end

end
