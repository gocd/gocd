/*
 * Copyright 2021 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.helpers

import com.thoughtworks.go.domain.JobResult
import com.thoughtworks.go.domain.JobState
import com.thoughtworks.go.domain.MaterialRevisions
import com.thoughtworks.go.domain.PipelinePauseInfo
import com.thoughtworks.go.domain.buildcause.BuildCause
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel
import com.thoughtworks.go.presentation.pipelinehistory.PipelineModel
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels

class PipelineModelMother {
  static PipelineModel pipeline_model(String pipeline_name, String label_name, boolean no_history = false, boolean can_force = true, String pause_cause = null, boolean can_operate = true, MaterialRevisions revisions = new MaterialRevisions(), boolean can_administer = false) {
    def instance
    if (no_history) {
      instance = pipeline_instance_model_empty pipeline_name, "cruise-in-future"
    } else {
      instance = pipeline_instance_model([name: pipeline_name, label: label_name, counter: 5, stages: [[name: "cruise", counter: "10", approved_by: "Anonymous"]]])
    }

    pipeline_model_with_instances([instance], pipeline_name, can_force, pause_cause, can_operate, revisions, can_administer)
  }

  static PipelineModel pipeline_model_with_instances(instances, pipeline_name, can_force = true, pause_cause = null, can_operate = true, revisions = new MaterialRevisions(), can_administer = false) {
    def pipeline_model = new PipelineModel(pipeline_name, can_force, can_operate, pause_cause ? PipelinePauseInfo.paused(pause_cause, "raghu", new Date(12345)) : PipelinePauseInfo.notPaused())

    instances.each { instance ->
      pipeline_model.addPipelineInstance(instance)
    }
    pipeline_model.getLatestPipelineInstance().setMaterialRevisionsOnBuildCause(revisions)
    pipeline_model.updateAdministrability(can_administer)
    pipeline_model
  }

  static PipelineInstanceModel pipeline_instance_model_empty(pipeline_name, String... stage_names) {
    def stages = new StageInstanceModels()
    stage_names.each { stage_name ->
      stages.addFutureStage(stage_name, false)
    }
    PipelineInstanceModel.createEmptyPipelineInstanceModel(pipeline_name, BuildCause.createNeverRun(), stages)
  }

  static PipelineInstanceModel pipeline_instance_model(Map options = [name  : "pipeline1", label: "label1", counter: 1,
                                                                      stages: [[name     : "stage1", counter: "1", approved_by: "Anonymous",
                                                                                job_state: JobState.Completed, job_result: JobResult.Passed]]]) {
    def stages = new StageInstanceModels()

    options.stages.each { Map stage_detail ->
      def stage = com.thoughtworks.go.helpers.StageModelMother.stage_model(stage_detail.name, stage_detail.counter, stage_detail.job_state ?: JobState.Completed, stage_detail.job_result ?: JobResult.Passed)

      //  Sigh. The stage_model call above uses Time.now. If this is not done, two stages might have
      //  the same scheduled time causing tests to become unpredictable.
      sleep 10

      stage.setApprovedBy stage_detail.approved_by
      stages.add stage
    }

    def pipeline = PipelineInstanceModel.createPipeline(options.name, -1, options.label, BuildCause.createManualForced(), stages)
    pipeline.setCounter options.counter
    pipeline
  }
}
