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

describe "admin/pipelines/general.html.erb" do
  include GoUtil, ReflectiveUtil

  before(:each) do
    @pipeline = PipelineConfigMother.pipelineConfigWithTimer("pipeline-name", "1 1 1 1 1 1 1")
    assign(:pipeline, @pipeline)

    assign(:cruise_config, @cruise_config = BasicCruiseConfig.new)
    @cruise_config.addPipeline("group-1", @pipeline)

    set(@cruise_config, "md5", "abc")
    in_params(:pipeline_name => "foo_bar", :action => "new", :controller => "admin/stages")
  end

  it "should render new form" do
    render

    Capybara.string(response.body).find('#pipeline_edit_form').tap do |form|
      expect(form).to have_selector("input[type='hidden'][name='config_md5'][value='abc']")

      expect(form).to have_selector("div[class='contextual_help has_go_tip_right']")
      expect(form).to have_selector("input[type='checkbox'][name='pipeline[#{PipelineConfig::LOCK}]']")
      expect(form).to have_selector("input[type='text'][name='pipeline[#{PipelineConfig::NAME}]'][value='pipeline-name']")

      expect(form).to have_selector("input[type='text'][name='pipeline[#{PipelineConfig::LABEL_TEMPLATE}]'][value='${COUNT}']")

      expect(form).to have_selector("input[type='text'][name='pipeline[#{PipelineConfig::TIMER_CONFIG}][#{TimerConfig::TIMER_SPEC}]'][value='1 1 1 1 1 1 1']")

      expect(form).to have_selector("input[type='checkbox'][name='pipeline[#{PipelineConfig::TIMER_CONFIG}][#{TimerConfig::TIMER_ONLY_ON_CHANGES}]']")
    end
    expect(response.body).not_to have_selector(".field_with_errors")
    expect(response.body).not_to have_selector(".form_error")
  end

  it "should render form with approval type auto of the first stage" do
    render

    Capybara.string(response.body).find('#pipeline_edit_form').tap do |form|
      expect(form).to have_selector("input[type='hidden'][name='pipeline[approval][type]'][value='manual']")
      expect(form).to have_selector("input[type='checkbox'][name='pipeline[approval][type]'][checked='checked'][value='success']")
      expect(form).to have_selector("label[for='pipeline_approval_type']", :text => "Automatic pipeline scheduling")
      expect(form).to have_selector("div.contextual_help.has_go_tip_right[title='If unchecked, this pipeline will only schedule in response to a Manual/API/Timer trigger. Unchecking this box is the same as making the first stage manual.']")
    end
  end

  it "should render form with approval type manual of the first stage" do
    @pipeline = PipelineConfigMother.pipelineConfigWithTimer("pipeline-name", "1 1 1 1 1 1 1")
    @pipeline.get(0).updateApproval(Approval.manualApproval())
    assign(:pipeline, @pipeline)

    render

    Capybara.string(response.body).find('#pipeline_edit_form').tap do |form|
      expect(form).not_to have_selector("input[type='checkbox'][name='pipeline[approval][type]'][checked='checked']")

      expect(form).to have_selector("input[type='hidden'][name='pipeline[approval][type]'][value='manual']")
      expect(form).to have_selector("input[type='checkbox'][name='pipeline[approval][type]'][value='success']")
      expect(form).to have_selector("label[for='pipeline_approval_type']", :text => "Automatic pipeline scheduling")
      expect(form).to have_selector("div.contextual_help.has_go_tip_right[title='If unchecked, this pipeline will only schedule in response to a Manual/API/Timer trigger. Unchecking this box is the same as making the first stage manual.']")
    end
  end

  it "should render form with approval type as disabled if pipeline refers to a template" do
    @cruise_config = BasicCruiseConfig.new
    @pipeline = GoConfigMother.new.addPipelineWithTemplate(@cruise_config, "pipeline", "template", "stage", ["job"].to_java(java.lang.String))
    assign(:pipeline, @pipeline)
    assign(:cruise_config, @cruise_config)

    render

    Capybara.string(response.body).find('#pipeline_edit_form').tap do |form|
      expect(form).to have_selector("input[type='checkbox'][name='not_to_be_submitted'][checked='checked'][disabled='disabled']")
      expect(form).to have_selector("label[for='pipeline_approval_type']", :text => "Automatic pipeline scheduling")
      expect(form).to have_selector("div.inline_instruction", :text => "Since this pipeline is based on a template, automatic/manual behaviour is determined by the template (first stage)")
      expect(form).to have_selector("div.contextual_help.has_go_tip_right[title='If unchecked, this pipeline will only schedule in response to a Manual/API/Timer trigger. Unchecking this box is the same as making the first stage manual.']")
    end
  end

  it "should render errors" do
    set(@pipeline, com.thoughtworks.go.config.PipelineConfig::LABEL_TEMPLATE, "bad-label-template")

    errors = config_error(PipelineConfig::LABEL_TEMPLATE, "Invalid label template")
    errors.add("lock", "Lock has a bad value")
    set(@pipeline, "errors", errors)
    set(@pipeline.getTimer(), "errors", config_error(TimerConfig::TIMER_SPEC, "Invalid timer spec"))

    render

    Capybara.string(response.body).find('#pipeline_edit_form').tap do |form|
      expect(form).to have_selector("div.field_with_errors input[type='text'][name='pipeline[#{PipelineConfig::LABEL_TEMPLATE}]'][value='bad-label-template']")
      expect(form).to have_selector("div.form_error", :text => "Invalid label template")

      expect(form).to have_selector("div.field_with_errors input[type='checkbox'][name='pipeline[#{PipelineConfig::LOCK}]']")
      expect(form).to have_selector("div.form_error", :text => "Lock has a bad value")

      expect(form).to have_selector("div.field_with_errors input[type='text'][name='pipeline[#{PipelineConfig::TIMER_CONFIG}][#{TimerConfig::TIMER_SPEC}]'][value='1 1 1 1 1 1 1']")
      expect(form).to have_selector("div.form_error", :text => "Invalid timer spec")
    end
  end

  def config_error(key, message)
    config_error = ConfigErrors.new()
    config_error.add(key, message)
    config_error
  end

end
