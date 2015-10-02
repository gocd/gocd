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

describe "admin/configuration/split_pane.html.erb" do

  before :each do
    view.stub(:config_view_path).and_return("config_view_path")
    view.stub(:config_update_path).and_return('config_update_path')
  end

  it "should render heading" do
    assign(:conflicted_config, GoConfig.new({"content" => 'conflicted-content', "md5" => 'conflict-md5', "location" => "path_to_config_xml"}))
    assign(:go_config, GoConfig.new({"content" => 'current-content', "md5" => 'current-md5', "location" => "path_to_config_xml"}))

    render

    Capybara.string(response.body).find('div.heading').tap do |div|
      expect(div).to have_selector("h2", :text => "Configuration File")
      expect(div).to have_selector("span", :text => "Configuration File Path:")
      expect(div).to have_selector("span", :text => "path_to_config_xml")
    end
  end

  it "should render view" do
    assign(:conflicted_config, GoConfig.new({"content" => 'conflicted-content', "md5" => 'conflict-md5', "location" => "path_to_config_xml"}))
    assign(:go_config, GoConfig.new({"content" => 'current-content', "md5" => 'current-md5', "location" => "path_to_config_xml"}))
    date = java.util.Date.new(1366866649)
    difference = "#{time_ago_in_words(date.to_string)} #{l.string('AGO')}"
    cruise_config_revision = double("cruise config revision")
    cruise_config_revision.should_receive(:getTime).and_return(date)
    cruise_config_revision.should_receive(:getUsername).and_return("Ali")
    assign(:go_config_revision, cruise_config_revision)

    render

    Capybara.string(response.body).find('div#admin-holder-for-admin-config-source-xml').tap do |div|
      div.find("div.conflicted_content").tap do |conflicted_content|
        expect(div).to have_selector("h3", :text => "Your Changes")
        expect(div).to have_selector("pre.wrap_pre", :text => "conflicted-content")

        expect(div).not_to have_selector("input.hidden[name='configMd5'][value='conflict-md5']")
        expect(div).not_to have_selector("input.hidden[name='config_md5'][value='conflict-md5']")
      end
      div.find("div.current_content").tap do |current_content|
        current_content.find("form#config_editor_form[method='post'][action='config_update_path']").tap do |conflicted_content|
          expect(current_content).to have_selector("input[name='_method'][value='put']")
          current_content.find("div.form_heading").tap do |form_heading|
            expect(form_heading).to have_selector("div.config_change_timestamp", :text => "Last modified: #{difference} by Ali")
            expect(form_heading).to have_selector("div.config_change_timestamp[title='Last modified: #{difference} by Ali']")
            form_heading.find("div.buttons-group").tap do |buttons_group|
              expect(buttons_group).to have_selector("input#save_config[class='link_as_button primary'][type='submit'][value='SAVE'][disabled='disabled']")
              expect(buttons_group).to have_selector("a#cancel_edit[class='link_as_button'][href='config_view_path']", :text => "Cancel")
            end
          end
          expect(current_content).to have_selector("input[type='hidden'][name='go_config[md5]'][value='current-md5']")
          expect(current_content).to have_selector("textarea#content[name='go_config[content]']", :text => "current-content")
        end
      end
    end
  end

  it "should not bomb when no time stamp for current revision exists" do
    assign(:conflicted_config, GoConfig.new({"content" => 'conflicted-content', "md5" => 'conflict-md5', "location" => "path_to_config_xml"}))
    assign(:go_config, GoConfig.new({"content" => 'current-content', "md5" => 'current-md5', "location" => "path_to_config_xml"}))
    assign(:go_config_revision, nil)

    render

    expect(response.body).to have_selector("div.config_change_timestamp", :text => "Last modified: Unknown by Unknown")
  end

  it "should show global errors in case of config save failure" do
    assign(:conflicted_config, GoConfig.new({"content" => 'conflicted-content', "md5" => 'conflict-md5', "location" => "path_to_config_xml"}))
    assign(:go_config, GoConfig.new({"content" => 'current-content', "md5" => 'current-md5', "location" => "path_to_config_xml"}))
    assign(:errors, ['some error that has happened', 'more lines'])

    render

    Capybara.string(response.body).find('div.form_submit_errors').tap do |div|
      div.find("div.errors").tap do |errors|
        expect(errors).to have_selector("h3", :text => "The following error(s) need to be resolved in order to perform this action:")
        errors.find("ul").tap do |ul|
          expect(ul).to have_selector("li.error", :text => "some error that has happened")
          expect(ul).to have_selector("li.error", :text => "more lines")
        end
      end
    end
  end

end
