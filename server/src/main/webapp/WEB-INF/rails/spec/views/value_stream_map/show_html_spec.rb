#
# Copyright 2020 ThoughtWorks, Inc.
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


describe "/value_stream_map/show.html.erb" do
  include GoUtil

  before(:each)  do
    in_params :pipeline_name => 'P1', :pipeline_counter => "1"
    allow(view).to receive(:url_for_pipeline).with('P1').and_return('link_for_P1')
    allow(view).to receive(:can_view_admin_page?).and_return(true)
    allow(view).to receive(:is_user_an_admin?).and_return(true)
    allow(view).to receive(:is_user_a_group_admin?).and_return(true)
    allow(view).to receive(:is_user_authorized_to_view_templates?).and_return(true)
    allow(view).to receive(:current_user).and_return(Username.new(CaseInsensitiveString.new("user")))
  end

  describe "render html" do
    it "should render pipeline label for VSM page breadcrumb when it is available" do
      pipeline = double('pipeline instance')
      expect(pipeline).to receive(:getLabel).and_return('test')
      assign(:pipeline, pipeline)

      render

      Capybara.string(response.body).find("ul.entity_title").tap do |div|
        div.find("li.name").tap do |li|
          expect(li).to have_selector("a[href='link_for_P1']", :text=> "P1")
        end
        div.find("li.last").tap do |li|
          expect(li).to have_selector("h1", :text=> "test")
        end
      end
    end

    it "should render pipeline counter for VSM page breadcrumb when pipeline label is not available" do
      assign(:pipeline, nil)

      render

      Capybara.string(response.body).find("ul.entity_title").tap do |div|
        div.find("li.name").tap do |li|
          expect(li).to have_selector("a[href='link_for_P1']", :text=> "P1")
        end
        div.find("li.last").tap do |li|
          expect(li).to have_selector("h1", :text=> "1")
        end
      end
    end
  end

end
