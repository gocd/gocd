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

describe ::ConfigUpdate::PipelineOrTemplateNode do
  include ::ConfigUpdate::PipelineOrTemplateNode

  before(:each) do
    allow(self).to receive(:params).and_return(@params = {})
    @cruise_config = BasicCruiseConfig.new
    @cruise_config.addPipeline("foo-group", @pipeline = PipelineConfigMother.createPipelineConfig("pipeline-bar", "stage-baz", ["job-foo"].to_java(java.lang.String)))
    @cruise_config.addTemplate(@template = PipelineTemplateConfig.new(CaseInsensitiveString.new("template-bar"), [StageConfigMother.stageConfig("stage-baz")].to_java(StageConfig)))
  end

  it "should load pipeline" do
    params[:stage_parent] = "pipelines"
    params[:pipeline_name] = "pipeline-bar"
    node(@cruise_config).should == @pipeline
  end

  it "should load template" do
    params[:stage_parent] = "templates"
    params[:pipeline_name] = "template-bar"
    node(@cruise_config).should == @template
  end
end
