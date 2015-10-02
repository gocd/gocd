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

describe "admin/pipelines_snippet/edit.html.erb" do
  include ReflectiveUtil

  it "should render the group xml" do
    group_xml = "<grp></grp>"
    group_name = "foo"
    assign(:group_as_xml, group_xml)
    assign(:group_name, group_name)
    assign(:config_md5, "md5")
    assign(:modifiable_groups, ["foo", "bar"])

    render

    Capybara.string(response.body).find('div#edit_group').tap do |div|
      div.find("form[action='#{pipelines_snippet_update_path(:group_name => group_name)}']").tap do |form|
        expect(form).to have_selector("input[type='hidden'][name='config_md5'][value='md5']")
        expect(form).to have_selector("a.cancel[href='#{pipelines_snippet_show_path(:group_name => group_name)}']", 'Cancel')
        form.find("button.submit[id='save_config'][disabled='disabled']").tap do |button|
          expect(button).to have_selector("span", :text => "SAVE")
        end
        expect(form).to have_selector("textarea#content_container_for_edit", "&lt;foo&gt;&lt;/foo&gt;")
      end
    end
    Capybara.string(response.body).find('div#modifiable_groups').tap do |div|
      expect(div).to have_selector("li.selected a.modifiable_group_link[href='#{pipelines_snippet_show_path(:group_name => 'foo')}']", 'foo')
      expect(div).to have_selector("a.modifiable_group_link[href='#{pipelines_snippet_show_path(:group_name => 'bar')}']", 'bar')
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

    render

    Capybara.string(response.body).find('div.form_submit_errors').tap do |div|
      div.find("div.errors").tap do |form|
        expect(form).to have_selector("li", :text => "error1")
        expect(form).to have_selector("li", :text => "error2")
      end
    end
  end
end
