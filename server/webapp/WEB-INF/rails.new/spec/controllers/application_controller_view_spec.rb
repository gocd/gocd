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

describe NonApiController do
  before do
    draw_test_controller_route
  end

  describe "ParamEncoder" do
    it "should add before filter for relevant actions" do
      get :encoded_param_user_action, :decodable_param => "Zm9vL2Jhcg%3D%3D%0A"
      expect(assigns(:decodable_param)).to eq("foo/bar")
    end

    it "should not add before filter for excluded actions" do
      get :non_encoded_param_user_action, :decodable_param => "Zm9vL2Jhcg%3D%3D%0A"
      expect(assigns(:decodable_param)).to eq("Zm9vL2Jhcg%3D%3D%0A")
    end
  end

  describe "with view" do
    render_views
    it "should render erroneous responses" do
      get :not_found_action
      expect(response.status).to eq(404)
      expect(response.body).to have_selector("div.biggest", :text=>":(")
      expect(response.body).to have_selector("h3", :text=>"it was not found { description }")
      page = Capybara::Node::Simple.new(response.body)
      expect(page.title).to include("HTTP ERROR 404 - Go")
    end

    it "should render erroneous responses" do
      get :localized_not_found_action
      expect(response.status).to eq(404)
      expect(response.body).to have_selector("div.biggest", :text=>":(")
      expect(response.body).to have_selector("h3", :text=>"You do not have view permissions for pipeline 'mingle'.")
    end

    it "should fail with double_render error when not rendering error" do
      lambda do
        get :double_render_without_error
      end.should raise_error
    end
  end

  it "should render error using shared/error template" do
    get :not_found_action
    assert_template "shared/error"
    assert_template layout: :application
  end

  it "should render error using custom template if chosen" do
    controller.prepend_view_path 'spec/util/views'
    controller.error_template_for_request = 'crazy_error_template'
    get :not_found_action
    assert_template "crazy_error_template"
    assert_template layout: :application
  end
end

describe Api::TestController do
  render_views

  before do
    draw_test_controller_route
  end

  describe :disable_auto_refresh do
    it "should propagate autoRefresh=false" do
      get :auto_refresh, "autoRefresh" => "false"
      expect(response.body).to eq("http://test.host/?autoRefresh=false")
    end
  end

  describe :render_operation_result do
    it "should render 404 responses in error template" do
      get :not_found_action, :no_layout=>true
      expect(response.status).to eq(404)
      expect(response.body).to eq("it was not found { description }\n")
    end

    it "should render 401 responses in error template" do
      get :unauthorized_action, :no_layout=>true
      expect(response.status).to eq(401)
      expect(response.body).to eq("you are not allowed { description }\n")
    end
  end

  describe :render_operation_result_if_failure do
    it "should render 404 responses in error template" do
      get :another_not_found_action, :no_layout => true
      expect(response.status).to eq(404)
      expect(response.body).to eq("it was again not found { description }\n")
    end
  end

  it "should render erroneous responses" do
    get :localized_not_found_action, :no_layout=>true
    expect(response.status).to eq(404)
    expect(response.body).to eq("You do not have view permissions for pipeline 'mingle'." + "\n")
  end

  it "should render erroneous responses without appending an extra new-line in the end, if one already exists" do
    get :localized_not_found_action_with_message_ending_in_newline, :no_layout=>true
    expect(response.status).to eq(404)
    expect(response.body).to eq("Message with newline.\n")
  end

  it "should render responses when given operation-result without any message" do
    get :localized_operation_result_without_message, :no_layout=>true
    expect(response.status).to eq(200)
    expect(response.body).to eq(" ")
  end

  describe :unresolved do
    it "should resolve as action for any unmatched url" do
      expect(:get => "/cruise/foo/bar/baz/quux/hell/yeah?random=junk").to route_to({:controller => 'application', :action => "unresolved", :url => "cruise/foo/bar/baz/quux/hell/yeah", :random => "junk"})
    end

    it "should render a pretty payload with message" do
      @controller.stub(:url_for).and_return("foo/bar")
      get :unresolved
      expect(response.body).to have_selector("h3", :text=>"The url you are trying to reach appears to be incorrect.")
    end

    it "should render a 404" do
      get :unresolved
      expect(response.status).to eq(404)
    end

    it "should show the status in the response" do
      @controller.stub(:url_for).and_return("foo/bar")
      get :unresolved
      expect(response.body).to have_selector("div.biggest", :text=>":(")
    end
  end
end
