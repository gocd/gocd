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

describe "admin/stages/index.html.erb" do
  include GoUtil
  include FormUI

  before(:each) do
    allow(view).to receive(:is_user_a_template_admin_for_template?).and_return(true)
    allow(view).to receive(:is_user_authorized_view_template?).and_return(true)
    @pipeline = PipelineConfigMother.createPipelineConfigWithStages("pipeline-name", ["dev", "acceptance"].to_java(:string))
    assign(:pipeline, @pipeline)

    @dev_stage = @pipeline.get(0)

    assign(:cruise_config, @cruise_config = BasicCruiseConfig.new)
    @cruise_config.addPipeline("group-1", @pipeline)

    stage_usage = java.util.HashSet.new
    assign(:stage_usage, stage_usage)
    stage_usage.add(@dev_stage)
    assign(:template_list, [TemplatesViewModel.new(PipelineTemplateConfigMother.createTemplate("defaultTemplate"), true, false)])

    set(@cruise_config, "md5", "abc")
    in_params(:pipeline_name => "foo_bar", :action => "new", :controller => "admin/stages", :stage_parent => "pipelines")
  end

  it "should show stages" do
    render

    Capybara.string(response.body).find('table.list_table').tap do |table|
      expect(table).to have_selector("td a", :text => @dev_stage.name().to_s)
    end

    expect(response.body).to have_selector("input[type='radio'][checked='checked'][title='Define Stages']")
    expect(response.body).not_to have_selector(".field_with_errors")
    expect(response.body).not_to have_selector(".form_error")
  end

  it "should display stages of templated pipeline with trigger type when template is selected" do
    @pipeline = PipelineConfigMother.pipelineConfigWithTemplate("pipeline-name", "template-name")
    test_template = PipelineTemplateConfigMother.createTemplate("template-name")
    allow(@cruise_config).to receive(:getTemplateByName).and_return(test_template)
    assign(:processed_cruise_config, @processed_cruise_config = BasicCruiseConfig.new)
    allow(@processed_cruise_config).to receive(:pipelineConfigByName).and_return(PipelineConfigMother.createPipelineConfigWithStage("pipeline-name", test_template.first().name().toString()))
    assign(:pipeline, @pipeline)

    render

    expect(response.body).to have_selector("input[type='radio'][checked='checked'][title='Use Template']")
    expect(response.body).to have_selector("input[type='radio'][disabled='disabled']")
    expect(response.body).to have_selector("label[class='disabled']", :text => "Define Stages")
  end

  it "should render switch to template form with prompt on save pipeline" do
    assign(:template_list, [TemplatesViewModel.new(PipelineTemplateConfigMother.createTemplate("template1"), true, false), TemplatesViewModel.new(PipelineTemplateConfigMother.createTemplate("template2"), true, false)])

    render

    Capybara.string(response.body).find("form#pipeline_edit_form[method='post']").tap do |form|
      expect(form).to have_selector("button#submit_pipeline_edit_form[onclick='return false;']", :text => "SAVE")
      expect(form).to have_selector("script[type='text/javascript']", :visible => false, :text => /#warning_prompt/)
      expect(form).to have_selector("div#warning_prompt[style='display:none;']", :visible => false, :text => /Switching to a template will cause all of the currently defined stages in this pipeline to be lost. Are you sure you want to continue\?/)
    end
  end

  it "should not render error div when no error exists" do
    assign(:template_list, [TemplatesViewModel.new(PipelineTemplateConfigMother.createTemplate("template1"), true, false), TemplatesViewModel.new(PipelineTemplateConfigMother.createTemplate("template2"), true, false)])

    render

    expect(response.body).not_to have_selector(".form_error.template_form_error", :text => "To assign this template, please fix the following template requirements for this pipeline:")
  end

  it "should not render templates dropdown when stage parent is templates" do
    in_params(:stage_parent => "templates")

    render

    expect(response.body).not_to have_selector("select[id='select_template']")

    Capybara.string(response.body).find("table.list_table").tap do |table|
      expect(table).to have_selector("td a")
    end
  end

  it "should disable the delete button if a stage is used as a material" do
    render

    expect(response.body).to have_selector("tr.stage_dev td.remove span.icon_cannot_remove[title='Cannot delete this stage because it is used as a material in other pipelines']")
  end

  it "should submit a form on deletion and prompt on deletion" do
    allow(view).to receive(:random_dom_id).and_return("delete_stage_random_id")

    render

    Capybara.string(response.body).find("tr.stage_acceptance td.remove form#delete_stage_random_id").tap do |tr|
      expect(tr).to have_selector("span#trigger_delete_stage_random_id.icon_remove")
      expect(tr).to have_selector("script[type='text/javascript']", :visible => false, :text => /Util.escapeDotsFromId\('trigger_delete_stage_random_id #warning_prompt'\)/)
      expect(tr).to have_selector("div#warning_prompt[style='display:none;']", :visible => false, :text => /Are you sure you want to delete the stage 'acceptance' \?/)
    end
  end

  it "should disable the delete button if there is only 1 stage" do
    @pipeline.remove(@dev_stage)
    assign(:pipeline, @pipeline)

    render

    expect(response.body).to have_selector("tr.stage_acceptance td.remove span.delete_icon_disabled[title='Cannot delete the only stage in a pipeline']")
  end

  it "should display move stage down button" do
    render

    Capybara.string(response.body).find("table.list_table").tap do |table|
      table.all("td").tap do |tds|
        tds[0].find("form[action='#{admin_stage_increment_index_path(:pipeline_name => @pipeline.name(), :stage_name => @pipeline.get(0).name())}']") do |form|
          form.find("button[type='submit']") do |button|
            expect(button).to have_selector(".promote_down")
          end
        end
      end
    end
  end

  it "should disable the move stage down button for the last stage" do
    render

    Capybara.string(response.body).find("table.list_table").tap do |table|
      table.find("tr.stage_acceptance").tap do |acceptance_stage_row|
        acceptance_stage_row.find("form[action='#{admin_stage_increment_index_path(:pipeline_name => @pipeline.name(), :stage_name => @pipeline.get(1).name())}']") do |form|
          expect(form).not_to have_selector(".promote_down")
        end
      end
    end
  end

  it "should display move stage up button" do
    render

    Capybara.string(response.body).find("table.list_table").tap do |table|
      table.find("tr.stage_acceptance") do |acceptance_stage_row|
        acceptance_stage_row.find("form[action='#{admin_stage_decrement_index_path(:pipeline_name => @pipeline.name(), :stage_name => @pipeline.get(1).name())}']") do |form|
          form.find("button[type='submit']") do |button|
            expect(button).to have_selector(".promote_up")
          end
        end
      end
    end
  end

  it "should disable the move stage up button for the first stage" do
    render

    Capybara.string(response.body).find("table.list_table").tap do |table|
      table.all("td").tap do |tds|
        tds[0].find("form[action='#{admin_stage_decrement_index_path(:pipeline_name => @pipeline.name(), :stage_name => @pipeline.get(0).name())}']") do |form|
          expect(form).not_to have_selector(".promote_up")
        end
      end
    end
  end

  it "should not render template dropdown and options when there are no templates. Should display relevant message" do
    assign(:template_list, [])

    render

    expect(response.body).to have_selector(".no_templates_message", :text => "There are no templates configured")
    expect(response.body).not_to have_selector(".template_selection")
  end

end
