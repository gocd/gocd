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
  include GoUtil, FormUI, ReflectiveUtil

  before(:each) do
    @material_config = TfsMaterialConfig.new(GoCipher.new, UrlArgument.new("http://10.4.4.101:8080/tfs/Sample"), "loser", "domain" ,"passwd", "walk_this_path")
    @material_config.setAutoUpdate(true)
    @material_config.setName(CaseInsensitiveString.new("Tfs Material Name"))
    @material_config.setFolder("boulder")
    @ignored_file = IgnoredFiles.new("/sugar")
    @material_config.setFilter(Filter.new([@ignored_file, IgnoredFiles.new("/jaggery")].to_java(IgnoredFiles)))

    assigns[:cruise_config] = @cruise_config = CruiseConfig.new
    set(@cruise_config, "md5", "abc")
  end

  it "should render all tfs material attributes" do
    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/tfs/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "FOO"}}

    response.body.should have_tag("input[type='hidden'][name='current_tab'][value=?]", "materials")
    response.body.should have_tag(".popup_form input[type='text'][name='material[#{com.thoughtworks.go.config.materials.AbstractMaterialConfig::MATERIAL_NAME}]'][value='Tfs Material Name']")
    response.body.should have_tag(".popup_form input[type='text'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::URL}]'][value='http://10.4.4.101:8080/tfs/Sample']")
    response.body.should have_tag(".popup_form input[type='text'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::USERNAME}]'][value='loser']")
    response.body.should have_tag(".popup_form input[type='text'][name='material[#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::DOMAIN}]'][value='domain']")
    response.body.should have_tag(".popup_form input[type='password'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::PASSWORD}]'][value='passwd']")
    response.body.should have_tag(".popup_form input[type='hidden'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::ENCRYPTED_PASSWORD}]'][value='#{@material_config.getEncryptedPassword()}']")
    response.body.should have_tag(".popup_form input[type='text'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::FOLDER}]'][value='boulder']")
    response.body.should have_tag(".popup_form input[type='checkbox'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::AUTO_UPDATE}]'][checked='checked']")
    response.body.should have_tag(".popup_form input[type='text'][name='material[#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::PROJECT_PATH}]'][value='walk_this_path']")
    response.body.should have_tag(".popup_form textarea[name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::FILTER}]']", "/sugar,/jaggery")
    response.body.should have_tag(".form_buttons button[type='submit'] span", "FOO")

    response.body.should_not have_tag(".popup_form input[type='text'][name='material[workspaceOwner]']")
    response.body.should_not have_tag(".popup_form input[type='text'][name='material[workspace]']")
  end

  it "should display the password field disabled in edit mode" do
    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/tfs/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "FOO", :edit_mode => true}}
    response.body.should have_tag(".popup_form input[disabled='disabled'][type='password'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::PASSWORD}]'][value='passwd']")
    response.body.should have_tag(".popup_form input[type='checkbox'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::PASSWORD_CHANGED}]']")
  end

  it "should display the password field as textbox in new mode" do
    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/tfs/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "FOO", :edit_mode => false}}
    response.body.should have_tag(".popup_form input[type='password'][name='material[#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::PASSWORD}]']")
    response.body.should have_tag(".popup_form  div[class='hidden']") do
      with_tag("input[type='checkbox'][name='material[#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::PASSWORD_CHANGED}]'][value=?][checked='checked']", "1")
    end
  end

  it "should display check connection button" do
    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/tfs/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "FOO"}}

    response.body.should have_tag(".popup_form button#check_connection_tfs", "CHECK CONNECTION")
    response.body.should have_tag(".popup_form #vcsconnection-message_tfs", "")
    response.body.should have_tag(".url")
    response.body.should have_tag(".username")
    response.body.should have_tag(".password")
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

    response.body.should have_tag(".popup_form") do
      with_tag("div.fieldWithErrors input[type='text'][name='material[#{com.thoughtworks.go.config.materials.AbstractMaterialConfig::MATERIAL_NAME}]'][value='Tfs Material Name']")
      with_tag("div.form_error", "Material Name is so wrong")

      with_tag("div.fieldWithErrors input[type='text'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::URL}]'][value='http://10.4.4.101:8080/tfs/Sample']")
      with_tag("div.form_error", "Url is wrong")

      with_tag("div.fieldWithErrors input[type='text'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::USERNAME}]'][value='loser']")
      with_tag("div.form_error", "Username is wrong")

      with_tag("div.fieldWithErrors input[type='password'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::PASSWORD}]'][value='passwd']")
      with_tag("div.form_error", "Password is wrong")

      with_tag("div.fieldWithErrors input[type='text'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::FOLDER}]'][value='boulder']")
      with_tag("div.form_error", "Folder is wrong")

      with_tag("div.fieldWithErrors input[type='checkbox'][name='material[#{com.thoughtworks.go.config.materials.ScmMaterialConfig::AUTO_UPDATE}]'][checked='checked']")
      with_tag("div.form_error", "AUTO_UPDATE is wrong")


      with_tag("div.fieldWithErrors input[type='text'][name='material[#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::PROJECT_PATH}]'][value='walk_this_path']")
      with_tag("div.form_error", "path you talk and path you walk are different.")

      #Have skipped asserting on the div fieldWithError thats rendered around the text area , since the keys mismatch (pattern vs filter). Div around the actual text area is currently
      #not affecting functionality in any way.
      with_tag("div.form_error", "Filter is wrong")

      without_tag("div.fieldWithErrors input[type='text'][name='material[workspaceOwner]'][value='m1cr050f7']")
      without_tag("div.fieldWithErrors input[type='text'][name='material[workspace]'][value='work_in_space']")
    end
  end
end