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

describe "/pipelines/_pipeline_group.html" do
  include PipelineModelMother

  it "should cache pipeline partials of different pipelines separately" do
    pipeline_group1 = pipeline_group_model_for_caching("pipelineName", JobState::Building, JobResult::Unknown)
    pipeline_group2 = pipeline_group_model_for_caching("pipelineName", JobState::Completed, JobResult::Failed)
    key_proc = proc {|pipeline_group| [ViewCacheKey.new.forPipelineModelBox(pipeline_group.getPipelineModels().get(0)), {:subkey => "pipeline_html", :skip_digest => true}]}
    check_fragment_caching(pipeline_group1, pipeline_group2, key_proc) do |pipeline_group|
      render :partial => "pipelines/pipeline_group", :locals => {:scope => {:pipeline_group => pipeline_group}}
    end
  end

  it "should always show compare link" do
    pipeline_group = pipeline_group_model_for_caching("pipelineName", JobState::Building, JobResult::Unknown)
    render :partial => "pipelines/pipeline_group", :locals => {:scope => {:pipeline_group => pipeline_group}}

    expect(response.body).to have_selector("span.compare_pipeline")
  end

  it "should render a pipeline in the group using a partial" do
    pipeline_group = pipeline_group_model_for_caching("pipelineName", JobState::Building, JobResult::Unknown)
    common_attributes = {:should_display_previously_blurb => true, :show_controls => true, :show_changes => true, :show_compare => true}
    stub_template "shared/_pipeline.html.erb" => "PIPELINE"

    render :partial => "pipelines/pipeline_group", locals: {scope: {pipeline_group: pipeline_group}}

    assert_template partial: "shared/pipeline.html", locals: {scope: common_attributes.merge({pipeline_model: pipeline_group.getPipelineModels().get(0)})}
  end

  it "should render multiple pipelines in the group using a partial" do
    group_with_multiple_pipelines = PipelineGroupModel.new("group1")
    group_with_multiple_pipelines.add(create_pipeline_model("pipelineName_1", JobState::Building, JobResult::Unknown))
    group_with_multiple_pipelines.add(create_pipeline_model("pipelineName_2", JobState::Building, JobResult::Unknown))

    common_attributes = {:should_display_previously_blurb => true, :show_controls => true, :show_changes => true, :show_compare => true}
    stub_template "shared/_pipeline.html.erb" => "PIPELINE"

    render :partial => "pipelines/pipeline_group", locals: {scope: {pipeline_group: group_with_multiple_pipelines}}

    assert_template partial: "shared/pipeline.html", locals: {scope: common_attributes.merge({pipeline_model: group_with_multiple_pipelines.getPipelineModels().get(0)})}
    assert_template partial: "shared/pipeline.html", locals: {scope: common_attributes.merge({pipeline_model: group_with_multiple_pipelines.getPipelineModels().get(1)})}
  end

  def pipeline_group_model_for_caching(pipeline_name, latest_pipeline_job_state, latest_pipeline_job_result)
    group = PipelineGroupModel.new("group1")
    group.add(create_pipeline_model(pipeline_name, latest_pipeline_job_state, latest_pipeline_job_result))
    group
  end

  def create_pipeline_model(pipeline_name, latest_pipeline_job_state, latest_pipeline_job_result)
    model = PipelineModel.new(pipeline_name, true, true, PipelinePauseInfo.notPaused())
    stages = StageInstanceModels.new
    stages.add(stage_instance("stageName", 13, latest_pipeline_job_state, latest_pipeline_job_result))
    stages.add(NullStageHistoryItem.new("stage2", true))
    pipeline_instance = PipelineInstanceModel.createPipeline(pipeline_name, 10, "label-10", BuildCause.createExternal(), stages)
    pipeline_instance.setId(12)
    model.addPipelineInstance(pipeline_instance)

    stages2 = StageInstanceModels.new()
    stages2.add(stage_instance("stageName", 7, JobState::Completed, JobResult::Passed))
    stages2.add(stage_instance("stage2", 10, JobState::Assigned, JobResult::Unknown))
    pipeline_instance2 = PipelineInstanceModel.createPipeline(pipeline_name, 7, "label-7", BuildCause.createExternal(), stages2)
    pipeline_instance2.setId(14)
    model.addPipelineInstance(pipeline_instance2)
    model
  end

  def stage_instance(name, id, state, job_result)
    jobs = JobHistory.new()
    jobs.addJob("dev", state, job_result, java.util.Date.new())
    stage_instance = StageInstanceModel.new(name, "2", jobs)
    stage_instance.setId(id)
    stage_instance
  end

end
