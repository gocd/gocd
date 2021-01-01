#
# Copyright 2021 ThoughtWorks, Inc.
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
#

module APIModelMother
  def create_pagination_model
    @pagination_view_model = double('PaginationViewModel')
    allow(@pagination_view_model).to receive(:getPageSize).and_return(10)
    allow(@pagination_view_model).to receive(:getOffset).and_return(1)
    allow(@pagination_view_model).to receive(:getTotal).and_return(100)
    @pagination_view_model
  end

  def create_empty_pagination_model
    @pagination_view_model = double('PaginationViewModel')
    allow(@pagination_view_model).to receive(:getPageSize).and_return(nil)
    allow(@pagination_view_model).to receive(:getOffset).and_return(nil)
    allow(@pagination_view_model).to receive(:getTotal).and_return(nil)
    @pagination_view_model
  end

  def create_material_view_model
    @material_view_model = double('MaterialViewModel')
    allow(@material_view_model).to receive(:getId).and_return(2)
    allow(@material_view_model).to receive(:getFingerprint).and_return('fingerprint')
    allow(@material_view_model).to receive(:getTypeForDisplay).and_return('git')
    allow(@material_view_model).to receive(:getLongDescription).and_return('URL: http://test.com Branch: master')
    @material_view_model
  end

  def create_empty_material_view_model
    @material_view_model = double('MaterialViewModel')
    allow(@material_view_model).to receive(:getId).and_return(nil)
    allow(@material_view_model).to receive(:getFingerprint).and_return(nil)
    allow(@material_view_model).to receive(:getTypeForDisplay).and_return(nil)
    allow(@material_view_model).to receive(:getLongDescription).and_return(nil)
    @material_view_model
  end

  def create_modified_file_view_model
    @modified_file_view_model = double('ModifiedFileViewModel')
    allow(@modified_file_view_model).to receive(:getFileName).and_return('file-name')
    allow(@modified_file_view_model).to receive(:getAction).and_return('add')
    @modified_file_view_model
  end

  def create_empty_modified_file_view_model
    @modified_file_view_model = double('ModifiedFileViewModel')
    allow(@modified_file_view_model).to receive(:getFileName).and_return(nil)
    allow(@modified_file_view_model).to receive(:getAction).and_return(nil)
    @modified_file_view_model
  end

  def create_modification_view_model
    @modification_view_model = double('ModificationViewModel')
    allow(@modification_view_model).to receive(:getId).and_return(321)
    allow(@modification_view_model).to receive(:getRevision).and_return('revision')
    time = double('time')
    allow(time).to receive(:getTime).and_return(12345678)
    allow(@modification_view_model).to receive(:getModifiedTime).and_return(time)
    allow(@modification_view_model).to receive(:getUserName).and_return('user name')
    allow(@modification_view_model).to receive(:getComment).and_return('comment')
    allow(@modification_view_model).to receive(:getEmailAddress).and_return('test@test.com')
    allow(@modification_view_model).to receive(:getModifiedFiles).and_return([create_modified_file_view_model])
    @modification_view_model
  end

  def create_empty_modification_view_model
    @modification_view_model = double('ModificationViewModel')
    allow(@modification_view_model).to receive(:getId).and_return(nil)
    allow(@modification_view_model).to receive(:getRevision).and_return(nil)
    allow(@modification_view_model).to receive(:getModifiedTime).and_return(nil)
    allow(@modification_view_model).to receive(:getUserName).and_return(nil)
    allow(@modification_view_model).to receive(:getComment).and_return(nil)
    allow(@modification_view_model).to receive(:getEmailAddress).and_return(nil)
    allow(@modification_view_model).to receive(:getModifiedFiles).and_return([create_empty_modified_file_view_model])
    @modification_view_model
  end

  def create_material_revision_view_model
    @material_revisions_view_model = double('MaterialRevisionsViewModel')
    allow(@material_revisions_view_model).to receive(:getMaterial).and_return(create_material_view_model)
    allow(@material_revisions_view_model).to receive(:isChanged).and_return(true)
    allow(@material_revisions_view_model).to receive(:getModifications).and_return([create_modification_view_model])
    @material_revisions_view_model
  end

  def create_empty_material_revision_view_model
    @material_revisions_view_model = double('MaterialRevisionsViewModel')
    allow(@material_revisions_view_model).to receive(:getMaterial).and_return(create_empty_material_view_model)
    allow(@material_revisions_view_model).to receive(:isChanged).and_return(nil)
    allow(@material_revisions_view_model).to receive(:getModifications).and_return([create_empty_modification_view_model])
    @material_revisions_view_model
  end

  def create_build_cause_model
    @build_cause_view_model = double('BuildCauseViewModel')
    allow(@build_cause_view_model).to receive(:getBuildCauseMessage).and_return('message')
    allow(@build_cause_view_model).to receive(:isForced).and_return(true)
    allow(@build_cause_view_model).to receive(:getApprover).and_return('me')
    allow(@build_cause_view_model).to receive(:getMaterialRevisions).and_return([create_material_revision_view_model])
    @build_cause_view_model
  end

  def create_empty_build_cause_model
    @build_cause_view_model = double('BuildCauseViewModel')
    allow(@build_cause_view_model).to receive(:getBuildCauseMessage).and_return(nil)
    allow(@build_cause_view_model).to receive(:isForced).and_return(nil)
    allow(@build_cause_view_model).to receive(:getApprover).and_return(nil)
    allow(@build_cause_view_model).to receive(:getMaterialRevisions).and_return([create_empty_material_revision_view_model])
    @build_cause_view_model
  end

  def create_job_history_model
    @job_view_model = double('JobViewModel')
    allow(@job_view_model).to receive(:getId).and_return(543)
    allow(@job_view_model).to receive(:getName).and_return('job name')
    allow(@job_view_model).to receive(:getState).and_return('state')
    allow(@job_view_model).to receive(:getResult).and_return('result')
    time = double('time')
    allow(time).to receive(:getTime).and_return(12345678)
    allow(@job_view_model).to receive(:getScheduledDate).and_return(time)
    @job_view_model
  end

  def create_empty_job_history_model
    @job_view_model = double('JobViewModel')
    allow(@job_view_model).to receive(:getId).and_return(nil)
    allow(@job_view_model).to receive(:getName).and_return(nil)
    allow(@job_view_model).to receive(:getState).and_return(nil)
    allow(@job_view_model).to receive(:getResult).and_return(nil)
    allow(@job_view_model).to receive(:getScheduledDate).and_return(nil)
    @job_view_model
  end

  def create_job_state_transition_model
    @job_state_transition_view_model = double('JobStateTransitionViewModel')
    allow(@job_state_transition_view_model).to receive(:getId).and_return(987)
    allow(@job_state_transition_view_model).to receive(:getCurrentState).and_return('building')
    time = double('time')
    allow(time).to receive(:getTime).and_return(12345678)
    allow(@job_state_transition_view_model).to receive(:getStateChangeTime).and_return(time)
    @job_state_transition_view_model
  end

  def create_empty_job_state_transition_model
    @job_state_transition_view_model = double('JobStateTransitionViewModel')
    allow(@job_state_transition_view_model).to receive(:getId).and_return(nil)
    allow(@job_state_transition_view_model).to receive(:getCurrentState).and_return(nil)
    allow(@job_state_transition_view_model).to receive(:getStateChangeTime).and_return(nil)
    @job_state_transition_view_model
  end

  def create_job_model
    @job_view_model = create_job_history_model
    allow(@job_view_model).to receive(:isRerun).and_return(false)
    allow(@job_view_model).to receive(:getOriginalJobId).and_return(0)
    allow(@job_view_model).to receive(:getAgentUuid).and_return('uuid')
    allow(@job_view_model).to receive(:getPipelineName).and_return('pipeline')
    allow(@job_view_model).to receive(:getPipelineCounter).and_return(123)
    allow(@job_view_model).to receive(:getStageName).and_return('stage')
    allow(@job_view_model).to receive(:getStageCounter).and_return('1')
    allow(@job_view_model).to receive(:getTransitions).and_return([create_job_state_transition_model])
    @job_view_model
  end

  def create_empty_job_model
    @job_view_model = create_empty_job_history_model
    allow(@job_view_model).to receive(:isRerun).and_return(nil)
    allow(@job_view_model).to receive(:getOriginalJobId).and_return(nil)
    allow(@job_view_model).to receive(:getAgentUuid).and_return(nil)
    allow(@job_view_model).to receive(:getPipelineName).and_return(nil)
    allow(@job_view_model).to receive(:getPipelineCounter).and_return(nil)
    allow(@job_view_model).to receive(:getStageName).and_return(nil)
    allow(@job_view_model).to receive(:getStageCounter).and_return(nil)
    allow(@job_view_model).to receive(:getTransitions).and_return([create_empty_job_state_transition_model])
    @job_view_model
  end

  def create_agent_job_run_history_model
    @job_view_model = create_job_model

    @job_history_view_model = double('JobHistoryViewModel')
    allow(@job_history_view_model).to receive(:getJobInstances).and_return([@job_view_model])
    @job_history_view_model
  end

  def create_empty_agent_job_run_history_model
    @job_view_model = create_empty_job_model

    @job_history_view_model = double('JobHistoryViewModel')
    allow(@job_history_view_model).to receive(:getJobInstances).and_return([@job_view_model])
    @job_history_view_model
  end

  def create_stage_model
    @stage_view_model = double('StageViewModel')
    allow(@stage_view_model).to receive(:getId).and_return(456)
    allow(@stage_view_model).to receive(:getName).and_return('stage name')
    allow(@stage_view_model).to receive(:getCounter).and_return('1')
    allow(@stage_view_model).to receive(:isScheduled).and_return(false)
    allow(@stage_view_model).to receive(:getApprovalType).and_return('manual')
    allow(@stage_view_model).to receive(:getApprovedBy).and_return('me')
    allow(@stage_view_model).to receive(:getResult).and_return('passed')
    allow(@stage_view_model).to receive(:getRerunOfCounter).and_return(1)
    allow(@stage_view_model).to receive(:hasOperatePermission).and_return('yes')
    allow(@stage_view_model).to receive(:getCanRun).and_return(true)
    allow(@stage_view_model).to receive(:getBuildHistory).and_return([create_job_history_model])
    allow(@stage_view_model).to receive(:getIdentifier).and_return('random')
    allow(@stage_view_model).to receive(:getPipelineName).and_return('pipeline')
    allow(@stage_view_model).to receive(:getPipelineCounter).and_return(1)
    @stage_view_model
  end

  def create_stage_model_for_instance
    @stage_identifier_view_model = double('StageIdentifierModel')
    allow(@stage_identifier_view_model).to receive(:getPipelineName).and_return('pipeline name')
    allow(@stage_identifier_view_model).to receive(:getPipelineCounter).and_return(1)

    @stage_view_model = double('StageInstanceViewModel')
    allow(@stage_view_model).to receive(:getId).and_return(456)
    allow(@stage_view_model).to receive(:getName).and_return('stage name')
    allow(@stage_view_model).to receive(:getCounter).and_return('1')
    allow(@stage_view_model).to receive(:getApprovalType).and_return('manual')
    allow(@stage_view_model).to receive(:getApprovedBy).and_return('me')
    allow(@stage_view_model).to receive(:getResult).and_return('passed')
    allow(@stage_view_model).to receive(:getRerunOfCounter).and_return(1)
    allow(@stage_view_model).to receive(:shouldFetchMaterials).and_return(true)
    allow(@stage_view_model).to receive(:shouldCleanWorkingDir).and_return(true)
    allow(@stage_view_model).to receive(:isArtifactsDeleted).and_return(true)
    allow(@stage_view_model).to receive(:getJobInstances).and_return([create_job_model])
    allow(@stage_view_model).to receive(:getIdentifier).and_return(@stage_identifier_view_model)
    @stage_view_model
  end

  def create_empty_stage_model
    @stage_view_model = double('StageViewModel')
    allow(@stage_view_model).to receive(:getId).and_return(nil)
    allow(@stage_view_model).to receive(:getName).and_return(nil)
    allow(@stage_view_model).to receive(:getCounter).and_return(nil)
    allow(@stage_view_model).to receive(:isScheduled).and_return(nil)
    allow(@stage_view_model).to receive(:getApprovalType).and_return(nil)
    allow(@stage_view_model).to receive(:getApprovedBy).and_return(nil)
    allow(@stage_view_model).to receive(:getResult).and_return(nil)
    allow(@stage_view_model).to receive(:getRerunOfCounter).and_return(nil)
    allow(@stage_view_model).to receive(:hasOperatePermission).and_return(nil)
    allow(@stage_view_model).to receive(:getCanRun).and_return(nil)
    allow(@stage_view_model).to receive(:getBuildHistory).and_return([create_empty_job_history_model])
    allow(@stage_view_model).to receive(:getIdentifier).and_return(nil)
    allow(@stage_view_model).to receive(:getPipelineName).and_return('not-required')
    allow(@stage_view_model).to receive(:getPipelineCounter).and_return('not-required')
    @stage_view_model
  end

  def create_pipeline_model
    @pipeline_view_model = double('PipelineViewModel')
    allow(@pipeline_view_model).to receive(:getId).and_return(321)
    allow(@pipeline_view_model).to receive(:getName).and_return('pipeline name')
    allow(@pipeline_view_model).to receive(:getCounter).and_return(123)
    allow(@pipeline_view_model).to receive(:getLabel).and_return('label')
    allow(@pipeline_view_model).to receive(:getNaturalOrder).and_return(1.0)
    allow(@pipeline_view_model).to receive(:getCanRun).and_return(true)
    allow(@pipeline_view_model).to receive(:isPreparingToSchedule).and_return(false)
    allow(@pipeline_view_model).to receive(:isCurrentlyLocked).and_return(false)
    allow(@pipeline_view_model).to receive(:isLockable).and_return(true)
    allow(@pipeline_view_model).to receive(:canUnlock).and_return(false)
    allow(@pipeline_view_model).to receive(:getBuildCause).and_return(create_build_cause_model)
    allow(@pipeline_view_model).to receive(:getStageHistory).and_return([create_stage_model])
    allow(@pipeline_view_model).to receive(:getComment).and_return('pipeline comment')
    @pipeline_view_model
  end

  def create_empty_pipeline_model
    @pipeline_view_model = double('PipelineViewModel')
    allow(@pipeline_view_model).to receive(:getId).and_return(nil)
    allow(@pipeline_view_model).to receive(:getName).and_return(nil)
    allow(@pipeline_view_model).to receive(:getCounter).and_return(nil)
    allow(@pipeline_view_model).to receive(:getLabel).and_return(nil)
    allow(@pipeline_view_model).to receive(:getNaturalOrder).and_return(nil)
    allow(@pipeline_view_model).to receive(:getCanRun).and_return(nil)
    allow(@pipeline_view_model).to receive(:isPreparingToSchedule).and_return(nil)
    allow(@pipeline_view_model).to receive(:isCurrentlyLocked).and_return(nil)
    allow(@pipeline_view_model).to receive(:isLockable).and_return(nil)
    allow(@pipeline_view_model).to receive(:canUnlock).and_return(nil)
    allow(@pipeline_view_model).to receive(:getBuildCause).and_return(create_empty_build_cause_model)
    allow(@pipeline_view_model).to receive(:getStageHistory).and_return([create_empty_stage_model])
    allow(@pipeline_view_model).to receive(:getComment).and_return(nil)
    @pipeline_view_model
  end

  def create_pipeline_history_model
    @pipeline_history_view_model = [create_pipeline_model]
    @pipeline_history_view_model
  end

  def create_empty_pipeline_history_model
    @pipeline_history_view_model = [create_empty_pipeline_model]
    @pipeline_history_view_model
  end

  def create_agent_model
    @agent_view_model = double('AgentViewModel')
    allow(@agent_view_model).to receive(:getUuid).and_return("uuid3")
    allow(@agent_view_model).to receive(:getHostname).and_return("CCeDev01")
    allow(@agent_view_model).to receive(:getIpAddress).and_return("127.0.0.1")
    allow(@agent_view_model).to receive(:getLocation).and_return("/var/lib/go-server")
    allow(@agent_view_model).to receive(:getStatusForDisplay).and_return("Idle")
    allow(@agent_view_model).to receive(:buildLocator).and_return("/pipeline/1/stage/1/job")
    allow(@agent_view_model).to receive(:getOperatingSystem).and_return("Linux")
    disk_space = DiskSpace.new(0)
    allow(@agent_view_model).to receive(:freeDiskSpace).and_return(disk_space)
    allow(@agent_view_model).to receive(:getResources).and_return(["java"])
    allow(@agent_view_model).to receive(:getEnvironments).and_return(["foo"])
    @agent_view_model
  end

  def create_empty_agent_model
    @agent_view_model = double('AgentViewModel')
    allow(@agent_view_model).to receive(:getUuid).and_return(nil)
    allow(@agent_view_model).to receive(:getHostname).and_return(nil)
    allow(@agent_view_model).to receive(:getIpAddress).and_return(nil)
    allow(@agent_view_model).to receive(:getLocation).and_return(nil)
    allow(@agent_view_model).to receive(:getStatusForDisplay).and_return(nil)
    allow(@agent_view_model).to receive(:buildLocator).and_return(nil)
    allow(@agent_view_model).to receive(:getOperatingSystem).and_return(nil)
    allow(@agent_view_model).to receive(:freeDiskSpace).and_return(nil)
    allow(@agent_view_model).to receive(:getResources).and_return(nil)
    allow(@agent_view_model).to receive(:getEnvironments).and_return(nil)
    @agent_view_model
  end

  def create_config_revision_model
    @config_revision_view_model = double('config_revision_view_model')
    allow(@config_revision_view_model).to receive(:getMd5).and_return('md5')
    allow(@config_revision_view_model).to receive(:getUsername).and_return('user name')
    allow(@config_revision_view_model).to receive(:getGoVersion).and_return('version')
    time = double('time')
    allow(time).to receive(:getTime).and_return(12345678)
    allow(@config_revision_view_model).to receive(:getTime).and_return(time)
    allow(@config_revision_view_model).to receive(:getSchemaVersion).and_return('schema')
    allow(@config_revision_view_model).to receive(:getCommitSHA).and_return('commit')
    @config_revision_view_model
  end

  def create_empty_config_revision_model
    @config_revision_view_model = double('config_revision_view_model')
    allow(@config_revision_view_model).to receive(:getMd5).and_return(nil)
    allow(@config_revision_view_model).to receive(:getUsername).and_return(nil)
    allow(@config_revision_view_model).to receive(:getGoVersion).and_return(nil)
    allow(@config_revision_view_model).to receive(:getTime).and_return(nil)
    allow(@config_revision_view_model).to receive(:getSchemaVersion).and_return(nil)
    allow(@config_revision_view_model).to receive(:getCommitSHA).and_return(nil)
    @config_revision_view_model
  end

  def create_pipeline_status_model
    @pipeline_status_model = double('PipelineStatusViewModel')
    allow(@pipeline_status_model).to receive(:isPaused).and_return(true)
    allow(@pipeline_status_model).to receive(:pausedCause).and_return('Pausing it for some reason')
    allow(@pipeline_status_model).to receive(:pausedBy).and_return('admin')
    allow(@pipeline_status_model).to receive(:isPaused).and_return(true)
    allow(@pipeline_status_model).to receive(:isLocked).and_return(true)
    allow(@pipeline_status_model).to receive(:isSchedulable).and_return(true)
    @pipeline_status_model
  end

  def create_stage_config_model
    @stage_config_view_model = double('StageConfigViewModel')
    allow(@stage_config_view_model).to receive(:name).and_return('stage name')
    @stage_config_view_model
  end

  def create_material_config_model
    @material_config_view_model = double('MaterialConfigViewModel')
    allow(@material_config_view_model).to receive(:getFingerprint).and_return('fingerprint')
    allow(@material_config_view_model).to receive(:getTypeForDisplay).and_return('git')
    allow(@material_config_view_model).to receive(:getLongDescription).and_return('URL: http://test.com Branch: master')
    @material_config_view_model
  end

  def create_pipeline_config_model
    @pipeline_config_view_model = double('PipelineConfigViewModel')
    allow(@pipeline_config_view_model).to receive(:name).and_return('pipeline name')
    allow(@pipeline_config_view_model).to receive(:getLabelTemplate).and_return('label')
    allow(@pipeline_config_view_model).to receive(:materialConfigs).and_return([create_material_config_model])
    allow(@pipeline_config_view_model).to receive(:getStages).and_return([create_stage_config_model])
    @pipeline_config_view_model
  end

  def create_pipeline_group_config_model
    @pipeline_group_config_view_model = double('PipelineGroupConfigViewModel')
    allow(@pipeline_group_config_view_model).to receive(:getGroup).and_return('pipeline group name')
    allow(@pipeline_group_config_view_model).to receive(:getPipelines).and_return([create_pipeline_config_model])
    @pipeline_group_config_view_model
  end
end

