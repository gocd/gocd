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

describe "admin/users/_user_results_table.html.erb" do
  it "should replace . in username with _dot_" do
    user_search_model = double('user_search_model')
    user = User.new("username.with.dot", "display.name.with.dot", "email.with.dot@dot.com")
    user_search_model.should_receive(:getUser).at_least(:once).and_return(user)
    user_search_model.should_receive(:getUserSourceType).and_return(com.thoughtworks.go.presentation.UserSourceType::LDAP)

    render partial: "admin/users/user_results_table.html", locals: { scope: { users: [user_search_model] } }

    expect(response.body).to have_selector("input[type='radio'][name='selection'][id*='selection_button_user_username_dot_with_dot_dot']")
  end

  it "should santize all fields when generating a DOM ID, to handle fields with special characters" do
    allow(view).to receive(:random_dom_id) { |prefix| prefix + 'RAND' }

    user_search_model = double("user_search_model")
    user = User.new("!username-with_special_chars._@#\$%^&*()-_ and spaces", "!display-name-with_special_chars_@#\$%^&*()-_", "email!me#now*@dot.com")
    user_search_model.should_receive(:getUser).at_least(:once).and_return(user)
    user_search_model.should_receive(:getUserSourceType).and_return(com.thoughtworks.go.presentation.UserSourceType::LDAP)

    render partial: "admin/users/user_results_table.html", locals: { scope: { users: [user_search_model] } }

    expected_selection_id = "selection_button_user__username-with_special_chars_dot___________-__and_spacesRAND"
    expected_name_field_id = "name_user__username-with_special_chars_dot___________-__and_spacesRAND"
    expected_full_name_field_id = "display_name_user__display-name-with_special_chars__________-_RAND"
    expected_email_field_id = "email_user__username-with_special_chars_dot___________-__and_spacesRAND"

    expect(response.body).to have_content("jQuery(\'##{expected_selection_id}\').change(Util.disable_or_enable_submittable_fields" \
                                              "([\"#{expected_name_field_id}\",\"#{expected_full_name_field_id}\",\"#{expected_email_field_id}\"]));")

    expect(response.body).to have_selector("input[type='radio'][name='selection'][id='#{expected_selection_id}']")
    expect(response.body).to have_selector("input[type='hidden'][name='selections[][name]'][id='#{expected_name_field_id}']")
    expect(response.body).to have_selector("input[type='hidden'][name='selections[][full_name]'][id='#{expected_full_name_field_id}']")
    expect(response.body).to have_selector("input[type='hidden'][name='selections[][email]'][id='#{expected_email_field_id}']")
  end
end
