#
# Copyright 2019 ThoughtWorks, Inc.
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
#

require 'rails_helper'

describe "admin/templates/new.html.erb" do

  include ReflectiveUtil
  include GoUtil

  before(:each) do
    assign(:user, Username.new(CaseInsensitiveString.new("loser")))
    assign(:cruise_config, cruise_config = BasicCruiseConfig.new)
    set(cruise_config, "md5", "abcd1234")
    view.extend Admin::TemplatesHelper
    allow(view).to receive(:template_create_path).and_return("template_create_path")
  end

  it "should display form to create a new template" do
    allow(view).to receive(:allow_pipeline_selection?).and_return(true)
    assign(:pipeline, PipelineTemplateConfigViewModel.new)

    render

    Capybara.string(response.body).find("form[action='template_create_path'][method='post']").tap do |form|
      expect(form).to have_selector("input[name='config_md5'][value='abcd1234']", visible: :hidden)
      expect(form).to have_selector("input[name='pipeline[template][name]']")
    end
  end

  it "should display the pipeline from which to extract a template" do
    allow(view).to receive(:allow_pipeline_selection?).and_return(true)
    foo = PipelineTemplateConfigViewModel.new(PipelineTemplateConfig.new, nil, java.util.Arrays.asList([PipelineConfigMother.pipelineConfig("pipeline1"), PipelineConfigMother.pipelineConfig("pipeline.2"), PipelineConfigMother.pipelineConfig("Foo")].to_java(PipelineConfig)))
    assign(:pipeline, foo)

    render

    Capybara.string(response.body).find("form[action='template_create_path'][method='post']").tap do |form|
      expect(form).to have_selector("input[type='hidden'][name='pipeline[useExistingPipeline]']", visible: :hidden)
      expect(form).to have_selector("input#pipeline_useExistingPipeline[type='checkbox'][name='pipeline[useExistingPipeline]'][value='1'][class='pipeline_to_extract_selector']")
      expect(form).to have_selector("label[for='pipeline_useExistingPipeline']", :text => "Extract From Pipeline")

      expect(form).not_to have_selector("input#pipeline_useExistingPipeline[type='checkbox'][name='pipeline[useExistingPipeline]'][value='1'][class='pipeline_to_extract_selector'][disabled=disabled]")
      expect(form).not_to have_selector("label[for='pipeline_useExistingPipeline'][class='disabled']", :text => "Extract From Pipeline")

      expect(form).to have_selector("div.contextual_help.has_go_tip_right[title='If a pipeline is not selected, a template with a default stage and default job is created. If a pipeline is selected, the template will use the stages from the pipeline and the pipeline itself will start using this template.']")

      form.find("#pipelines_to_extract_from.hidden") do |pipelines_to_extract_from|
        pipelines_to_extract_from.find("select[name='pipeline[selectedPipelineName]']") do |select|
          expect(select).to have_selector("option[value='pipeline1']", :text => "pipeline1")
          expect(select).to have_selector("option[value='pipeline.2']", :text => "pipeline.2")
          expect(select).to have_selector("option[value='Foo']", :text => "Foo")
        end
      end
    end
  end

  it "should show the pipelines selection and check 'extract from' if useExistingPipeline is true" do
    allow(view).to receive(:allow_pipeline_selection?).and_return(true)
    assign(:pipeline, vm = PipelineTemplateConfigViewModel.new(PipelineTemplateConfig.new, 'pipeline1',java.util.Arrays.asList([PipelineConfigMother.pipelineConfig("pipeline1"), PipelineConfigMother.pipelineConfig("pipeline.2"), PipelineConfigMother.pipelineConfig("Foo")].to_java(PipelineConfig))))
    vm.setConfigAttributes( {"template" => {"name" => ""}, "useExistingPipeline" => "1", "pipelineNames" => "pipeline1", "selectedPipelineName" => "pipeline1"})

    render

    Capybara.string(response.body).find("form[action='template_create_path'][method='post']").tap do |form|
      expect(form).to have_selector("input[type='hidden'][name='pipeline[useExistingPipeline]']", visible: :hidden)
      expect(form).to have_selector("input#pipeline_useExistingPipeline[type='checkbox'][name='pipeline[useExistingPipeline]'][value='1'][class='pipeline_to_extract_selector']")
      expect(form).to have_selector("label[for='pipeline_useExistingPipeline']", :text => "Extract From Pipeline")

      expect(form).not_to have_selector("#pipelines_to_extract_from.hidden")

      expect(form).to have_selector("#pipelines_to_extract_from")
    end
  end

  it "should disable the pipelines selection if there are no pipelines" do
    allow(view).to receive(:allow_pipeline_selection?).and_return(true)
    assign(:pipeline, PipelineTemplateConfigViewModel.new(PipelineTemplateConfig.new, 'pipeline1', java.util.ArrayList.new()))

    render

    Capybara.string(response.body).find("form[action='template_create_path'][method='post']").tap do |form|
      expect(form).to have_selector("div.contextual_help.has_go_tip_right[title='No pipelines available for extracting template. Either all pipelines use templates already or no pipelines exists.']")

      expect(form).to have_selector("input[type='hidden'][name='pipeline[useExistingPipeline]']", visible: :hidden)
      expect(form).to have_selector("input#pipeline_useExistingPipeline[type='checkbox'][name='pipeline[useExistingPipeline]'][value='1'][class='pipeline_to_extract_selector'][disabled='disabled']")
      expect(form).to have_selector("label[for='pipeline_useExistingPipeline'][class='disabled']", :text => "Extract From Pipeline")
    end
  end

  it "should disable the pipelines selection and checkbox if pipeline to extract is pegged" do
    allow(view).to receive(:allow_pipeline_selection?).and_return(false)
    assign(:pipeline, PipelineTemplateConfigViewModel.new(PipelineTemplateConfig.new, 'pipeline1', java.util.ArrayList.new()))
    params[:pipelineToExtractFrom] = "temp_pipeline"

    render

    Capybara.string(response.body).find("form[action='template_create_path'][method='post']").tap do |form|
      expect(form).to have_selector("input[type='hidden'][name='pipeline[useExistingPipeline]'][value='1']", visible: :hidden)
      expect(form).to have_selector("input#pipeline_useExistingPipeline[type='checkbox'][name='pipeline[useExistingPipeline]'][value='1'][class='pipeline_to_extract_selector'][disabled='disabled']")
      expect(form).to have_selector("label[for='pipeline_useExistingPipeline'][class='disabled']", :text => "Extract From Pipeline")

      expect(form).to have_selector("select[disabled='disabled'][name='pipeline[pipelineNames]'][id='pipeline_pipelineNames']")
      expect(form).to have_selector("input#pipeline_selectedPipelineName[type='hidden'][value='pipeline1'][name='pipeline[selectedPipelineName]']", visible: :hidden)
    end
  end
end

