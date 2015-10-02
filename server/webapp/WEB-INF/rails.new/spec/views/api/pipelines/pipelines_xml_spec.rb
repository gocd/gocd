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

describe "/api/pipelines/pipelines" do

  before do
    pipeline_config = PipelineMother.twoBuildPlansWithResourcesAndHgMaterialsAtUrl("uat", "default-stage", "http://foo:bar@baz.com:8000")
    @pipeline = PipelineHistoryMother.pipelineHistory(pipeline_config, @schedule_time = java.util.Date.new()).first
    @pipeline.setMaterialConfigs(pipeline_config.materialConfigs())
    pipelines = PipelineInstanceModels.createPipelineInstanceModels();
    assign(:pipelines, pipelines)
    pipelines.add(@pipeline)
    pipelines.add(@empty_pipeline = PipelineInstanceModel.createEmptyPipelineInstanceModel("pipeline", BuildCause.createWithEmptyModifications(), StageInstanceModels.new()))
  end

  it "should render all pipelines" do
    render :template => 'api/pipelines/pipelines.xml.erb'

    doc = Nokogiri::XML(response.body)

    pipelines = doc.xpath("pipelines/pipeline")
    expect(pipelines[0].attr("href")).to eq("http://test.host/api/pipelines/#{@pipeline.getName()}/stages.xml")
    expect(pipelines[1].attr("href")).to eq("http://test.host/api/pipelines/#{@empty_pipeline.getName()}/stages.xml")
  end

  it "should have a self referencing link" do
    render :template => '/api/pipelines/pipelines.xml.erb'

    doc = Nokogiri::XML(response.body)
    expect(doc.xpath("pipelines/link[@rel='self'][@href='http://test.host/api/pipelines.xml']")).to_not be_nil_or_empty
  end
end
