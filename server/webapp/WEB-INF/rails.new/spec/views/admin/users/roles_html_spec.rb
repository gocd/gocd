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


describe "/admin/users/roles.html.erb" do
  it "should render tristate-checkbox disabled when go-sys-adm checkbox is disabled" do
    assign(:selections,[])
    admin_selection = TriStateSelection.new(com.thoughtworks.go.domain.config.Admin::GO_SYSTEM_ADMIN, TriStateSelection::Action.nochange, false)
    assign(:admin_selection, admin_selection)

    render

    expect(response.body).to have_selector(".hilighted .selectors .tristate_disabled_message", :text=>"The selected users have administrative privilege via other roles. To remove this privilege, remove them from all administrative roles.")
    expect(response.body).to have_selector("select#field_for_#{admin_selection.object_id}[disabled='disabled']")
    expect(response.body).to include("new TriStateCheckbox($('view_for_#{admin_selection.object_id}'), $('field_for_#{admin_selection.object_id}'), false);")
  end

  it "should render go-sys-adm checkbox" do
    assign(:selections,[])
    admin_selection = TriStateSelection.new(com.thoughtworks.go.domain.config.Admin::GO_SYSTEM_ADMIN, TriStateSelection::Action.nochange)
    assign(:admin_selection, admin_selection)

    render

    expect(response.body).not_to have_selector(".hilighted .selectors .tristate_disabled_message")
    expect(response.body).not_to have_selector("select#field_for_#{admin_selection.object_id}[name='selections[Go System Admin]']")
    expect(response.body).to include("new TriStateCheckbox($('view_for_#{admin_selection.object_id}'), $('field_for_#{admin_selection.object_id}'), true);")
  end

  it "should render roles checkboxes" do
    foo = TriStateSelection.new("foo", TriStateSelection::Action.nochange)
    bar = TriStateSelection.new("bar", TriStateSelection::Action.add)
    assign(:selections,[foo,bar])
    assign(:admin_selection,TriStateSelection.new(com.thoughtworks.go.domain.config.Admin::GO_SYSTEM_ADMIN, TriStateSelection::Action.nochange))

    render

    expect(response.body).to have_selector("select#field_for_#{foo.object_id}")
    expect(response.body).to include("new TriStateCheckbox($('view_for_#{foo.object_id}'), $('field_for_#{foo.object_id}'), true);")
    expect(response.body).to have_selector("select#field_for_#{bar.object_id}")
    expect(response.body).to include("new TriStateCheckbox($('view_for_#{bar.object_id}'), $('field_for_#{bar.object_id}'), true);")
  end
end
