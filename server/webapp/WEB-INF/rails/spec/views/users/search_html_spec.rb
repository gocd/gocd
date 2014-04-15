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

describe "users/search.html" do
  it "should have the error message container" do
   assigns[:users] = []
   render "users/search.html"

   response.body.should have_tag("span#add_error_message")
 end

  it "should render search results" do
    assigns[:users] = [UserSearchModel.new(User.new("foo", "Mr Foo", "foo@cruise.go"), UserSourceType::LDAP),
                       UserSearchModel.new(User.new("Bar", "Mr Bar", "bar@cruise.com"), UserSourceType::PASSWORD_FILE)]

    render 'users/search.html'

    response.should have_tag("table[class='list_table']") do
      with_tag("tr[class='user']") do
        with_tag("td[class='username'][title='Mr Foo'] span", "Mr Foo")
        with_tag("td[class='fullname'][title='foo'] span", "foo")
        with_tag("td[class='email'][title='foo@cruise.go'] span", "foo@cruise.go")
        with_tag("td[class='source'][title='LDAP'] span", "LDAP")
      end

      with_tag("tr[class='user']") do
        with_tag("td[class='username'][title='Mr Bar'] span", "Mr Bar")
        with_tag("td[class='fullname'][title='Bar'] span", "Bar")
        with_tag("td[class='email'][title='bar@cruise.com'] span", "bar@cruise.com")
        with_tag("td[class='source'][title='Password File'] span", "Password File")
      end
    end
  end
end