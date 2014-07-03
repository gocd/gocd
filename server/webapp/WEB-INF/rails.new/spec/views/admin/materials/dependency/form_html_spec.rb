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
    @material_config = DependencyMaterialConfig.new(CaseInsensitiveString.new("up-pipeline"), CaseInsensitiveString.new("up-stage"))
    @material_config.setName(CaseInsensitiveString.new("Dependency Material Name"))

    assign(:cruise_config, @cruise_config = CruiseConfig.new)
    set(@cruise_config, "md5", "abc")
  end

  it "should render all dependency material attributes" do
    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/dependency/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "foo"}}

    response.body.should have_tag("input[type='hidden'][name='current_tab'][value=?]", "materials")
    response.body.should have_tag(".popup_form input[type='text'][name='material[#{AbstractMaterialConfig::MATERIAL_NAME}]'][value='Dependency Material Name']")
    response.body.should have_tag(".popup_form input[type='text'][name='material[#{DependencyMaterialConfig::PIPELINE_STAGE_NAME}]'][value='up-pipeline \[up-stage\]']")
  end

  it "should display new dependency material view with errors" do
    error = config_error(AbstractMaterialConfig::MATERIAL_NAME, "Material Name is so wrong")
    error.add(DependencyMaterialConfig::PIPELINE_STAGE_NAME, "Pipeline stage name is wrong")
    set(@material_config, "errors", error)

    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/dependency/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "foo"}}

    response.body.should have_tag(".popup_form") do
      with_tag("div.fieldWithErrors input[type='text'][name='material[#{AbstractMaterialConfig::MATERIAL_NAME}]'][value='Dependency Material Name']")
      with_tag("div.form_error", "Material Name is so wrong")

      with_tag("div.fieldWithErrors input[type='text'][name='material[#{DependencyMaterialConfig::PIPELINE_STAGE_NAME}]'][value='up-pipeline \[up-stage\]']")
      with_tag("div.form_error", "Pipeline stage name is wrong")

    end
  end
end