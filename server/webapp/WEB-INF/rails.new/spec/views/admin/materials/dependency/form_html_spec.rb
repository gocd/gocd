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
    @material_config = DependencyMaterialConfig.new(CaseInsensitiveString.new("up-pipeline"), CaseInsensitiveString.new("up-stage"))
    @material_config.setName(CaseInsensitiveString.new("Dependency Material Name"))

    assign(:cruise_config, @cruise_config = BasicCruiseConfig.new)
    set(@cruise_config, "md5", "abc")
  end

  it "should render all dependency material attributes" do
    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/dependency/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "foo"}}

    expect(response.body).to have_selector("input[type='hidden'][name='current_tab'][value='materials']")
    expect(response.body).to have_selector(".popup_form input[type='text'][name='material[#{AbstractMaterialConfig::MATERIAL_NAME}]'][value='Dependency Material Name']")
    expect(response.body).to have_selector(".popup_form input[type='text'][name='material[#{DependencyMaterialConfig::PIPELINE_STAGE_NAME}]'][value='up-pipeline \[up-stage\]']")
  end

  it "should display new dependency material view with errors" do
    error = config_error(AbstractMaterialConfig::MATERIAL_NAME, "Material Name is so wrong")
    error.add(DependencyMaterialConfig::PIPELINE_STAGE_NAME, "Pipeline stage name is wrong")
    set(@material_config, "errors", error)

    in_params(:pipeline_name => "pipeline_name")

    render :partial => "admin/materials/dependency/form.html", :locals => {:scope => {:material => @material_config, :url => "http://google.com", :method => "POST", :submit_label => "foo"}}

    Capybara.string(response.body).find('.popup_form').tap do |popup_form|
      expect(popup_form).to have_selector("div.field_with_errors input[type='text'][name='material[#{AbstractMaterialConfig::MATERIAL_NAME}]'][value='Dependency Material Name']")
      expect(popup_form).to have_selector("div.form_error", :text => "Material Name is so wrong")

      expect(popup_form).to have_selector("div.field_with_errors input[type='text'][name='material[#{DependencyMaterialConfig::PIPELINE_STAGE_NAME}]'][value='up-pipeline \[up-stage\]']")
      expect(popup_form).to have_selector("div.form_error", :text => "Pipeline stage name is wrong")
    end
  end

  it "should not generate the id for input text field of material name" do
    render partial: "admin/materials/dependency/form.html", locals: { scope: { material: @material_config, url: "http://google.com", method: "POST", submit_label: "foo" }}

    Capybara.string(response.body).all(".form_item .form_item_block").tap do |material_name_text_field|
      expect(material_name_text_field[0]).to_not have_selector("input[type='text'][id]")
    end
  end
end
