#
# Copyright 2019 ThoughtWorks, Inc.
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
#

require 'rails_helper'

describe "admin/materials/pluggable_scm/new.html.erb" do
  include GoUtil
  include FormUI

  scm_plugin_id = 'my.scm.plugin'
  scm_plugin_template = "<input ng-model=\"KEY1\" type=\"text\"><input ng-model=\"key2\" type=\"text\">"

  before :each do
    in_params(:pipeline_name => 'pipeline_name')

    assign(:cruise_config, config = BasicCruiseConfig.new)
    set(config, 'md5', 'md5-1')

    allow(view).to receive(:admin_pluggable_scm_create_path).and_return('admin_pluggable_scm_create_path')
    pluggable_scm = PluggableSCMMaterialConfig.new(nil, SCMMother.create(nil, nil, scm_plugin_id, '1', Configuration.new), 'dest', Filter.new)
    assign(:material, @material = pluggable_scm)
    assign(:meta_data_store, @meta_data_store = SCMMetadataStore.getInstance())

    setup_meta_data
  end

  after :each do
    @meta_data_store.clear()
  end

  it "should render the config md5, form buttons and flash message" do
    render

    expect(response.body).to have_selector('#message_pane')

    Capybara.string(response.body).find("form[action='admin_pluggable_scm_create_path'][method='post']").tap do |form|
      expect(form).to have_selector("input[id='config_md5'][type='hidden'][value='md5-1']", visible: :hidden)
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

  it "should render name, check-connection, auto-update, destination & filter" do
    render

    expect(response.body).to have_selector(".popup_form input[type='text'][name='material[#{com.thoughtworks.go.domain.scm.SCM::NAME}]']")
    expect(response.body).to have_selector(".popup_form button#check_connection_pluggable_scm", :text => 'CHECK CONNECTION')
    expect(response.body).to have_selector(".popup_form #pluggable_scm_check_connection_message", :text => '')
    expect(response.body).to have_selector(".popup_form input[type='checkbox'][name='material[#{com.thoughtworks.go.domain.scm.SCM::AUTO_UPDATE}]'][checked='checked']")
    expect(response.body).to have_selector(".popup_form input[type='text'][name='material[#{PluggableSCMMaterialConfig::FOLDER}]']")
    expect(response.body).to have_selector(".popup_form textarea[name='material[#{PluggableSCMMaterialConfig::FILTER}]']", :text => '')
  end

  it "should render plugin template and data for a new pluggable SCM" do
    render

    Capybara.string(response.body).find('div.plugged_material#material_angular_pluggable_material_my_scm_plugin').tap do |div|
      template_text = text_without_whitespace(div.find('div.plugged_material_template'))
      expect(template_text).to eq(scm_plugin_template)

      expect(div.find('span.plugged_material_data', :visible => false).text.strip!).to eq('{}')
    end
  end

  it "should display new pluggable SCM material view with errors" do
    scm_errors = config_error(com.thoughtworks.go.domain.scm.SCM::NAME, 'Material Name is so wrong')
    scm_errors.add(com.thoughtworks.go.domain.scm.SCM::AUTO_UPDATE, 'AUTO_UPDATE is wrong')
    set(@material.getSCMConfig(), 'errors', scm_errors)

    pluggable_scm_errors = config_error(PluggableSCMMaterialConfig::FOLDER, 'Folder is wrong')
    set(@material, 'errors', pluggable_scm_errors)
    @ignored_file = IgnoredFiles.new('/sugar')
    @material.setFilter(Filter.new([@ignored_file].to_java(IgnoredFiles)))
    set(@ignored_file, 'configErrors', config_error(com.thoughtworks.go.config.materials.IgnoredFiles::PATTERN, 'Filter is wrong'))

    render

    Capybara.string(response.body).find('.popup_form').tap do |popup_form|
      expect(popup_form).to have_selector('div.form_error', :text => 'Material Name is so wrong')
      expect(popup_form).to have_selector('div.form_error', :text => 'AUTO_UPDATE is wrong')
      expect(popup_form).to have_selector('div.form_error', :text => 'Folder is wrong')
      expect(popup_form).to have_selector('div.form_error', :text => 'Filter is wrong')
    end
  end

  def setup_meta_data
    @meta_data_store.clear()
    scm_plugin_id = 'my.scm.plugin'
    scm_plugin_template = "<input ng-model=\"KEY1\" type=\"text\"><input ng-model=\"key2\" type=\"text\">"
    scm_view = double('SCMView')
    allow(scm_view).to receive(:displayValue).and_return('Display Name')
    allow(scm_view).to receive(:template).and_return(scm_plugin_template)
    @meta_data_store.addMetadataFor(scm_plugin_id, SCMConfigurations.new, scm_view)
  end

  def text_without_whitespace element
    element.native.inner_html.gsub(/^[\n ]*/, '').gsub(/[\n ]*$/, '')
  end
end
