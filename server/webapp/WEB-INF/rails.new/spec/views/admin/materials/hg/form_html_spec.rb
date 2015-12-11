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

describe "_form.html.erb" do
  include GoUtil, FormUI

  before(:each) do
    @material_config = HgMaterialConfig.new("hg://foo", "dest")
    @material_config.setName(CaseInsensitiveString.new("Hg Material Name"))
    @material_config.setAutoUpdate(true)
    @ignored_file = IgnoredFiles.new("/sugar")
    @material_config.setFilter(Filter.new([@ignored_file, IgnoredFiles.new("/jaggery")].to_java(IgnoredFiles)))

    assign(:cruise_config, @cruise_config = BasicCruiseConfig.new)
    set(@cruise_config, "md5", "abc")
  end

  it "should render all hg material attributes" do
    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/hg/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "FOO"}}

    expect(response.body).to have_selector("input[type='hidden'][name='current_tab'][value='materials']")
    expect(response.body).to have_selector(".popup_form input[type='hidden'][name='material_type'][value='#{@material_config.getType()}']")
    expect(response.body).to have_selector(".popup_form input[type='text'][name='material[#{AbstractMaterialConfig::MATERIAL_NAME}]'][value='Hg Material Name']")
    expect(response.body).to have_selector(".popup_form input[type='text'][name='material[#{HgMaterialConfig::URL}]'][value='hg://foo']")
    expect(response.body).to have_selector(".popup_form input[type='text'][name='material[#{ScmMaterialConfig::FOLDER}]'][value='dest']")
    expect(response.body).to have_selector(".popup_form input[type='checkbox'][name='material[#{ScmMaterialConfig::AUTO_UPDATE}]'][checked='checked']")
    expect(response.body).to have_selector(".popup_form textarea[name='material[#{ScmMaterialConfig::FILTER}]']", :text => "/sugar,/jaggery")
    expect(response.body).to have_selector(".form_buttons button[type='submit'] span", :text => "FOO")
  end

  it "should display check connection button" do
    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/hg/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "FOO"}}

    expect(response.body).to have_selector(".popup_form button#check_connection_hg", :text => "CHECK CONNECTION")
    expect(response.body).to have_selector(".popup_form #vcsconnection-message_hg", :text => "", visible: false)
  end

  it "should display new hg material view with errors" do
    error = config_error(HgMaterialConfig::URL, "Url is wrong")
    error.add(ScmMaterialConfig::MATERIAL_NAME, "Material Name is so wrong")
    error.add(HgMaterialConfig::AUTO_UPDATE, "AUTO_UPDATE is wrong")
    error.add(HgMaterialConfig::FOLDER, "Folder is wrong")
    error.add(com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::FOLDER, "Folder is wrong")
    set(@material_config, "errors", error)
    set(@ignored_file, "configErrors", config_error(com.thoughtworks.go.config.materials.IgnoredFiles::PATTERN, "Filter is wrong"))

    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/hg/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "foo"}}

    Capybara.string(response.body).find('.popup_form').tap do |popup_form|
      expect(popup_form).to have_selector("div.field_with_errors input[type='text'][name='material[#{AbstractMaterialConfig::MATERIAL_NAME}]'][value='Hg Material Name']")
      expect(popup_form).to have_selector("div.form_error", :text => "Material Name is so wrong")

      expect(popup_form).to have_selector("div.field_with_errors input[type='text'][name='material[#{HgMaterialConfig::URL}]'][value='hg://foo']")
      expect(popup_form).to have_selector("div.form_error", :text => "Url is wrong")

      expect(popup_form).to have_selector("div.field_with_errors input[type='text'][name='material[#{ScmMaterialConfig::FOLDER}]'][value='dest']")
      expect(popup_form).to have_selector("div.form_error", :text => "Folder is wrong")
      expect(popup_form).to have_selector("div.field_with_errors input[type='checkbox'][name='material[#{ScmMaterialConfig::AUTO_UPDATE}]'][checked='checked']")
      expect(popup_form).to have_selector("div.form_error", :text => "AUTO_UPDATE is wrong")

      #Have skipped asserting on the div fieldWithError thats rendered around the text area , since the keys mismatch (pattern vs filter). Div around the actual text area is currently
      #not affecting functionality in any way.
      expect(popup_form).to have_selector("div.form_error", :text => "Filter is wrong")
    end
  end

  it "should not generate the id for input text of url field" do
    render partial: "admin/materials/hg/form.html", locals: { scope: { material: @material_config, url: "http://google.com", method: "POST", submit_label: "foo" }}

    Capybara.string(response.body).all(".form_item .form_item_block").tap do |text_field|
      expect(text_field[0]).to_not have_selector("input[type='text'][class='form_input url'][id]")
    end
  end
end
