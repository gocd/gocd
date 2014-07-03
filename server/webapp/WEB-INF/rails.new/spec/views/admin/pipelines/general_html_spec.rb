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

describe "admin/pipelines/general.html.erb" do
  include GoUtil, ReflectiveUtil

  PIPELINE_GENERAL_PAGE = 'admin/pipelines/general.html.erb' unless defined? PIPELINE_GENERAL_PAGE

  before(:each) do
    @pipeline = PipelineConfigMother.pipelineConfigWithTimer("pipeline-name", "1 1 1 1 1 1 1")
    assign(:pipeline, @pipeline)

    assign(:cruise_config, @cruise_config = CruiseConfig.new)
    @cruise_config.addPipeline("group-1", @pipeline)

    set(@cruise_config, "md5", "abc")
    in_params(:pipeline_name => "foo_bar", :action => "new", :controller => "admin/stages")
  end

  it "should render new form" do
    render PIPELINE_GENERAL_PAGE

    response.body.should have_tag("#pipeline_edit_form") do
      with_tag("input[type='hidden'][name='config_md5'][value='abc']")

      with_tag("div[class='contextual_help has_go_tip_right']")
      with_tag("input[type='checkbox'][name='pipeline[#{PipelineConfig::LOCK}]']")
      with_tag("input[type='text'][name='pipeline[#{PipelineConfig::NAME}]'][value='pipeline-name']")

      with_tag("input[type='text'][name='pipeline[#{PipelineConfig::LABEL_TEMPLATE}]'][value='${COUNT}']")

      with_tag("input[type='text'][name='pipeline[#{PipelineConfig::TIMER_CONFIG}][#{TimerConfig::TIMER_SPEC}]'][value='1 1 1 1 1 1 1']")

      with_tag("input[type='checkbox'][name='pipeline[#{PipelineConfig::TIMER_CONFIG}][#{TimerConfig::TIMER_ONLY_ON_CHANGES}]']")
    end
    response.body.should_not have_tag(".fieldWithErrors")
    response.body.should_not have_tag(".form_error")
  end

  it "should render form with approval type auto of the first stage" do
    render PIPELINE_GENERAL_PAGE

    response.body.should have_tag("#pipeline_edit_form") do
      with_tag("input[type='hidden'][name='pipeline[approval][type]'][value=?]", "manual")
      with_tag("input[type='checkbox'][name='pipeline[approval][type]'][checked='checked'][value=?]", "success")
      with_tag("label[for='pipeline_approval_type']", "Automatic pipeline scheduling")
      with_tag("div.contextual_help.has_go_tip_right[title=?]", "If unchecked, this pipeline will only schedule in response to a Manual/API/Timer trigger. Unchecking this box is the same as making the first stage manual.")
    end
  end

  it "should render form with approval type manual of the first stage" do
    @pipeline = PipelineConfigMother.pipelineConfigWithTimer("pipeline-name", "1 1 1 1 1 1 1")
    @pipeline.get(0).updateApproval(Approval.manualApproval())
    assign(:pipeline, @pipeline)

    render PIPELINE_GENERAL_PAGE

    response.body.should have_tag("#pipeline_edit_form") do
      without_tag("input[type='checkbox'][name='pipeline[approval][type]'][checked='checked']")
      with_tag("input[type='hidden'][name='pipeline[approval][type]'][value=?]", "manual")
      with_tag("input[type='checkbox'][name='pipeline[approval][type]'][value=?]", "success")
      with_tag("label[for='pipeline_approval_type']", "Automatic pipeline scheduling")
      with_tag("div.contextual_help.has_go_tip_right[title=?]", "If unchecked, this pipeline will only schedule in response to a Manual/API/Timer trigger. Unchecking this box is the same as making the first stage manual.")
    end
  end

  it "should render form with approval type as disabled if pipeline refers to a template" do
    @cruise_config = CruiseConfig.new
    @pipeline = GoConfigMother.new.addPipelineWithTemplate(@cruise_config, "pipeline", "template", "stage", ["job"].to_java(java.lang.String))
    assign(:pipeline, @pipeline)
    assign(:cruise_config, @cruise_config)

    render PIPELINE_GENERAL_PAGE

    response.body.should have_tag("#pipeline_edit_form") do
      with_tag("input[type='checkbox'][name='not_to_be_submitted'][checked='checked'][disabled='disabled']")
      with_tag("label[for='pipeline_approval_type']", "Automatic pipeline scheduling")
      with_tag("div.inline_instruction", "Since this pipeline is based on a template, automatic/manual behaviour is determined by the template (first stage)")
      with_tag("div.contextual_help.has_go_tip_right[title=?]", "If unchecked, this pipeline will only schedule in response to a Manual/API/Timer trigger. Unchecking this box is the same as making the first stage manual.")
    end
  end

  it "should render errors" do
    set(@pipeline, com.thoughtworks.go.config.PipelineConfig::LABEL_TEMPLATE, "bad-label-template")

    errors = config_error(PipelineConfig::LABEL_TEMPLATE, "Invalid label template")
    errors.add("lock", "Lock has a bad value")
    set(@pipeline, "errors", errors)
    set(@pipeline.getTimer(), "errors", config_error(TimerConfig::TIMER_SPEC, "Invalid timer spec"))

    render PIPELINE_GENERAL_PAGE

    response.body.should have_tag("#pipeline_edit_form") do
      with_tag("div.fieldWithErrors input[type='text'][name='pipeline[#{PipelineConfig::LABEL_TEMPLATE}]'][value='bad-label-template']")
      with_tag("div.form_error", "Invalid label template")
      
      with_tag("div.fieldWithErrors input[type='checkbox'][name='pipeline[#{PipelineConfig::LOCK}]']")
      with_tag("div.form_error", "Lock has a bad value")

      with_tag("div.fieldWithErrors input[type='text'][name='pipeline[#{PipelineConfig::TIMER_CONFIG}][#{TimerConfig::TIMER_SPEC}]'][value='1 1 1 1 1 1 1']")
      with_tag("div.form_error", "Invalid timer spec")
    end
  end

  def config_error(key, message)
    config_error = ConfigErrors.new()
    config_error.add(key, message)
    config_error
  end

end
