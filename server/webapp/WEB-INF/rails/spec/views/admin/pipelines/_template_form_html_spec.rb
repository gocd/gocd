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

require File.join(File.dirname(__FILE__), "/../../../spec_helper")

describe "admin/pipelines/new.html.erb" do

  before(:each) do
    @pipeline = PipelineConfigMother.createPipelineConfig("", "defaultStage", ["defaultJob"].to_java(java.lang.String))
    @pipeline_group = PipelineConfigs.new
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

    response.body.should have_tag("div#select_template_container") do
      with_tag("select[name='pipeline_group[pipeline][templateName]']") do
        with_tag("option[value='template.name']", "template.name")
        with_tag("option[value='template.name.2']", "template.name.2")
      end
      with_tag("a.view_template_link", "View")
    end
  end

  it "should have a hidden section of parameters for a template" do
    scope = {:pipeline => @pipeline, :template_list => @templates, :form => @form}
    render :partial => "admin/pipelines/template_form.html", :locals => {:scope => scope}

    with_tag("textarea##{scope[:name_body_map][CaseInsensitiveString.new("template.name")]}") do
      with_tag("label", "Define Parameters")
      with_tag("input[readonly='readonly'][name='pipeline_group[pipeline][params][][name]'][value=?]", "foo")
      with_tag("input[readonly='readonly'][name='pipeline_group[pipeline][params][][name]'][value=?]", "bar")
      with_tag("input[readonly='readonly'][name='pipeline_group[pipeline][params][][name]'][value=?]", "blah")
      with_tag("input[name='pipeline_group[pipeline][params][][valueForDisplay]']")
      with_tag("input[name='pipeline_group[pipeline][params][][valueForDisplay]']")
      with_tag("input[name='pipeline_group[pipeline][params][][valueForDisplay]']")
    end
    with_tag("textarea##{scope[:name_body_map][CaseInsensitiveString.new("template.name.2")]}") do
      with_tag("input[readonly='readonly'][name='pipeline_group[pipeline][params][][name]'][value=?]", "foo2")
      with_tag("input[name='pipeline_group[pipeline][params][][valueForDisplay]']")
    end

    response.body.should_not have_tag("div.information");

  end

  it "should have empty hidden section when no parameters for a template" do
    template = PipelineTemplateConfigMother.createTemplate("template.name")
    templates = TemplatesConfig.new([template].to_java(PipelineTemplateConfig))
    scope = {:pipeline => @pipeline, :template_list => templates, :form => @form}

    render :partial => "admin/pipelines/template_form.html", :locals => {:scope => scope}

    with_tag("textarea##{scope[:name_body_map][CaseInsensitiveString.new("template.name")]}",/\s*/)
  end

  it "should provide message when no templates are available" do
    render :partial => "admin/pipelines/template_form.html", :locals => {:scope => {:pipeline => @pipeline, :template_list =>TemplatesConfig.new(), :form => @form}}
    response.body.should have_tag("div#select_template_container") do
      with_tag("div.information", "There are no templates configured")
    end
    response.body.should_not have_tag("div.fieldset");
  end

end