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

describe "admin/materials/pluggable_scm/new.html.erb" do
  include GoUtil, FormUI

  PLUGIN_ID = "my.scm.plugin"
  PLUGIN_TEMPLATE = "<input ng-model=\"KEY1\" type=\"text\"><input ng-model=\"key2\" type=\"text\">"

  before :each do
    assign(:cruise_config, config = CruiseConfig.new)
    set(config, "md5", "md5-1")

    view.stub(:admin_pluggable_scm_create_path).and_return("admin_pluggable_scm_create_path")
    pluggable_scm = PluggableSCMMaterialConfig.new(nil, SCMMother.create(nil, nil, PLUGIN_ID, '1', Configuration.new), 'dest', Filter.new)
    assign(:material, @material = pluggable_scm)
    assign(:meta_data_store, @meta_data_store = SCMMetadataStore.getInstance())

    setup_meta_data
  end

  it "should render the config md5, form buttons and flash message" do
    render

    expect(response.body).to have_selector("#message_pane")

    Capybara.string(response.body).find("form[action='admin_pluggable_scm_create_path'][method='post']").tap do |form|
      expect(form).to have_selector("input[id='config_md5'][type='hidden'][value='md5-1']")
      expect(form).to have_selector("button[type='submit']", :text => "SAVE")
      expect(form).to have_selector("button", :text => "Cancel")
    end
  end

  it "should render the config conflict message" do
    assign(:config_file_conflict, true)

    render

    expect(response.body).to have_selector("#config_save_actions")
  end

  it "should render the required message" do
    render

    expect(response.body).to have_selector(".required .asterisk")
  end

  it "should render name, destination & filter" do
    render

    expect(response.body).to have_selector(".popup_form input[type='text'][name='material[#{com.thoughtworks.go.domain.scm.SCM::NAME}]']")
    expect(response.body).to have_selector(".popup_form input[type='text'][name='material[#{PluggableSCMMaterialConfig::FOLDER}]']")
    expect(response.body).to have_selector(".popup_form textarea[name='material[#{PluggableSCMMaterialConfig::FILTER}]']", :text => "")
  end

  it "should render plugin template and data for a new pluggable SCM" do
    render

    Capybara.string(response.body).find('div.plugged_material#material_angular_pluggable_material_my_scm_plugin').tap do |div|
      template_text = text_without_whitespace(div.find("div.plugged_material_template"))
      expect(template_text).to eq(PLUGIN_TEMPLATE)

      div.find("span.plugged_material_data", :visible => false).text.strip!.should == '{}'
    end
  end

  def setup_meta_data
    @meta_data_store.clear()

    scm_view = double('SCMView')
    scm_view.stub(:displayValue).and_return('Display Name')
    scm_view.stub(:template).and_return(PLUGIN_TEMPLATE)
    @meta_data_store.addMetadataFor(PLUGIN_ID, SCMConfigurations.new, scm_view)
  end

  def text_without_whitespace element
    element.native.inner_html.gsub(/^[\n ]*/, '').gsub(/[\n ]*$/, '')
  end
end