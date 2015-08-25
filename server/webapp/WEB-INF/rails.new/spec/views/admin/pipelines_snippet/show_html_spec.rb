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

describe "admin/pipelines_snippet/show.html.erb" do
  include ReflectiveUtil

  it "should render the group xml" do
    group_xml = "<foo></foo>"
    assign(:group_as_xml, group_xml)
    assign(:group_name, "foo")
    assign(:modifiable_groups, ["foo", "bar"])

    render

    Capybara.string(response.body).find('div#view_group').tap do |div|
      expect(div).to have_selector("pre#content_container", :text => "<foo></foo>") # user sees <foo></foo>
      expect(div).to have_selector("a.edit[href='#{pipelines_snippet_edit_path(:group_name => 'foo')}']", :text => 'Edit')
    end
    Capybara.string(response.body).find('div#modifiable_groups').tap do |div|
      expect(div).to have_selector("li.selected a.modifiable_group_link[href='#{pipelines_snippet_show_path(:group_name => 'foo')}']", :text => 'foo')
      expect(div).to have_selector("a.modifiable_group_link[href='#{pipelines_snippet_show_path(:group_name => 'bar')}']", :text => 'bar')
    end
  end
end
