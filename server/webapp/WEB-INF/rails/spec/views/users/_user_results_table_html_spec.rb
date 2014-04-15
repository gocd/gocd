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

describe "users/_user_results_table.html.erb" do
  it "should replace . in username with _dot_" do
    user_search_model = mock('user_search_model')
    user = User.new("username.with.dot", "display.name.with.dot", "email.with.dot@dot.com")
    user_search_model.should_receive(:getUser).at_least(:once).and_return(user)
    user_search_model.should_receive(:getUserSourceType).and_return(com.thoughtworks.go.presentation.UserSourceType::LDAP)

    render :partial => "users/user_results_table.html", :locals => {:scope => { :users => [user_search_model]}}

    assigns[:selection_field] = user
    response.body.should have_tag("input[type='radio'][name='selection'][id*='selection_button_user_username_dot_with_dot_dot']")
  end
end
