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

describe "admin/materials/pluggable_scm/show_existing.html.erb" do
  include GoUtil, FormUI

  PLUGIN_ID = 'my.scm.plugin'

  before :each do
    in_params(:pipeline_name => 'pipeline_name')

    assign(:cruise_config, config = BasicCruiseConfig.new)
    set(config, 'md5', 'md5-1')

    view.stub(:admin_pluggable_scm_choose_existing_path).and_return('admin_pluggable_scm_choose_existing_path')

    scm = SCMMother.create('scm-id', 'scm-name', PLUGIN_ID, '1', Configuration.new)
    scms = com.thoughtworks.go.domain.scm.SCMs.new
    scms.add(scm)
    pluggable_scm = PluggableSCMMaterialConfig.new(nil, scm, 'dest', Filter.new)
    assign(:material, @material = pluggable_scm)
    assign(:scms, @scms = scms)
  end

  it "should render the config md5, form buttons and flash message" do
    render

    expect(response.body).to have_selector('#message_pane')

    Capybara.string(response.body).find("form[action='admin_pluggable_scm_choose_existing_path'][method='post']").tap do |form|
      expect(form).to have_selector("input[id='config_md5'][type='hidden'][value='md5-1']")
      expect(form).to have_selector("button[type='submit']", :text => 'SAVE')
      expect(form).to have_selector("button", :text => 'Cancel')
    end
  end

  it "should render the config conflict message" do
    assign(:config_file_conflict, true)

    render

    expect(response.body).to have_selector('#config_save_actions')
  end

  it "should render the required message" do
    render

    expect(response.body).to have_selector('.required .asterisk')
  end

  it "should render scm selection drop-down, destination & filter" do
    render

    expect(response.body).to have_select("material[#{com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig::SCM_ID}]", :options => ['scm-name'])
    expect(response.body).to have_selector(".popup_form input[type='text'][name='material[#{PluggableSCMMaterialConfig::FOLDER}]']")
    expect(response.body).to have_selector(".popup_form textarea[name='material[#{PluggableSCMMaterialConfig::FILTER}]']", :text => '')
  end

  it "should display show existing pluggable SCM material view with errors" do
    pluggable_scm_errors = config_errors([PluggableSCMMaterialConfig::SCM_ID, 'Duplicate SCM found'], [PluggableSCMMaterialConfig::FOLDER, 'Folder is wrong'])
    set(@material, "errors", pluggable_scm_errors)
    @ignored_file = IgnoredFiles.new('/sugar')
    @material.setFilter(Filter.new([@ignored_file].to_java(IgnoredFiles)))
    set(@ignored_file, 'configErrors', config_error(com.thoughtworks.go.config.materials.IgnoredFiles::PATTERN, 'Filter is wrong'))

    render

    Capybara.string(response.body).find('.popup_form').tap do |popup_form|
      expect(popup_form).to have_selector('div.form_error', :text => 'Duplicate SCM found')
      expect(popup_form).to have_selector('div.form_error', :text => 'Folder is wrong')
      expect(popup_form).to have_selector('div.form_error', :text => 'Filter is wrong')
    end
  end

  it "should render warning when no SCMs in config" do
    assign(:scms, @scms = com.thoughtworks.go.domain.scm.SCMs.new)

    render

    expect(response.body).to have_selector('.popup_form .warnings', :text => 'No SCMs found. Please add a SCM first.')
  end

  def text_without_whitespace element
    element.native.inner_html.gsub(/^[\n ]*/, '').gsub(/[\n ]*$/, '')
  end
end
