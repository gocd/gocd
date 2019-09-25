#
# Copyright 2019 ThoughtWorks, Inc.
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

describe "/value_stream_map/show_material.html.erb" do
  include GoUtil

  before(:each)  do
    in_params :material_fingerprint => 'fingerprint', :revision => "revision"
    allow(view).to receive(:can_view_admin_page?).and_return(true)
    allow(view).to receive(:is_user_an_admin?).and_return(true)
    allow(view).to receive(:is_user_a_group_admin?).and_return(true)
    allow(view).to receive(:is_user_authorized_to_view_templates?).and_return(true)
    allow(view).to receive(:current_user).and_return(Username.new(CaseInsensitiveString.new("user")))
  end

  describe "render html" do
    it "should render material name & revision for VSM page breadcrumb when it is available" do
      assign :material_display_name, "material_name"
      render

      Capybara.string(response.body).find("ul.entity_title").tap do |div|
        expect(div).to have_selector("li.name", :text=> "\n      Material\n      material_name")
        expect(div).to have_selector("li.last", :text=> "revision")
      end
    end

    it "should give VSM page a title with VSM of material-fingerprint" do
      assign(:user, double('username', :anonymous? => true))
      assign :material_display_name, "material_name"

      render :template => "value_stream_map/show_material.html.erb", :layout => 'layouts/value_stream_map'

      page = Capybara::Node::Simple.new(response.body)
      expect(page.title).to include("Value Stream Map for material_name")
    end
  end
end
