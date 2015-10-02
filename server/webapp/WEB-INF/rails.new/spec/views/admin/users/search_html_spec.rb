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


describe "/admin/users/search.html.erb" do
  it "should have the error message container" do
   assign(:users,[])

   render

   expect(response.body).to have_selector("span#add_error_message")
  end

  it "should render search results" do
    assign(:users,[
          UserSearchModel.new(User.new("foo", "Mr Foo", "foo@cruise.go"), UserSourceType::LDAP),
          UserSearchModel.new(User.new("Bar", "Mr Bar", "bar@cruise.com"), UserSourceType::PASSWORD_FILE)])

    render

    Capybara.string(response.body).all("table[class='list_table'] tr[class='user']").tap do |tr|

        expect(tr[0]).to have_selector("td[class='username'][title='Mr Foo'] span", :text=> "Mr Foo")
        expect(tr[0]).to have_selector("td[class='fullname'][title='foo'] span", :text=> "foo")
        expect(tr[0]).to have_selector("td[class='email'][title='foo@cruise.go'] span", :text=> "foo@cruise.go")
        expect(tr[0]).to have_selector("td[class='source'][title='LDAP'] span", :text=> "LDAP")

        expect(tr[1]).to have_selector("td[class='username'][title='Mr Bar'] span", :text=> "Mr Bar")
        expect(tr[1]).to have_selector("td[class='fullname'][title='Bar'] span", :text=> "Bar")
        expect(tr[1]).to have_selector("td[class='email'][title='bar@cruise.com'] span", :text=> "bar@cruise.com")
        expect(tr[1]).to have_selector("td[class='source'][title='Password File'] span", :text=> "Password File")
    end
  end
end
