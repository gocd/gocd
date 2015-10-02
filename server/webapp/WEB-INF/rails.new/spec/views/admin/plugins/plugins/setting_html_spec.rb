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

describe "admin/plugins/plugins/settings.html.erb" do
  include GoUtil, FormUI

  SETTINGS_PLUGIN_ID = 'my.scm.plugin'
  SETTINGS_PLUGIN_TEMPLATE = "<input ng-model=\"KEY1\" type=\"text\"><input ng-model=\"key2\" type=\"text\">"

  before :each do
    view.stub(:update_settings_path).and_return('update_settings_path')

    assign(:plugin_settings, @plugin_settings = PluginSettings.new(SETTINGS_PLUGIN_ID))
    assign(:meta_data_store, @meta_data_store = double('metadata store'))

    @plugin_settings.populateSettingsMap({'KEY1' => 'value1', 'key2' => 'value2'})
    expect(@meta_data_store).to receive(:template).with(SETTINGS_PLUGIN_ID).and_return(SETTINGS_PLUGIN_TEMPLATE)
  end

  it "should render the form buttons and flash message" do
    render

    expect(response.body).to have_selector('#message_pane')

    Capybara.string(response.body).find("form[action='update_settings_path'][method='post']").tap do |form|
      expect(form).to have_selector("button[type='submit']", :text => 'SAVE')
      expect(form).to have_selector("button", :text => 'Cancel')
    end
  end

  it "should render the required message" do
    render

    expect(response.body).to have_selector('.required .asterisk')
  end

  it "should render plugin template and data for a plugin settings" do
    render

    Capybara.string(response.body).find('div.plugin_settings#plugin_settings_angular_plugin_settings').tap do |div|
      template_text = text_without_whitespace(div.find('div.plugin_settings_template'))
      expect(template_text).to eq(SETTINGS_PLUGIN_TEMPLATE)

      data_for_template = JSON.parse(div.find('span.plugin_settings_data', :visible => false).text)
      expect(data_for_template.keys.sort).to eq(['KEY1', 'key2'])
      expect(data_for_template['KEY1']).to eq({'value' => 'value1'})
      expect(data_for_template['key2']).to eq({'value' => 'value2'})
    end
  end

  def text_without_whitespace element
    element.native.inner_html.gsub(/^[\n ]*/, '').gsub(/[\n ]*$/, '')
  end
end
