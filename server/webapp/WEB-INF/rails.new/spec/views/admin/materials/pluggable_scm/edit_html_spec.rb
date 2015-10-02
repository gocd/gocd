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

describe "/admin/materials/pluggable_scm/edit.html.erb" do
  include GoUtil, FormUI

  SCM_PLUGIN_ID = 'my.scm.plugin'
  SCM_PLUGIN_TEMPLATE = "<input ng-model=\"KEY1\" type=\"text\"><input ng-model=\"key2\" type=\"text\">"

  before :each do
    in_params(:pipeline_name => 'pipeline_name')

    assign(:cruise_config, config = BasicCruiseConfig.new)
    set(config, 'md5', 'md5-1')

    view.stub(:admin_pluggable_scm_update_path).and_return('admin_pluggable_scm_update_path')
    configuration = Configuration.new([ConfigurationPropertyMother.create('KEY1', false, 'value1'), ConfigurationPropertyMother.create('key2', false, 'value2')].to_java(ConfigurationProperty))
    scm = SCMMother.create('scm-id', 'scm-name', SCM_PLUGIN_ID, '1', configuration)
    scm.setAutoUpdate(false)
    filters = Filter.new([IgnoredFiles.new('/sugar'), IgnoredFiles.new('/jaggery')].to_java(IgnoredFiles))
    pluggable_scm = PluggableSCMMaterialConfig.new(nil, scm, 'dest', filters)
    assign(:material, @material = pluggable_scm)
    assign(:meta_data_store, @meta_data_store = SCMMetadataStore.getInstance())

    setup_meta_data
  end

  after :each do
    @meta_data_store.clear()
  end

  it "should render the warning, pipelines used in link" do
    render

    expect(response.body).to have_selector('.warnings', text: 'This is a global copy. All pipelines using this SCM will be affected.')
    expect(response.body).to have_selector("a#show_pipelines_used_in", :text => 'Show pipelines using this SCM')
    expect(response.body).to have_selector("#pipelines_used_in")
  end

  it "should render the config md5, form buttons and flash message" do
    render

    expect(response.body).to have_selector('#message_pane')

    Capybara.string(response.body).find("form[action='admin_pluggable_scm_update_path'][method='post']").tap do |form|
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

  it "should render name, check-connection, auto-update, destination & filter" do
    render

    expect(response.body).to have_selector(".popup_form input[type='text'][name='material[#{com.thoughtworks.go.domain.scm.SCM::NAME}]'][value='scm-name']")
    expect(response.body).to have_selector(".popup_form button#check_connection_pluggable_scm", :text => 'CHECK CONNECTION')
    expect(response.body).to have_selector(".popup_form #pluggable_scm_check_connection_message", :text => '')
    expect(response.body).to have_selector(".popup_form input[type='checkbox'][name='material[#{com.thoughtworks.go.domain.scm.SCM::AUTO_UPDATE}]']")
    expect(response.body).to have_selector(".popup_form input[type='text'][name='material[#{PluggableSCMMaterialConfig::FOLDER}]'][value='dest']")
    expect(response.body).to have_selector(".popup_form textarea[name='material[#{PluggableSCMMaterialConfig::FILTER}]']", :text => '/sugar,/jaggery')
  end

  it "should render plugin template and data for a new pluggable SCM" do
    render

    Capybara.string(response.body).find('div.plugged_material#material_angular_pluggable_material_my_scm_plugin').tap do |div|
      template_text = text_without_whitespace(div.find('div.plugged_material_template'))
      expect(template_text).to eq(SCM_PLUGIN_TEMPLATE)

      data_for_template = JSON.parse(div.find('span.plugged_material_data', :visible => false).text)
      expect(data_for_template.keys.sort).to eq(['KEY1', 'key2'])
      expect(data_for_template['KEY1']).to eq({'value' => 'value1'})
      expect(data_for_template['key2']).to eq({'value' => 'value2'})
    end
  end

  it "should display edit pluggable SCM material view with errors" do
    scm_errors = config_error(com.thoughtworks.go.domain.scm.SCM::NAME, 'Material Name is so wrong')
    scm_errors.add(com.thoughtworks.go.domain.scm.SCM::AUTO_UPDATE, 'AUTO_UPDATE is wrong')
    set(@material.getSCMConfig(), 'errors', scm_errors)

    pluggable_scm_errors = config_error(PluggableSCMMaterialConfig::FOLDER, 'Folder is wrong')
    set(@material, "errors", pluggable_scm_errors)
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

    scm_view = double('SCMView')
    scm_view.stub(:displayValue).and_return('Display Name')
    scm_view.stub(:template).and_return(SCM_PLUGIN_TEMPLATE)
    @meta_data_store.addMetadataFor(SCM_PLUGIN_ID, SCMConfigurations.new, scm_view)
  end

  def text_without_whitespace element
    element.native.inner_html.gsub(/^[\n ]*/, '').gsub(/[\n ]*$/, '')
  end
end
