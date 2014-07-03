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

describe "admin/configuration/split_pane.html" do

  before :each do
    template.stub(:config_view_path).and_return("config_view_path")
    template.stub(:config_update_path).and_return('config_update_path')
  end

  it "should render heading" do
    assign(:conflicted_config, GoConfig.new({"content" => 'conflicted-content', "md5" => 'conflict-md5', "location" => "path_to_config_xml"}))
    assign(:go_config, GoConfig.new({"content" => 'current-content', "md5" => 'current-md5', "location" => "path_to_config_xml"}))

    render 'admin/configuration/split_pane.html'

    response.body.should have_tag("div.heading") do
      with_tag("h2", "Configuration File")
      with_tag("span", "Configuration File Path:")
      with_tag("span", "path_to_config_xml")
    end
  end

  it "should render view" do
    assign(:conflicted_config, GoConfig.new({"content" => 'conflicted-content', "md5" => 'conflict-md5', "location" => "path_to_config_xml"}))
    assign(:go_config, GoConfig.new({"content" => 'current-content', "md5" => 'current-md5', "location" => "path_to_config_xml"}))
    date = java.util.Date.new(1366866649)
    current_date = java.util.Date.new()
    difference = current_date.getYear() - date.getYear()
    cruise_config_revision = mock("cruise config revision")
    cruise_config_revision.should_receive(:getTime).and_return(date)
    cruise_config_revision.should_receive(:getUsername).and_return("Ali")
    assign(:go_config_revision, cruise_config_revision)

    render 'admin/configuration/split_pane.html'

    response.body.should have_tag("div#admin-holder-for-admin-config-source-xml") do
      with_tag("div.conflicted_content") do
        with_tag("h3", "Your Changes")
        with_tag("pre.wrap_pre", "conflicted-content")
        without_tag("input.hidden[name='configMd5'][value=?]", "conflict-md5")
        without_tag("input.hidden[name='config_md5'][value=?]", "conflict-md5")
      end
      with_tag("div.current_content") do
        with_tag("form#config_editor_form[method='post'][action=?]", "config_update_path") do
          with_tag("input[name='_method'][value=?]", 'put')
          with_tag("div.form_heading") do
            with_tag("div.config_change_timestamp", "Last modified: over #{difference} years ago by Ali")
            with_tag("div.config_change_timestamp[title=?]", "Last modified: over #{difference} years ago by Ali")
            with_tag("div.buttons-group") do
              with_tag("input#save_config[class='link_as_button primary'][type='submit'][value='SAVE'][disabled='disabled']")
              with_tag("a#cancel_edit[class='link_as_button'][href=?]", "config_view_path", :text => "Cancel")
            end
          end
          with_tag("input[type='hidden'][name='go_config[md5]'][value=?]", 'current-md5')
          with_tag("textarea#content[name='go_config[content]']", "current-content")
        end
      end
    end
  end

  it "should not bomb when no time stamp for current revision exists" do
    assign(:conflicted_config, GoConfig.new({"content" => 'conflicted-content', "md5" => 'conflict-md5', "location" => "path_to_config_xml"}))
    assign(:go_config, GoConfig.new({"content" => 'current-content', "md5" => 'current-md5', "location" => "path_to_config_xml"}))
    assign(:go_config_revision, nil)

    render 'admin/configuration/split_pane.html'

    response.body.should have_tag("div.config_change_timestamp", "Last modified: Unknown by Unknown")
  end

  it "should show global errors in case of config save failure" do
    assign(:conflicted_config, GoConfig.new({"content" => 'conflicted-content', "md5" => 'conflict-md5', "location" => "path_to_config_xml"}))
    assign(:go_config, GoConfig.new({"content" => 'current-content', "md5" => 'current-md5', "location" => "path_to_config_xml"}))
    assign(:errors, ['some error that has happened', 'more lines'])

    render 'admin/configuration/split_pane.html'

    response.body.should have_tag("div.form_submit_errors") do
      with_tag("div.errors") do
        with_tag("h3", "The following error(s) need to be resolved in order to perform this action:")
        with_tag("ul") do
          with_tag("li.error", "some error that has happened")
          with_tag("li.error", "more lines")
        end
      end
    end
  end

end
