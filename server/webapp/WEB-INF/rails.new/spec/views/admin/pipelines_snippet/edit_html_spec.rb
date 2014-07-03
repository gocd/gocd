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

describe "admin/pipelines_snippet/edit.html.erb" do
  include ReflectiveUtil

  it "should render the group xml" do
    group_xml = "<grp></grp>"
    group_name = "foo"
    assign(:group_as_xml, group_xml)
    assign(:group_name, group_name)
    assign(:config_md5, "md5")
    assign(:modifiable_groups, ["foo", "bar"])

    render "admin/pipelines_snippet/edit.html"

    response.body.should have_tag("div#edit_group") do
      with_tag("form[action='#{pipelines_snippet_update_path(:group_name => group_name)}']") do
        with_tag("input[type='hidden'][name='config_md5'][value='md5']")
        with_tag("a.cancel[href='#{pipelines_snippet_show_path(:group_name => group_name)}']", 'Cancel')
        with_tag("button.submit[id='save_config'][disabled='disabled']") do
          with_tag("span", "SAVE")
        end
        with_tag("textarea#content_container_for_edit", h(group_xml))
      end
    end
    response.body.should have_tag("div#modifiable_groups") do
      with_tag("li.selected a.modifiable_group_link[href='#{pipelines_snippet_show_path(:group_name => 'foo')}']", 'foo')
      with_tag("a.modifiable_group_link[href='#{pipelines_snippet_show_path(:group_name => 'bar')}']", 'bar')
    end
  end

  it "should render global errrors" do
    group_xml = "<grp></grp>"
    group_name = "foo"
    assign(:group_as_xml, group_xml)
    assign(:group_name, group_name)
    assign(:config_md5, "md5")
    assign(:modifiable_groups, ["foo", "bar"])
    assign(:errors, ['error1', 'error2'])

    render "admin/pipelines_snippet/edit.html"

    response.body.should have_tag("div.form_submit_errors") do
      with_tag("div.errors") do
        with_tag("li", "error1")
        with_tag("li", "error2")
      end
    end
  end
end
