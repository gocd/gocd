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

describe "admin/pipelines/template_form.html.erb" do

  before(:each) do
    @pipeline = PipelineConfigMother.createPipelineConfig("", "defaultStage", ["defaultJob"].to_java(java.lang.String))
    @pipeline_group = BasicPipelineConfigs.new
    @pipeline_group.add(@pipeline)
    @template1 = PipelineTemplateConfigMother.createTemplateWithParams("template.name", ["foo", "bar", "blah"].to_java(java.lang.String))
    @template2 = PipelineTemplateConfigMother.createTemplateWithParams("template.name.2", ["foo2"].to_java(java.lang.String))
    @templates = TemplatesConfig.new([@template1, @template2].to_java(PipelineTemplateConfig))
    fields_for(:pipeline_group, @pipeline_group) do |gf|
      gf.fields_for(:pipeline, @pipeline) do |f|
        @form = f
      end
    end
  end

  it "should render the template dropdown" do

    render :partial => "admin/pipelines/template_form.html", :locals => {:scope => {:pipeline => @pipeline, :template_list => @templates, :form => @form}}

    Capybara.string(response.body).find('div#select_template_container').tap do |div|
      div.find("select[name='pipeline_group[pipeline][templateName]']").tap do |select|
        expect(select).to have_selector("option[value='template.name']", :text => "template.name")
        expect(select).to have_selector("option[value='template.name.2']", :text => "template.name.2")
      end
      expect(div).to have_selector("a.view_template_link", :text => "View")
    end
  end

  it "should have a hidden section of parameters for a template" do
    scope = {:pipeline => @pipeline, :template_list => @templates, :form => @form}

    render :partial => "admin/pipelines/template_form.html", :locals => {:scope => scope}

    textarea_content = Capybara.string(response.body).find("textarea##{scope[:name_body_map][CaseInsensitiveString.new("template.name")]}").text

    expect(textarea_content).to have_selector("label", :text => "Define Parameters")
    expect(textarea_content).to have_selector("input[readonly='readonly'][name='pipeline_group[pipeline][params][][name]'][value='foo']")
    expect(textarea_content).to have_selector("input[readonly='readonly'][name='pipeline_group[pipeline][params][][name]'][value='bar']")
    expect(textarea_content).to have_selector("input[readonly='readonly'][name='pipeline_group[pipeline][params][][name]'][value='blah']")
    expect(textarea_content).to have_selector("input[name='pipeline_group[pipeline][params][][valueForDisplay]']")
    expect(textarea_content).to have_selector("input[name='pipeline_group[pipeline][params][][valueForDisplay]']")
    expect(textarea_content).to have_selector("input[name='pipeline_group[pipeline][params][][valueForDisplay]']")

    textarea_content = Capybara.string(response.body).find("textarea##{scope[:name_body_map][CaseInsensitiveString.new("template.name.2")]}").text

    expect(textarea_content).to have_selector("input[readonly='readonly'][name='pipeline_group[pipeline][params][][name]'][value='foo2']")
    expect(textarea_content).to have_selector("input[name='pipeline_group[pipeline][params][][valueForDisplay]']")

    expect(response.body).not_to have_selector("div.information");
  end

  it "should have empty hidden section when no parameters for a template" do
    template = PipelineTemplateConfigMother.createTemplate("template.name")
    templates = TemplatesConfig.new([template].to_java(PipelineTemplateConfig))
    scope = {:pipeline => @pipeline, :template_list => templates, :form => @form}

    render :partial => "admin/pipelines/template_form.html", :locals => {:scope => scope}

    expect(response.body).to have_selector("textarea##{scope[:name_body_map][CaseInsensitiveString.new("template.name")]}", /\s*/)
  end

  it "should provide message when no templates are available" do
    render :partial => "admin/pipelines/template_form.html", :locals => {:scope => {:pipeline => @pipeline, :template_list =>TemplatesConfig.new(), :form => @form}}

    Capybara.string(response.body).find("div#select_template_container").tap do |div|
      expect(div).to have_selector("div.information", :text => "There are no templates configured")
    end
    expect(response.body).not_to have_selector("div.fieldset");
  end

end
