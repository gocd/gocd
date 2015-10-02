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

describe 'gadgets/pipeline/content.html.erb' do

  it "should render the given pipeline with absolute urls which open in a new window" do
    pipeline = PipelineModel.new("first", false, false, com.thoughtworks.go.domain.PipelinePauseInfo::notPaused)
    active_stage = PipelineHistoryMother::stagePerJob("stage", [PipelineHistoryMother::job(com.thoughtworks.go.domain.JobState::Building, com.thoughtworks.go.domain.JobResult::Unknown)]).first
    prev_stage = PipelineHistoryMother::stagePerJob("stage", [PipelineHistoryMother::job(com.thoughtworks.go.domain.JobState::Completed, com.thoughtworks.go.domain.JobResult::Failed)]).first
    active_stage.setPreviousStage(prev_stage)
    pipeline.addPipelineInstance(PipelineHistoryMother::singlePipeline("first", active_stage))
    assign(:pipeline, pipeline)

    render

    expect(response.body).to have_selector(".pipeline .pipeline_header h3.title a[href='http://test.host/tab/pipeline/history/first']", :text => "first")
    expect(response.body).to have_selector(".pipeline .pipeline_header h3.title a[target='_blank']")

    expect(response.body).to have_selector(".pipeline .pipeline_instance .status.details .label a[href='http://test.host/pipelines/value_stream_map/first/1']", :text => "1")
    expect(response.body).to have_selector(".pipeline .pipeline_instance .status.details .label a[target='_blank']")
    expect(response.body).to have_selector(".pipeline .pipeline_instance .status.details .label", :text => "Label:        1")
    expect(response.body).to have_selector(".pipeline .pipeline_instance .status.details .pipeline_instance_details")

    expect(response.body).to have_selector(".pipeline .pipeline_instance .stages a[href='http://test.host/pipelines/first/1/stage-0/1']")
    expect(response.body).to have_selector(".pipeline .pipeline_instance .stages a[target='_blank']")

    expect(response.body).to have_selector(".pipeline .pipeline_instance .previously_wrapper a[href='http://test.host/pipelines/pipeline/1/stage-0/1']")
    expect(response.body).to have_selector(".pipeline .pipeline_instance .previously_wrapper a[target='_blank']")
  end

  it "should cache pipeline partials of different pipelines separately" do
    pipeline1 = PipelineModel.new("first", false, false, com.thoughtworks.go.domain.PipelinePauseInfo::notPaused)
    pipeline1.addPipelineInstance(PipelineHistoryMother::singlePipeline("first", PipelineHistoryMother::stagePerJob("stage", [PipelineHistoryMother::job(com.thoughtworks.go.domain.JobResult::Passed)])))

    pipeline2 = PipelineModel.new("second", false, false, com.thoughtworks.go.domain.PipelinePauseInfo::notPaused)
    pipeline2.addPipelineInstance(PipelineHistoryMother::singlePipeline("second", PipelineHistoryMother::stagePerJob("stage", [PipelineHistoryMother::job(com.thoughtworks.go.domain.JobResult::Passed)])))

    key_proc = proc { |pipeline| [ViewCacheKey.new.forPipelineModelBox(pipeline), {:subkey => "pipeline_gadget_html", :skip_digest => true}] }
    check_fragment_caching(pipeline1, pipeline2, key_proc) do |pipeline|
      assign(:pipeline, pipeline)
      render
    end
  end
end
