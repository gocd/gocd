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

describe ::ConfigUpdate::JobsNode do
  include ::ConfigUpdate::JobsNode

  before(:each) do
    allow(self).to receive(:params).and_return(@params = {})
    @cruise_config = BasicCruiseConfig.new
    @cruise_config.addPipeline("go-group", pipeline = PipelineConfigMother.createPipelineConfig("pipeline", "stage", ["foo", "bar", "baz"].to_java(java.lang.String)))
    @jobs = pipeline.getStage(CaseInsensitiveString.new("stage")).getJobs()
  end

  it "should load job from jobs collection" do
    @params[:stage_parent] = "pipelines"
    @params[:pipeline_name] = "pipeline"
    @params[:stage_name] = "stage"
    @params[:job_name] = "bar"
    node(@cruise_config).should == @jobs
  end
end
