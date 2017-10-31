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

require File.expand_path(File.dirname(__FILE__) + '/stage_model_mother')
module PipelineModelMother
  include StageModelMother

  def pipeline_model(pipeline_name, label_name, no_history=false, can_force=true, pause_cause=nil, can_operate = true, revisions = MaterialRevisions.new([].to_java(MaterialRevision)), can_administer = false)
    if no_history
      instance = pipeline_instance_model_empty pipeline_name, "cruise-in-future"
    else
      instance = pipeline_instance_model({:name => pipeline_name, :label => label_name, :counter => 5, :stages => [{:name => "cruise", :counter => "10", :approved_by => "Anonymous"}]})
    end

    pipeline_model_with_instances [instance], pipeline_name, can_force, pause_cause, can_operate, revisions, can_administer
  end

  def pipeline_model_with_instances instances, pipeline_name, can_force=true, pause_cause=nil, can_operate = true, revisions = MaterialRevisions.new([].to_java(MaterialRevision)), can_administer = false
    pipeline_model = PipelineModel.new(pipeline_name, can_force, can_operate, pause_cause ? PipelinePauseInfo::paused(pause_cause, "raghu") : PipelinePauseInfo::notPaused())
    instances.each do |instance| pipeline_model.addPipelineInstance(instance) end
    pipeline_model.getLatestPipelineInstance().setMaterialRevisionsOnBuildCause(revisions)
    pipeline_model.updateAdministrability(can_administer)
    pipeline_model
  end

  def pipeline_instance_model_empty pipeline_name, *stage_names
    stages = StageInstanceModels.new
    stage_names.each do |stage_name| stages.addFutureStage(stage_name, false) end
    PipelineInstanceModel.createEmptyPipelineInstanceModel pipeline_name, BuildCause.createNeverRun(), stages
  end

  def pipeline_instance_model(options = {:name => "pipeline1", :label => "label1", :counter => 1,
                                         :stages => [{:name => "stage1", :counter => "1", :approved_by => "Anonymous",
                                                      :job_state => JobState::Completed, :job_result => JobResult::Passed}]})
    stages = StageInstanceModels.new
    options[:stages].each do |stage_detail|
      stage = stage_model stage_detail[:name], stage_detail[:counter], stage_detail[:job_state] || JobState::Completed, stage_detail[:job_result] || JobResult::Passed

      # Sigh. The stage_model call above uses Time.now. If this is not done, two stages might have
      # the same scheduled time causing tests to become unpredictable.
      sleep 0.1

      stage.setApprovedBy stage_detail[:approved_by]
      stages.add stage
    end

    pipeline = PipelineInstanceModel.createPipeline options[:name], -1, options[:label], BuildCause.createManualForced(), stages
    pipeline.setCounter options[:counter]
    pipeline
  end
end