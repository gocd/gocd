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

require File.expand_path(File.dirname(__FILE__) + '/../../spec_helper')

describe "/pipelines/_pipeline_group.html" do
  it "should cache pipeline partials of different pipelines separately" do
    pipeline_group1 = pipeline_group_model_for_caching(JobState::Building, JobResult::Unknown)
    pipeline_group2 = pipeline_group_model_for_caching(JobState::Completed, JobResult::Failed)
    key_proc = proc {|pipeline_group| [ViewCacheKey.new.forPipelineModelBox(pipeline_group.getPipelineModels().get(0)), {:subkey => "pipeline_html"}]}
    check_fragment_caching(pipeline_group1, pipeline_group2, key_proc) do |pipeline_group|
      render :partial => "pipelines/pipeline_group", :locals => {:scope => {:pipeline_group => pipeline_group}}
    end
  end

  it "should always show compare link" do
    pipeline_group = pipeline_group_model_for_caching(JobState::Building, JobResult::Unknown)
    render :partial => "pipelines/pipeline_group", :locals => {:scope => {:pipeline_group => pipeline_group}}

    response.body.should have_tag("span.compare_pipeline")
  end

  it "should render pipelines in the group using a partial" do
    pipeline_group = pipeline_group_model_for_caching(JobState::Building, JobResult::Unknown)

    attributes = {:pipeline_model => pipeline_group.getPipelineModels().get(0), :should_display_previously_blurb => true, :show_controls => true, :show_changes => true, :show_compare => true}
    template.should_receive(:render).with(:partial => "shared/pipeline.html", :locals => {:scope => attributes}).and_return("\"pipeline\"")

    render :partial => "pipelines/pipeline_group", :locals => {:scope => {:pipeline_group => pipeline_group}}
  end

  def pipeline_group_model_for_caching(latest_pipeline_job_state, latest_pipeline_job_result)
    model = PipelineModel.new("pipelineName", true, true, PipelinePauseInfo.notPaused())
    stages = StageInstanceModels.new
    stages.add(stage_instance("stageName", 13, latest_pipeline_job_state, latest_pipeline_job_result))
    stages.add(NullStageHistoryItem.new("stage2", true))
    pipeline_instance = PipelineInstanceModel.createPipeline("pipelineName", 10, "label-10", BuildCause.createExternal(), stages)
    pipeline_instance.setId(12)
    model.addPipelineInstance(pipeline_instance)

    stages2 = StageInstanceModels.new()
    stages2.add(stage_instance("stageName", 7, JobState::Completed, JobResult::Passed))
    stages2.add(stage_instance("stage2", 10, JobState::Assigned, JobResult::Unknown))
    pipeline_instance2 = PipelineInstanceModel.createPipeline("pipelineName", 7, "label-7", BuildCause.createExternal(), stages2)
    pipeline_instance2.setId(14)
    model.addPipelineInstance(pipeline_instance2)
    group = PipelineGroupModel.new("group1")
    group.add(model)
    group
  end

  def stage_instance(name, id, state, job_result)
    jobs = JobHistory.new()
    jobs.addJob("dev", state, job_result, java.util.Date.new())
    stage_instance = StageInstanceModel.new(name, "2", jobs)
    stage_instance.setId(id)
    stage_instance
  end
end
