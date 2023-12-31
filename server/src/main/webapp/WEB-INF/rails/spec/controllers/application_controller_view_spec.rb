#
# Copyright 2024 Thoughtworks, Inc.
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
#

require 'rails_helper'

describe NonApiController do
  before do
    draw_test_controller_route
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
      expect do
        get :double_render_without_error
      end.to raise_error(AbstractController::DoubleRenderError)
    end
  end

  it "should render error using shared/error template" do
    get :not_found_action
    assert_template "shared/error"
    assert_template layout: :application
  end

  it "should render error using custom template if chosen" do
    controller.prepend_view_path 'spec/support/util/views'
    controller.error_template_for_request = 'crazy_error_template'
    get :not_found_action
    assert_template "crazy_error_template"
    assert_template layout: :application
  end
end
