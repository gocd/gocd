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


describe "/admin/users/new.html.erb" do

  it "should render the add user page with the search text box" do
    render

    expect(response.body).to have_selector("form[action='#{users_search_path}']")
    Capybara.string(response.body).find("div.user_search_box.form_item").tap do |div|
      expect(div).to have_selector("input#search_id.searchbox")
      expect(div).to have_selector("label[for='search_text']", :text=>"Search for User:")
    end
  end

  it "should render the search button in a form" do
    render

    expect(response.body).to have_selector("#search_user_submit",:text=>"SEARCH")
    Capybara.string(response.body).find("div#search_results_container").tap do |div|
      expect(div).to have_selector("div#search_users_table")
      expect(div).to have_selector("table.list_table")
    end
  end

  it "should render the submit and close buttons in a form" do
    render

    expect(response.body).to have_selector("form[action='#{users_create_path}']")
    expect(response.body).to have_selector("div.actions #submit_add_user", :text=>"ADD USER")
    expect(response.body).to have_selector("div.actions #close_add_user", :text=>"CLOSE")
  end
end
