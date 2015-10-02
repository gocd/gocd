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

describe "/value_stream_map/show_material.html.erb" do
  before do
    stub_server_health_messages
  end
  include GoUtil

  before(:each)  do
    in_params :material_fingerprint => 'fingerprint', :revision => "revision"
    allow(view).to receive(:can_view_admin_page?).and_return(true)
    allow(view).to receive(:is_user_an_admin?).and_return(true)
  end

  describe "render html" do
    it "should render material fingerprint & revision for VSM page breadcrumb when it is available" do
      render

      Capybara.string(response.body).find("ul.entity_title").tap do |div|
        expect(div).to have_selector("li.name", :text=> "fingerprint")
        expect(div).to have_selector("li.last", :text=> "revision")
      end
    end

    it "should give VSM page a title with VSM of material-fingerprint" do
      assign(:user, double('username', :anonymous? => true))

      render :template => "value_stream_map/show_material.html.erb", :layout => 'layouts/value_stream_map'

      page = Capybara::Node::Simple.new(response.body)
      expect(page.title).to include("Value Stream Map of fingerprint")
    end
  end
end
