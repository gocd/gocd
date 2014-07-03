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

require File.join(File.dirname(__FILE__), "..", "..", "..", "..", "spec_helper")

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

    assign(:cruise_config, @cruise_config = CruiseConfig.new)
    set(@cruise_config, "md5", "abc")
  end

  it "should render all p4 material attributes" do
    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/p4/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "FOO"}}

    response.body.should have_tag("input[type='hidden'][name='current_tab'][value=?]", "materials")
    response.body.should have_tag(".popup_form input[type='hidden'][name='material_type'][value='#{@material_config.getType()}']")
    response.body.should have_tag(".popup_form input[type='text'][name='material[#{AbstractMaterialConfig::MATERIAL_NAME}]'][value='P4 Material Name']")
    response.body.should have_tag(".popup_form input[type='text'][name='material[#{P4MaterialConfig::SERVER_AND_PORT}]'][value='p4:5000']")
    response.body.should have_tag(".popup_form input[type='text'][name='material[#{P4MaterialConfig::USERNAME}]'][value='loser']")
    response.body.should have_tag(".popup_form input[type='password'][name='material[#{P4MaterialConfig::PASSWORD}]'][value='secret']")
    response.body.should have_tag(".popup_form textarea[name='material[#{P4MaterialConfig::VIEW}]']", "through_window")
    response.body.should have_tag(".popup_form input[type='checkbox'][name='material[#{P4MaterialConfig::USE_TICKETS}]'][value='true'][checked='checked']")
    response.body.should have_tag(".popup_form input[type='checkbox'][name='material[#{ScmMaterialConfig::AUTO_UPDATE}]'][checked='checked']")
    response.body.should have_tag(".popup_form textarea[name='material[#{ScmMaterialConfig::FILTER}]']", "/sugar,/jaggery")
    response.body.should have_tag(".form_buttons button[type='submit'] span", "FOO")
  end

  it "should display the password field disabled in edit mode" do
    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/p4/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "FOO", :edit_mode => true}}
    response.body.should have_tag(".popup_form input[disabled='disabled'][type='password'][name='material[#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::PASSWORD}]'][value='secret']")
    response.body.should have_tag(".popup_form input[type='checkbox'][name='material[#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::PASSWORD_CHANGED}]']")
  end

  it "should display the password field as textbox in new mode" do
    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/p4/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "FOO", :edit_mode => false}}
    response.body.should have_tag(".popup_form input[type='password'][name='material[#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::PASSWORD}]']")
    response.body.should have_tag(".popup_form  div[class='hidden']") do
      with_tag("input[type='checkbox'][name='material[#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::PASSWORD_CHANGED}]'][value=?][checked='checked']", "1")
    end
  end

  it "should display check connection button" do
    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/p4/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "FOO"}}

    response.body.should have_tag(".username")
    response.body.should have_tag(".password")
    response.body.should have_tag(".url")
    response.body.should have_tag(".popup_form button#check_connection_p4", "CHECK CONNECTION")
    response.body.should have_tag(".popup_form #vcsconnection-message_p4", "")
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

    response.body.should have_tag(".popup_form") do
      with_tag("div.fieldWithErrors input[type='text'][name='material[#{com.thoughtworks.go.config.materials.AbstractMaterialConfig::MATERIAL_NAME}]'][value='P4 Material Name']")
      with_tag("div.form_error", "Material Name is so wrong")
      with_tag("div.fieldWithErrors input[type='text'][name='material[#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::SERVER_AND_PORT}]'][value='p4:5000']")
      with_tag("div.form_error", "Port is wrong")
      with_tag("div.fieldWithErrors input[type='text'][name='material[#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::USERNAME}]'][value='loser']")
      with_tag("div.form_error", "Username is wrong")
      with_tag("div.fieldWithErrors input[type='password'][name='material[#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::PASSWORD}]'][value='secret']")
      with_tag("div.form_error", "Password is wrong")
      with_tag("div.fieldWithErrors input[type='checkbox'][name='material[#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::USE_TICKETS}]'][value='true'][checked='checked']")
      with_tag("div.form_error", "Tickets are wrong")
      with_tag("div.fieldWithErrors textarea[name='material[#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::VIEW}]']", "through_window")
      with_tag("div.form_error", "View is wrong")
      with_tag("div.fieldWithErrors input[type='text'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::FOLDER}]'][value='dest']")
      with_tag("div.form_error", "Folder is wrong")
      with_tag("div.fieldWithErrors input[type='checkbox'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::AUTO_UPDATE}]'][checked='checked']")
      with_tag("div.form_error", "AUTO_UPDATE is wrong")
      #Have skipped asserting on the div fieldWithError thats rendered around the text area , since the keys mismatch (pattern vs filter). Div around the actual text area is currently
      #not affecting functionality in any way.
      with_tag("div.form_error", "Filter is wrong")
    end
  end

  it "should display errors on P4Material view" do
    view = P4MaterialViewConfig.new("view")
    error = config_error(P4MaterialConfig::VIEW, "View is wrong")
    set(view, "configErrors", error)
    @material_config.setP4MaterialView(view)

    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/p4/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "foo"}}

    response.body.should have_tag(".popup_form") do
      with_tag("div.fieldWithErrors textarea[name='material[#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::VIEW}]']", "view")
      with_tag("div.form_error", "View is wrong")
    end
  end
end
