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
    stages = StageInstanceModels.new
    if no_history
      stages.addFutureStage("cruise-in-future", false);
      pipeline = PipelineInstanceModel.createEmptyPipelineInstanceModel(pipeline_name, BuildCause.createNeverRun(), stages)
    else
      stages.add(stage = stage_model("cruise", "10"))
      stage.setApprovedBy("Anonymous")
      pipeline = PipelineInstanceModel.createPipeline(pipeline_name, -1, label_name, BuildCause.createManualForced(), stages)
      pipeline.setCounter(5)
    end
    pipeline_model = PipelineModel.new(pipeline_name, can_force, can_operate, pause_cause ? PipelinePauseInfo::paused(pause_cause,"raghu"):PipelinePauseInfo::notPaused())
    pipeline_model.addPipelineInstance(pipeline)
    pipeline_model.getLatestPipelineInstance().setMaterialRevisionsOnBuildCause(revisions)
    pipeline_model.updateAdministrability(can_administer)
    pipeline_model
  end
end