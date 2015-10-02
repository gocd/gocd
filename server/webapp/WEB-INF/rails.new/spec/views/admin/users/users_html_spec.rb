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


describe "/admin/users/users.html.erb" do
  include GoUtil

  before(:each) do
    @users = [UserModel.new(User.new("foo", ["Foo", "fOO", "FoO"], "foo@cruise.go", true), ["user", "loser"], false),
                       UserModel.new(User.new("Bar", ["baR", "bAR", "BaR"], "bar@cruise.com", false), ["loser"], true)]
    assign(:users,@users)
    assign(:total_enabled_users,20)
    assign(:total_disabled_users,10)
  end

  it "should have the aggregrates on the page header" do
    render

    expect(response.body).to have_selector("h1", :text=>/User Summary/)
    Capybara.string(response.body).find("div ul.user_counts").tap do |div|
        expect(div).to have_selector("li.enabled", "Enabled: 20")
        expect(div).to have_selector("li.disabled", "Disabled: 10")
    end
  end

  it "should have title" do
    render
    view.instance_variable_get('@view_title').should == "Administration"
  end

  it "should render user details" do
    render

    Capybara.string(response.body).all("table[class='list_table sortable_table'] tr[class='user']").tap do |tr|
      expect(tr[0]).to have_selector("td[class='username'][title='foo'] span", :text=> "foo")
      expect(tr[0]).to have_selector("td[class='roles'][title='loser | user'] span", :text=> "loser | user")
      expect(tr[0]).to have_selector("td[class='aliases'][title='FoO | Foo | fOO'] span", :text=>"FoO | Foo | fOO")
      expect(tr[0]).to have_selector("td[class='is_admin'][title='No'] span", :text=> "No")
      expect(tr[0]).to have_selector("td[class='email'][title='foo@cruise.go'] span", :text=> "foo@cruise.go")
      expect(tr[0]).to have_selector("td[class='enabled'][title='Yes'] span", :text=> "Yes")

      expect(tr[1]).to have_selector("td[class='username'][title='Bar'] span", :text=> "Bar")
      expect(tr[1]).to have_selector("td[class='roles'][title='loser'] span", :text=> "loser")
      expect(tr[1]).to have_selector("td[class='aliases'][title='BaR | bAR | baR'] span", :text=>"BaR | bAR | baR")
      expect(tr[1]).to have_selector("td[class='is_admin'][title='Yes'] span", :text=> "Yes")
      expect(tr[1]).to have_selector("td[class='email'][title='bar@cruise.com'] span", :text=> "bar@cruise.com")
      expect(tr[1]).to have_selector("td[class='enabled'][title='Yes'] span", :text=> "Yes")

    end
  end

  it "should show sort links" do
      params[:column] = 'username'
      params[:order] = 'ASC'

      render

      Capybara.string(response.body).find("table.list_table.sortable_table tr.user_header").tap do |tr|
        expect(tr).to have_selector("th.username a[href='/admin/users?column=username&order=DESC']")
        expect(tr).to have_selector("th.roles a[href='/admin/users?column=roles&order=ASC']")
        expect(tr).to have_selector("th.aliases a[href='/admin/users?column=matchers&order=ASC']")
        expect(tr).to have_selector("th.is_admin a[href='/admin/users?column=is_admin&order=ASC']")
        expect(tr).to have_selector("th.email a[href='/admin/users?column=email&order=ASC']")
        expect(tr).to have_selector("th.enabled a[href='/admin/users?column=enabled&order=ASC']")

      end
  end

  it "should show 'Add Users' button" do
    render
    expect(response.body).to have_selector(".add_new_users a.link_as_button", :text=> "Add User")
  end

  it "should have a hidden field for operation for IE7 fix" do
    render
    Capybara.string(response.body).find("form#users_form").tap do |form|
      expect(form).to have_selector("input#operation[name='operation']")
    end
   end
end
