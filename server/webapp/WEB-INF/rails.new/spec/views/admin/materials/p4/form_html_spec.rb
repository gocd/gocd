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
    @material_config = P4MaterialConfig.new("p4:5000", "through_window", "loser")
    @material_config.setFolder("dest")
    @material_config.setPassword("secret")
    @material_config.setUseTickets(true)
    @material_config.setName(CaseInsensitiveString.new("P4 Material Name"))
    @material_config.setAutoUpdate(true)
    @ignored_file = IgnoredFiles.new("/sugar")
    @material_config.setFilter(Filter.new([@ignored_file, IgnoredFiles.new("/jaggery")].to_java(IgnoredFiles)))

    assign(:cruise_config, @cruise_config = BasicCruiseConfig.new)
    set(@cruise_config, "md5", "abc")
  end

  it "should render all p4 material attributes" do
    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/p4/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "FOO"}}

    expect(response.body).to have_selector("input[type='hidden'][name='current_tab'][value='materials']")
    expect(response.body).to have_selector(".popup_form input[type='hidden'][name='material_type'][value='#{@material_config.getType()}']")
    expect(response.body).to have_selector(".popup_form input[type='text'][name='material[#{AbstractMaterialConfig::MATERIAL_NAME}]'][value='P4 Material Name']")
    expect(response.body).to have_selector(".popup_form input[type='text'][name='material[#{P4MaterialConfig::SERVER_AND_PORT}]'][value='p4:5000']")
    expect(response.body).to have_selector(".popup_form input[type='text'][name='material[#{P4MaterialConfig::USERNAME}]'][value='loser']")
    expect(response.body).to have_selector(".popup_form input[type='password'][name='material[#{P4MaterialConfig::PASSWORD}]'][value='secret']")
    expect(response.body).to have_selector(".popup_form textarea[name='material[#{P4MaterialConfig::VIEW}]']", "through_window")
    expect(response.body).to have_selector(".popup_form input[type='checkbox'][name='material[#{P4MaterialConfig::USE_TICKETS}]'][value='true'][checked='checked']")
    expect(response.body).to have_selector(".popup_form input[type='checkbox'][name='material[#{ScmMaterialConfig::AUTO_UPDATE}]'][checked='checked']")
    expect(response.body).to have_selector(".popup_form textarea[name='material[#{ScmMaterialConfig::FILTER}]']", :text => "/sugar,/jaggery")
    expect(response.body).to have_selector(".form_buttons button[type='submit'] span", :text => "FOO")
  end

  it "should display the password field disabled in edit mode" do
    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/p4/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "FOO", :edit_mode => true}}

    expect(response.body).to have_selector(".popup_form input[disabled='disabled'][type='password'][name='material[#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::PASSWORD}]'][value='']")
    expect(response.body).to have_selector(".popup_form input[type='checkbox'][name='material[#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::PASSWORD_CHANGED}]']")
  end

  it "should display the password field as textbox in new mode" do
    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/p4/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "FOO", :edit_mode => false}}

    expect(response.body).to have_selector(".popup_form input[type='password'][name='material[#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::PASSWORD}]']")
    Capybara.string(response.body).find(".popup_form  div[class='hidden']").tap do |popup_form|
      expect(popup_form).to have_selector("input[type='checkbox'][name='material[#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::PASSWORD_CHANGED}]'][value='1'][checked='checked']")
    end
  end

  it "should display check connection button" do
    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/p4/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "FOO"}}

    expect(response.body).to have_selector(".username")
    expect(response.body).to have_selector(".password")
    expect(response.body).to have_selector(".url")
    expect(response.body).to have_selector(".popup_form button#check_connection_p4", :text => "CHECK CONNECTION")
    expect(response.body).to have_selector(".popup_form #vcsconnection-message_p4", :text => "", visible: false)
  end

  it "should display new p4 material view with errors" do
    error = config_error(P4MaterialConfig::SERVER_AND_PORT, "Port is wrong")
    error.add(P4MaterialConfig::USERNAME, "Username is wrong")
    error.add(P4MaterialConfig::PASSWORD, "Password is wrong")
    error.add(P4MaterialConfig::VIEW, "View is wrong")
    error.add(P4MaterialConfig::USE_TICKETS, "Tickets are wrong")
    error.add(ScmMaterialConfig::MATERIAL_NAME, "Material Name is so wrong")
    error.add(ScmMaterialConfig::AUTO_UPDATE, "AUTO_UPDATE is wrong")
    error.add(ScmMaterialConfig::FOLDER, "Folder is wrong")
    set(@material_config, "errors", error)
    set(@ignored_file, "configErrors", config_error(com.thoughtworks.go.config.materials.IgnoredFiles::PATTERN, "Filter is wrong"))

    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/p4/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "foo"}}

    Capybara.string(response.body).find('.popup_form').tap do |popup_form|
      expect(popup_form).to have_selector("div.field_with_errors input[type='text'][name='material[#{com.thoughtworks.go.config.materials.AbstractMaterialConfig::MATERIAL_NAME}]'][value='P4 Material Name']")
      expect(popup_form).to have_selector("div.form_error", :text => "Material Name is so wrong")
      expect(popup_form).to have_selector("div.field_with_errors input[type='text'][name='material[#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::SERVER_AND_PORT}]'][value='p4:5000']")
      expect(popup_form).to have_selector("div.form_error", :text => "Port is wrong")
      expect(popup_form).to have_selector("div.field_with_errors input[type='text'][name='material[#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::USERNAME}]'][value='loser']")
      expect(popup_form).to have_selector("div.form_error", :text => "Username is wrong")
      expect(popup_form).to have_selector("div.field_with_errors input[type='password'][name='material[#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::PASSWORD}]'][value='secret']")
      expect(popup_form).to have_selector("div.form_error", :text => "Password is wrong")
      expect(popup_form).to have_selector("div.field_with_errors input[type='checkbox'][name='material[#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::USE_TICKETS}]'][value='true'][checked='checked']")
      expect(popup_form).to have_selector("div.form_error", :text => "Tickets are wrong")
      expect(popup_form).to have_selector("div.field_with_errors textarea[name='material[#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::VIEW}]']", "through_window")
      expect(popup_form).to have_selector("div.form_error", :text => "View is wrong")
      expect(popup_form).to have_selector("div.field_with_errors input[type='text'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::FOLDER}]'][value='dest']")
      expect(popup_form).to have_selector("div.form_error", :text => "Folder is wrong")
      expect(popup_form).to have_selector("div.field_with_errors input[type='checkbox'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::AUTO_UPDATE}]'][checked='checked']")
      expect(popup_form).to have_selector("div.form_error", :text => "AUTO_UPDATE is wrong")

      #Have skipped asserting on the div fieldWithError thats rendered around the text area , since the keys mismatch (pattern vs filter). Div around the actual text area is currently
      #not affecting functionality in any way.
      expect(popup_form).to have_selector("div.form_error", :text => "Filter is wrong")
    end
  end

  it "should display errors on P4Material view" do
    view = P4MaterialViewConfig.new("view")
    error = config_error(P4MaterialConfig::VIEW, "View is wrong")
    set(view, "configErrors", error)
    @material_config.setP4MaterialView(view)

    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/p4/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "foo"}}

    Capybara.string(response.body).find('.popup_form').tap do |popup_form|
      expect(popup_form).to have_selector("div.field_with_errors textarea[name='material[#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::VIEW}]']", :text => "view")
      expect(popup_form).to have_selector("div.form_error", :text => "View is wrong")
    end
  end

  it "should not generate the id for url, username and view fields" do
    render partial: "admin/materials/p4/form.html", locals: { scope: { material: @material_config, url: "http://google.com", method: "POST", submit_label: "foo" }}

    Capybara.string(response.body).all(".form_item .form_item_block").tap do |text_field|
      expect(text_field[1]).to_not have_selector("input[type='text'][class='form_input url'][id]")
      expect(text_field[3]).to_not have_selector("input[type='text'][class='form_input username'][id]")
      expect(text_field[5]).to_not have_selector("textarea[class='form_input view'][id]")
    end
  end
end
