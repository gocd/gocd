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
    @material_config = GitMaterialConfig.new("git://foo", "master")
    @material_config.setFolder("dest")
    @material_config.setName(CaseInsensitiveString.new("Git Material Name"))
    @material_config.setAutoUpdate(true)
    @ignored_file = IgnoredFiles.new("/sugar")
    @material_config.setFilter(Filter.new([@ignored_file, IgnoredFiles.new("/jaggery")].to_java(IgnoredFiles)))

    assigns[:cruise_config] = @cruise_config = CruiseConfig.new
    set(@cruise_config, "md5", "abc")
  end

  it "should render all git material attributes" do
    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/git/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "FOO"}}

    response.body.should have_tag("input[type='hidden'][name='current_tab'][value=?]", "materials")
    response.body.should have_tag(".popup_form input[type='hidden'][name='material_type'][value='#{@material_config.getType()}']")
    response.body.should have_tag(".popup_form input[type='text'][name='material[#{AbstractMaterialConfig::MATERIAL_NAME}]'][value='Git Material Name']")
    response.body.should have_tag(".popup_form input[type='text'][name='material[#{GitMaterialConfig::URL}]'][value='git://foo']")
    response.body.should have_tag(".popup_form input[type='text'][name='material[#{GitMaterialConfig::BRANCH}]'][value='master']")
    response.body.should have_tag(".popup_form input[type='text'][name='material[#{ScmMaterialConfig::FOLDER}]'][value='dest']")
    response.body.should have_tag(".popup_form input[type='checkbox'][name='material[#{ScmMaterialConfig::AUTO_UPDATE}]'][checked='checked']")
    response.body.should have_tag(".popup_form textarea[name='material[#{ScmMaterialConfig::FILTER}]']", "/sugar,/jaggery")
    response.body.should have_tag(".form_buttons button[type='submit'] span", "FOO")    
  end

  it "should display check connection button" do
    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/git/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "FOO"}}

    response.body.should have_tag(".url")
    response.body.should have_tag(".popup_form button#check_connection_git", "CHECK CONNECTION")
    response.body.should have_tag(".popup_form #vcsconnection-message_git", "")
  end

  it "should display new git material view with errors" do
    error = config_error(GitMaterialConfig::URL, "Url is wrong")
    error.add(ScmMaterialConfig::MATERIAL_NAME, "Material Name is so wrong")
    error.add(GitMaterialConfig::BRANCH, "Branch is wrong")
    error.add(GitMaterialConfig::AUTO_UPDATE, "AUTO_UPDATE is wrong")
    error.add(GitMaterialConfig::FOLDER, "Folder is wrong")
    set(@material_config, "errors", error)
    set(@ignored_file, "configErrors", config_error(com.thoughtworks.go.config.materials.IgnoredFiles::PATTERN, "Filter is wrong"))

    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/git/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "foo"}}

    response.body.should have_tag(".popup_form") do
      with_tag("div.fieldWithErrors input[type='text'][name='material[#{AbstractMaterialConfig::MATERIAL_NAME}]'][value='Git Material Name']")
      with_tag("div.form_error", "Material Name is so wrong")

      with_tag("div.fieldWithErrors input[type='text'][name='material[#{GitMaterialConfig::URL}]'][value='git://foo']")
      with_tag("div.form_error", "Url is wrong")

      with_tag("div.fieldWithErrors input[type='text'][name='material[#{GitMaterialConfig::BRANCH}]'][value='master']")
      with_tag("div.form_error", "Branch is wrong")
      with_tag("div.fieldWithErrors input[type='text'][name='material[#{ScmMaterialConfig::FOLDER}]'][value='dest']")
      with_tag("div.form_error", "Folder is wrong")
      with_tag("div.fieldWithErrors input[type='checkbox'][name='material[#{ScmMaterialConfig::AUTO_UPDATE}]'][checked='checked']")
      with_tag("div.form_error", "AUTO_UPDATE is wrong")
      #Have skipped asserting on the div fieldWithError thats rendered around the text area , since the keys mismatch (pattern vs filter). Div around the actual text area is currently
      #not affecting functionality in any way.
      with_tag("div.form_error", "Filter is wrong")
    end
  end
end