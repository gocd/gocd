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
  include GoUtil, FormUI, ReflectiveUtil

  before(:each) do
    @material_config = TfsMaterialConfig.new(GoCipher.new, UrlArgument.new("http://10.4.4.101:8080/tfs/Sample"), "loser", "domain" ,"passwd", "walk_this_path")
    @material_config.setAutoUpdate(true)
    @material_config.setName(CaseInsensitiveString.new("Tfs Material Name"))
    @material_config.setFolder("boulder")
    @ignored_file = IgnoredFiles.new("/sugar")
    @material_config.setFilter(Filter.new([@ignored_file, IgnoredFiles.new("/jaggery")].to_java(IgnoredFiles)))

    assign(:cruise_config, @cruise_config = BasicCruiseConfig.new)
    set(@cruise_config, "md5", "abc")
  end

  it "should render all tfs material attributes" do
    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/tfs/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "FOO"}}

    expect(response.body).to have_selector("input[type='hidden'][name='current_tab'][value='materials']")
    expect(response.body).to have_selector(".popup_form input[type='text'][name='material[#{com.thoughtworks.go.config.materials.AbstractMaterialConfig::MATERIAL_NAME}]'][value='Tfs Material Name']")
    expect(response.body).to have_selector(".popup_form input[type='text'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::URL}]'][value='http://10.4.4.101:8080/tfs/Sample']")
    expect(response.body).to have_selector(".popup_form input[type='text'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::USERNAME}]'][value='loser']")
    expect(response.body).to have_selector(".popup_form input[type='text'][name='material[#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::DOMAIN}]'][value='domain']")
    expect(response.body).to have_selector(".popup_form input[type='password'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::PASSWORD}]'][value='passwd']")
    expect(response.body).to have_selector(".popup_form input[type='hidden'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::ENCRYPTED_PASSWORD}]'][value='#{@material_config.getEncryptedPassword()}']")
    expect(response.body).to have_selector(".popup_form input[type='text'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::FOLDER}]'][value='boulder']")
    expect(response.body).to have_selector(".popup_form input[type='checkbox'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::AUTO_UPDATE}]'][checked='checked']")
    expect(response.body).to have_selector(".popup_form input[type='text'][name='material[#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::PROJECT_PATH}]'][value='walk_this_path']")
    expect(response.body).to have_selector(".popup_form textarea[name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::FILTER}]']", :text => "/sugar,/jaggery")
    expect(response.body).to have_selector(".form_buttons button[type='submit'] span", :text => "FOO")

    expect(response.body).not_to have_selector(".popup_form input[type='text'][name='material[workspaceOwner]']")
    expect(response.body).not_to have_selector(".popup_form input[type='text'][name='material[workspace]']")
  end

  it "should display the password field disabled in edit mode" do
    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/tfs/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "FOO", :edit_mode => true}}

    expect(response.body).to have_selector(".popup_form input[disabled='disabled'][type='password'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::PASSWORD}]'][value='passwd']")
    expect(response.body).to have_selector(".popup_form input[type='checkbox'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::PASSWORD_CHANGED}]']")
  end

  it "should display the password field as textbox in new mode" do
    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/tfs/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "FOO", :edit_mode => false}}

    expect(response.body).to have_selector(".popup_form input[type='password'][name='material[#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::PASSWORD}]']")
    Capybara.string(response.body).find(".popup_form  div[class='hidden']").tap do |popup_form|
      expect(popup_form).to have_selector("input[type='checkbox'][name='material[#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::PASSWORD_CHANGED}]'][value='1'][checked='checked']")
    end
  end

  it "should display check connection button" do
    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/tfs/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "FOO"}}

    expect(response.body).to have_selector(".popup_form button#check_connection_tfs", :text => "CHECK CONNECTION")
    expect(response.body).to have_selector(".popup_form #vcsconnection-message_tfs", :text => "")
    expect(response.body).to have_selector(".url")
    expect(response.body).to have_selector(".username")
    expect(response.body).to have_selector(".password")
  end

  it "should display new tfs material view with errors" do
    error = config_error(com.thoughtworks.go.config.materials.ScmMaterialConfig::URL, "Url is wrong")
    error.add(com.thoughtworks.go.config.materials.ScmMaterialConfig::USERNAME, "Username is wrong")
    error.add(com.thoughtworks.go.config.materials.ScmMaterialConfig::MATERIAL_NAME, "Material Name is so wrong")
    error.add(com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::PROJECT_PATH, "path you talk and path you walk are different.")
    error.add(com.thoughtworks.go.config.materials.ScmMaterialConfig::PASSWORD, "Password is wrong")
    error.add(com.thoughtworks.go.config.materials.ScmMaterialConfig::AUTO_UPDATE, "AUTO_UPDATE is wrong")
    error.add(com.thoughtworks.go.config.materials.ScmMaterialConfig::FOLDER, "Folder is wrong")
    set(@material_config, "errors", error)
    set(@ignored_file, "configErrors", config_error(com.thoughtworks.go.config.materials.IgnoredFiles::PATTERN, "Filter is wrong"))

    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/tfs/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "foo"}}

    Capybara.string(response.body).find('.popup_form').tap do |popup_form|
      expect(popup_form).to have_selector("div.field_with_errors input[type='text'][name='material[#{com.thoughtworks.go.config.materials.AbstractMaterialConfig::MATERIAL_NAME}]'][value='Tfs Material Name']")
      expect(popup_form).to have_selector("div.form_error", :text => "Material Name is so wrong")

      expect(popup_form).to have_selector("div.field_with_errors input[type='text'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::URL}]'][value='http://10.4.4.101:8080/tfs/Sample']")
      expect(popup_form).to have_selector("div.form_error", :text => "Url is wrong")

      expect(popup_form).to have_selector("div.field_with_errors input[type='text'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::USERNAME}]'][value='loser']")
      expect(popup_form).to have_selector("div.form_error", :text => "Username is wrong")

      expect(popup_form).to have_selector("div.field_with_errors input[type='password'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::PASSWORD}]'][value='passwd']")
      expect(popup_form).to have_selector("div.form_error", :text => "Password is wrong")

      expect(popup_form).to have_selector("div.field_with_errors input[type='text'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::FOLDER}]'][value='boulder']")
      expect(popup_form).to have_selector("div.form_error", :text => "Folder is wrong")

      expect(popup_form).to have_selector("div.field_with_errors input[type='checkbox'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::AUTO_UPDATE}]'][checked='checked']")
      expect(popup_form).to have_selector("div.form_error", :text => "AUTO_UPDATE is wrong")

      expect(popup_form).to have_selector("div.field_with_errors input[type='text'][name='material[#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::PROJECT_PATH}]'][value='walk_this_path']")
      expect(popup_form).to have_selector("div.form_error", :text => "path you talk and path you walk are different.")

      #Have skipped asserting on the div fieldWithError thats rendered around the text area , since the keys mismatch (pattern vs filter). Div around the actual text area is currently
      #not affecting functionality in any way.
      expect(popup_form).to have_selector("div.form_error", :text => "Filter is wrong")

      expect(popup_form).not_to have_selector("div.field_with_errors input[type='text'][name='material[workspaceOwner]'][value='m1cr050f7']")
      expect(popup_form).not_to have_selector("div.field_with_errors input[type='text'][name='material[workspace]'][value='work_in_space']")
    end
  end

  it "should not generate the id for url, domain, username and project_path fields" do
    render :partial => "admin/materials/tfs/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "FOO"}}

    Capybara.string(response.body).all(".form_item .form_item_block").tap do |text_field|
      expect(text_field[1]).to_not have_selector("input[type='text'][class='form_input url'][id]")
      expect(text_field[3]).to_not have_selector("input[type='text'][class='form_input domain'][id]")
      expect(text_field[4]).to_not have_selector("input[type='text'][class='form_input username'][id]")
      expect(text_field[6]).to_not have_selector("input[type='text'][class='form_input project_path'][id]")
    end
  end
end
