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

describe "admin/stages/index.html.erb" do
  include GoUtil
  include FormUI

  STAGE_INDEX_PAGE = 'admin/stages/index.html.erb' unless defined?(STAGE_INDEX_PAGE)

  before(:each) do
    template.stub(:is_user_an_admin?).and_return(true)
    @pipeline = PipelineConfigMother.createPipelineConfigWithStages("pipeline-name", ["dev", "acceptance"].to_java(:string))
    assigns[:pipeline] = @pipeline

    @dev_stage = @pipeline.get(0)

    assigns[:cruise_config] = @cruise_config = CruiseConfig.new
    @cruise_config.addPipeline("group-1", @pipeline)

    assigns[:stage_usage] = java.util.HashSet.new
    assigns[:stage_usage].add(@dev_stage)
    assigns[:template_list] = ["defaultTemplate"]

    set(@cruise_config, "md5", "abc")
    in_params(:pipeline_name => "foo_bar", :action => "new", :controller => "admin/stages", :stage_parent => "pipelines")
  end

  it "should show stages" do
    render STAGE_INDEX_PAGE
    response.body.should have_tag("table.list_table") do
      with_tag("td a", @dev_stage.name().to_s)
    end

    response.body.should have_tag("input[type='radio'][checked='checked'][title='Define Stages']")
    response.body.should_not have_tag(".fieldWithErrors")
    response.body.should_not have_tag(".form_error")
  end

  it "should display stages of templated pipeline with trigger type when template is selected" do
    @pipeline = PipelineConfigMother.pipelineConfigWithTemplate("pipeline-name", "template-name")
    test_template = PipelineTemplateConfigMother.createTemplate("template-name")
    @cruise_config.stub(:getTemplateByName).and_return(test_template)
    assigns[:processed_cruise_config] = @processed_cruise_config = CruiseConfig.new
    @processed_cruise_config.stub(:pipelineConfigByName).and_return(PipelineConfigMother.createPipelineConfigWithStage("pipeline-name", test_template.first().name().toString()))
    assigns[:pipeline] = @pipeline
    render STAGE_INDEX_PAGE

    response.body.should have_tag("input[type='radio'][checked='checked'][title='Use Template']")
    response.body.should have_tag("input[type='radio'][disabled='disabled']")
    response.body.should have_tag("label[class='disabled']", "Define Stages")
  end

  it "should render templates dropdown always along with a edit link" do
    assigns[:template_list] = ["template1", "template2"]
    render STAGE_INDEX_PAGE

    response.body.should have_tag("select[id='select_template']") do
      with_tag("option[value='template1']")
      with_tag("option[value='template2']")
    end
    response.body.should have_tag("div#select_template_container div.form_buttons") do
      with_tag("button#submit_pipeline_edit_form[onclick='return false;']", "SAVE")
    end
    response.body.should have_tag("a.edit_template_link[href='#{template_edit_path(:pipeline_name => "template1", :stage_parent => "templates", :current_tab => 'general')}']", "Edit")
  end

  it "should render switch to template form with prompt on save pipeline" do
    assigns[:template_list] = ["template1", "template2"]
    render STAGE_INDEX_PAGE

    response.body.should have_tag("form#pipeline_edit_form[method='post']") do
      with_tag("button#submit_pipeline_edit_form[onclick='return false;']", "SAVE")
      with_tag("script[type='text/javascript']", /#warning_prompt/)
      with_tag("div#warning_prompt[style='display:none;']", /Switching to a template will cause all of the currently defined stages in this pipeline to be lost. Are you sure you want to continue\?/)
    end
  end

  it "should not render error div when no error exists" do
    assigns[:template_list] = ["template1", "template2"]

    render STAGE_INDEX_PAGE

    response.body.should_not have_tag(".form_error.template_form_error", "To assign this template, please fix the following template requirements for this pipeline:")
  end

  it "should not render templates dropdown when stage parent is templates" do
    in_params(:stage_parent => "templates")
    render STAGE_INDEX_PAGE
    response.body.should_not have_tag("select[id='select_template']")
    response.body.should have_tag("table.list_table") do
      with_tag("td a", @dev_stage.name().to_s)
    end
  end

  it "should disable the delete button if a stage is used as a material" do
    render STAGE_INDEX_PAGE
    response.body.should have_tag("tr.stage_dev td.remove span.icon_cannot_remove[title=?]", "Cannot delete this stage because it is used as a material in other pipelines")
  end

  it "should submit a form on deletion and prompt on deletion" do
    template.stub(:random_dom_id).and_return("delete_stage_random_id")
    render STAGE_INDEX_PAGE
    response.body.should have_tag("tr.stage_acceptance td.remove form#delete_stage_random_id") do
      with_tag("span#trigger_delete_stage_random_id.icon_remove")
      with_tag("script[type='text/javascript']", /Util.escapeDotsFromId\('trigger_delete_stage_random_id #warning_prompt'\)/)
      with_tag("div#warning_prompt[style='display:none;']", /Are you sure you want to delete the stage 'acceptance' \?/)
    end
  end

  it "should disable the delete button if there is only 1 stage" do
    @pipeline.remove(@dev_stage)
    assigns[:pipeline] = @pipeline
    render STAGE_INDEX_PAGE
    response.body.should have_tag("tr.stage_acceptance td.remove span.delete_icon_disabled[title=?]", "Cannot delete the only stage in a pipeline")
  end

  it "should display move stage down button" do
    render STAGE_INDEX_PAGE
    response.body.should have_tag("table.list_table") do
      with_tag("td form[action=?] button[type='submit'] .promote_down", admin_stage_increment_index_path(:pipeline_name => @pipeline.name(), :stage_name => @pipeline.get(0).name()))
    end
  end

  it "should disable the move stage down button for the last stage" do
    render STAGE_INDEX_PAGE
    response.body.should have_tag("table.list_table") do
      without_tag("td form[action=?] button[type='submit'] .promote_down", admin_stage_increment_index_path(:pipeline_name => @pipeline.name(), :stage_name => @pipeline.get(1).name()))
    end
  end

  it "should display move stage up button" do
    render STAGE_INDEX_PAGE
    response.body.should have_tag("table.list_table") do
      with_tag("td form[action=?] button[type='submit'] .promote_up", admin_stage_decrement_index_path(:pipeline_name => @pipeline.name(), :stage_name => @pipeline.get(1).name()))
    end
  end

  it "should disable the move stage up button for the first stage" do
    render STAGE_INDEX_PAGE
    response.body.should have_tag("table.list_table") do
      without_tag("td form[action=?] button[type='submit'] .promote_up", admin_stage_decrement_index_path(:pipeline_name => @pipeline.name(), :stage_name => @pipeline.get(0).name()))
    end
  end

  it "should display view template button when stage configuration uses templates" do
    assigns[:template_list] = ["template1", "template2"]
    render STAGE_INDEX_PAGE

    response.body.should have_tag("select[id='select_template']") do
      with_tag("option[value='template1']")
      with_tag("option[value='template2']")
    end
    response.body.should have_tag("a.view_template_link.skip_dirty_stop", "View")
    response.body.should_not have_tag(".no_templates_message")
  end

  it "should not render template dropdown and options when there are no templates. Should display relevant message" do
    assigns[:template_list] = []
    render STAGE_INDEX_PAGE

    response.body.should have_tag(".no_templates_message", "There are no templates configured")
    response.body.should_not have_tag(".template_selection")
  end

end
