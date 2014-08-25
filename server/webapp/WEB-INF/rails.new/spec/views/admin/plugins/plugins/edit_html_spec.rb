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

require File.join(File.dirname(__FILE__), "/../../../../spec_helper")

describe 'admin/plugins/plugins/edit.html.erb' do

  include GoUtil

  it 'should render plugin settings template' do
    assign(:settings_template, "<div class='plugin_settings_view'>plugin</div>")
    in_params(:plugin_id => "plugin-one")

    render

    Capybara.string(response.body).find('form#plugin_settings_form').tap do |form|
      expect(form).to have_selector('h2', :text => 'Settings for plugin: plugin-one')
      expect(form).to have_selector("input[type='hidden'][name='plugin_id'][value='plugin-one']")
      form.find('div#plugin_settings').tap do |div|
        expect(div).to have_selector('div.plugin_settings_view', :text => 'plugin')
      end
    end
  end

  it 'should render error' do
    assign(:settings_template, "<div class='plugin_settings_view'>plugin</div>")
    assign(:error, 'error')
    in_params(:plugin_id => 'plugin-one')

    render

    expect(response.body).to have_selector('div', :text => 'error')
    expect(response.body).not_to have_selector('form#plugin_settings_form')
  end
end