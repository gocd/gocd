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

describe "admin/templates/edit.html.erb" do

  include ReflectiveUtil
  include GoUtil

  before(:each) do
    template = PipelineTemplateConfig.new(CaseInsensitiveString.new("template1"), [StageConfigMother.stageConfig("defaultStage")].to_java(StageConfig))
    assigns[:pipeline] = template
    assigns[:cruise_config] = @cruise_config = CruiseConfig.new
    set(@cruise_config, "md5", "abc")
    in_params(:pipeline_name => "template1")
  end

  it "should have form element to edit template name" do

    render "admin/templates/general.html"

    response.body.should have_tag("form[action=?][method='post'][id='template_edit_form']", template_update_path(:pipeline_name => 'template1', :current_tab => "general", :stage_parent => "templates")) do
      with_tag("input[name='_method'][type='hidden'][value='put']")
      with_tag("input[name='config_md5'][value=?]", "abc")
      with_tag("input[name='template[name]'][value=?][readonly='readonly'][disabled='disabled']", "template1")
    end

  end
end