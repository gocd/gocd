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

require File.join(File.dirname(__FILE__), "/../../../spec_helper")

describe "admin/configuration/view.html" do

  before :each do
    template.stub(:config_edit_path).and_return('config_edit_path')
  end

  it "should render heading" do
    assign(:go_config, GoConfig.new({"content" => 'current-content', "md5" => 'current-md5', "location" => "path_to_config_xml"}))

    render 'admin/configuration/show.html'

    response.body.should have_tag("div.heading") do
      with_tag("h2", "Configuration File")
      with_tag("span", "Configuration File Path:")
      with_tag("span", "path_to_config_xml")
    end
  end

  it "should render view" do
    assign(:go_config, GoConfig.new({"content" => 'config-content', "md5" => 'md5', "location" => "path_to_config_xml"}))
    date = java.util.Date.new(1366866649)
    current_date = java.util.Date.new()
    difference = current_date.getYear() - date.getYear()
    cruise_config_revision = mock("cruise config revision")
    cruise_config_revision.should_receive(:getTime).and_return(date)
    cruise_config_revision.should_receive(:getUsername).and_return("Ali")
    assign(:go_config_revision, cruise_config_revision)

    render 'admin/configuration/show.html'

    response.body.should have_tag("div.sub_tab_container_content") do
      with_tag("div#tab-content-of-source-xml") do
        with_tag(".admin_holder") do
          with_tag("div.form_heading") do
            with_tag("div.config_change_timestamp", "Last modified: over #{difference} years ago by Ali")
            with_tag("div.config_change_timestamp[title=?]", "Last modified: over #{difference} years ago by Ali")
            with_tag("div.buttons-group") do
              with_tag("a#edit_config[class='link_as_button primary'][href=?]", 'config_edit_path', :text => "Edit")
            end
          end
          with_tag("div#content_area") do
            with_tag("pre#content_container", "config-content")
          end
        end
      end
    end
  end
end
