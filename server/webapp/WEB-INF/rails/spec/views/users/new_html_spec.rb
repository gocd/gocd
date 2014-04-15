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

describe "users/new.html" do

  it "should render the add user page with the search text box" do
    render "users/new.html"
    body = response.body
    body.should have_tag("form[action='#{users_search_path}']")
    body.should have_tag("div.user_search_box.form_item") do
      with_tag('input#search_id.searchbox')
      with_tag("label[for='search_text']", "Search for User:")
    end
  end

  it "should render the search button in a form" do
    render "users/new.html"
    body = response.body
    body.should have_tag("#search_user_submit", "SEARCH")
    body.should have_tag("div#search_results_container ") do
      with_tag("div #search_users_table")
      with_tag("table.list_table")
    end
  end

  it "should render the submit and close buttons in a form" do
    render "users/new.html"

    body = response.body
    body.should have_tag("form[action='#{users_create_path}']")
    body.should have_tag("div.actions #submit_add_user", "ADD USER")
    body.should have_tag("div.actions #close_add_user", "CLOSE")
  end
end
