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

require File.join(File.dirname(__FILE__), "..", "..", "spec_helper")

describe '/admin/users.html.erb' do
  include GoUtil

  before(:each) do
    @users = [UserModel.new(User.new("foo", ["Foo", "fOO", "FoO"], "foo@cruise.go", true), ["user", "loser"], false),
                       UserModel.new(User.new("Bar", ["baR", "bAR", "BaR"], "bar@cruise.com", false), ["loser"], true)]
    assigns[:users] = @users
    assigns[:total_enabled_users] = 20
    assigns[:total_disabled_users] = 10
    assigns[:permitted_users] = 30
    class << template
      def url_for options
        "/admin/users?column=#{options[:column]}&order=#{options[:order]}"
      end
    end
  end
  
  it "should have the aggregrates on the page header" do
    render 'users/users.html'
    response.should have_tag("h1", /User Summary/)
    response.should have_tag("div ul.user_counts") do
        with_tag("li.enabled", "Enabled: 20")
        with_tag("li.disabled", "Disabled: 10")
        with_tag("li.license_usage", "License Usage: 20/30")
    end
  end

  it "should have title" do
    render 'users/users.html'
    template.instance_variable_get('@view_title').should == "Administration"
  end

  it "should render user details" do
    render 'users/users.html'
    response.should have_tag("table[class='list_table sortable_table']") do
      with_tag("tr[class='user']") do
        with_tag("td[class='username'][title='foo'] span", "foo")
        with_tag("td[class='roles'][title='loser | user'] span", "loser | user")
        with_tag("td[class='aliases'][title='FoO | Foo | fOO'] span", "FoO | Foo | fOO")
        with_tag("td[class='is_admin'][title='No'] span", "No")
        with_tag("td[class='email'][title='foo@cruise.go'] span", "foo@cruise.go")
        with_tag("td[class='enabled'][title='Yes'] span", "Yes")
      end

      with_tag("tr[class='user']") do
        with_tag("td[class='username'][title='Bar'] span", "Bar")
        with_tag("td[class='roles'][title='loser'] span", "loser")
        with_tag("td[class='aliases'][title='BaR | bAR | baR'] span", "BaR | bAR | baR")
        with_tag("td[class='is_admin'][title='Yes'] span", "Yes")
        with_tag("td[class='email'][title='bar@cruise.com'] span", "bar@cruise.com")
        with_tag("td[class='enabled'][title='Yes'] span", "Yes")
      end
    end
  end
  
  it "should show sort links" do
      params[:column] = 'username'
      params[:order] = 'ASC'
      render 'users/users.html'
      response.should have_tag('table.list_table.sortable_table tr.user_header') do
        with_tag('th.username a[href=/admin/users?column=username&order=DESC]')
        with_tag('th.roles a[href=/admin/users?column=roles&order=ASC]')
        with_tag('th.aliases a[href=/admin/users?column=matchers&order=ASC]')
        with_tag('th.is_admin a[href=/admin/users?column=is_admin&order=ASC]')
        with_tag('th.email a[href=/admin/users?column=email&order=ASC]')
        with_tag('th.enabled a[href=/admin/users?column=enabled&order=ASC]')
      end
  end

  it "should show 'Add Users' button" do
    render 'users/users.html'
    response.should have_tag('.add_new_users a.link_as_button', "Add User")
  end

  it "should have a hidden field for operation for IE7 fix" do
    render 'users/users.html'
    response.body.should have_tag("form#users_form") do
      with_tag("input#operation[name='operation']")
    end
  end
end