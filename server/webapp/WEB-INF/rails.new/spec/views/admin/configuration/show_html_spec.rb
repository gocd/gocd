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

describe "admin/configuration/show.html.erb" do

  before :each do
    view.stub(:config_edit_path).and_return('config_edit_path')
  end

  it "should render heading" do
    assign(:go_config, GoConfig.new({"content" => 'current-content', "md5" => 'current-md5', "location" => "path_to_config_xml"}))

    render

    Capybara.string(response.body).find('div.heading').tap do |div|
      expect(div).to have_selector("h2", :text => "Configuration File")
      expect(div).to have_selector("span", :text => "Configuration File Path:")
      expect(div).to have_selector("span", :text => "path_to_config_xml")
    end
  end

  it "should render view" do
    assign(:go_config, GoConfig.new({"content" => 'config-content', "md5" => 'md5', "location" => "path_to_config_xml"}))
    date = java.util.Date.new(1366866649)
    difference = "#{time_ago_in_words(date.to_string)} #{l.string('AGO')}"
    cruise_config_revision = double("cruise config revision")
    cruise_config_revision.should_receive(:getTime).and_return(date)
    cruise_config_revision.should_receive(:getUsername).and_return("Ali")
    assign(:go_config_revision, cruise_config_revision)

    render

    Capybara.string(response.body).find('div.sub_tab_container_content').tap do |div|
      div.find("div#tab-content-of-source-xml").tap do |tab|
        tab.find(".admin_holder").tap do |admin|
          admin.find("div.form_heading").tap do |form_heading|
            expect(form_heading).to have_selector("div.config_change_timestamp", :text => "Last modified: #{difference} by Ali")
            expect(form_heading).to have_selector("div.config_change_timestamp[title='Last modified: #{difference} by Ali']")
            admin.find("div.buttons-group").tap do |buttons_group|
              expect(buttons_group).to have_selector("a#edit_config[class='link_as_button primary'][href='config_edit_path']", :text => "Edit")
            end
          end
          admin.find("div#content_area").tap do |content_area|
            expect(content_area).to have_selector("pre#content_container", :text => "config-content")
          end
        end
      end
    end
  end
end
