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

require File.join(File.dirname(__FILE__), "..", "..", "..", "spec_helper")

describe "users/roles.html" do
  it "should render tristate-checkbox disabled when go-sys-adm checkbox is disabled" do
    assigns[:selections] = []
    assigns[:admin_selection] = admin_selection = TriStateSelection.new(com.thoughtworks.go.domain.config.Admin::GO_SYSTEM_ADMIN, TriStateSelection::Action.nochange, false)
    render "admin/users/roles.html"

    response.body.should have_tag(".hilighted .selectors .tristate_disabled_message", "The selected users have administrative privilege via other roles. To remove this privilege, remove them from all administrative roles.")
    response.body.should have_tag("select#field_for_#{admin_selection.object_id}[disabled='disabled']")
    response.body.should include("new TriStateCheckbox($('view_for_#{admin_selection.object_id}'), $('field_for_#{admin_selection.object_id}'), false);")
  end

  it "should render go-sys-adm checkbox" do
    assigns[:selections] = []
    assigns[:admin_selection] = admin_selection = TriStateSelection.new(com.thoughtworks.go.domain.config.Admin::GO_SYSTEM_ADMIN, TriStateSelection::Action.nochange)
    render "admin/users/roles.html"

    response.body.should_not have_tag(".hilighted .selectors .tristate_disabled_message")
    response.body.should_not have_tag("select#field_for_#{admin_selection.object_id}[name='selections[Go System Admin]']")
    response.body.should include("new TriStateCheckbox($('view_for_#{admin_selection.object_id}'), $('field_for_#{admin_selection.object_id}'), true);")
  end

  it "should render roles checkboxes" do
    assigns[:selections] = [foo = TriStateSelection.new("foo", TriStateSelection::Action.nochange),
                            bar = TriStateSelection.new("bar", TriStateSelection::Action.add)]
    assigns[:admin_selection] = TriStateSelection.new(com.thoughtworks.go.domain.config.Admin::GO_SYSTEM_ADMIN, TriStateSelection::Action.nochange)
    render "admin/users/roles.html"

    response.body.should have_tag("select#field_for_#{foo.object_id}")
    response.body.should include("new TriStateCheckbox($('view_for_#{foo.object_id}'), $('field_for_#{foo.object_id}'), true);")
    response.body.should have_tag("select#field_for_#{bar.object_id}")
    response.body.should include("new TriStateCheckbox($('view_for_#{bar.object_id}'), $('field_for_#{bar.object_id}'), true);")
  end
end