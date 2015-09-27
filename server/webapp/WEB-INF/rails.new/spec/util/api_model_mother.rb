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

module APIModelMother
  def create_pagination_model
    @pagination_view_model = double('PaginationViewModel')
    @pagination_view_model.stub(:getPageSize).and_return(10)
    @pagination_view_model.stub(:getOffset).and_return(1)
    @pagination_view_model.stub(:getTotal).and_return(100)
    @pagination_view_model
  end

  def create_empty_pagination_model
    @pagination_view_model = double('PaginationViewModel')
    @pagination_view_model.stub(:getPageSize).and_return(nil)
    @pagination_view_model.stub(:getOffset).and_return(nil)
    @pagination_view_model.stub(:getTotal).and_return(nil)
    @pagination_view_model
  end

  def create_material_view_model
    @material_view_model = double('MaterialViewModel')
    @material_view_model.stub(:getId).and_return(2)
    @material_view_model.stub(:getFingerprint).and_return('fingerprint')
    @material_view_model.stub(:getTypeForDisplay).and_return('git')
    @material_view_model.stub(:getLongDescription).and_return('URL: http://test.com Branch: master')
    @material_view_model
  end

  def create_empty_material_view_model
    @material_view_model = double('MaterialViewModel')
    @material_view_model.stub(:getId).and_return(nil)
    @material_view_model.stub(:getFingerprint).and_return(nil)
    @material_view_model.stub(:getTypeForDisplay).and_return(nil)
    @material_view_model.stub(:getLongDescription).and_return(nil)
    @material_view_model
  end

  def create_modified_file_view_model
    @modified_file_view_model = double('ModifiedFileViewModel')
    @modified_file_view_model.stub(:getFileName).and_return('file-name')
    @modified_file_view_model.stub(:getAction).and_return('add')
    @modified_file_view_model
  end

  def create_empty_modified_file_view_model
    @modified_file_view_model = double('ModifiedFileViewModel')
    @modified_file_view_model.stub(:getFileName).and_return(nil)
    @modified_file_view_model.stub(:getAction).and_return(nil)
    @modified_file_view_model
  end

  def create_modification_view_model
    @modification_view_model = double('ModificationViewModel')
    @modification_view_model.stub(:getId).and_return(321)
    @modification_view_model.stub(:getRevision).and_return('revision')
    time = double('time')
    time.stub(:getTime).and_return(12345678)
    @modification_view_model.stub(:getModifiedTime).and_return(time)
    @modification_view_model.stub(:getUserName).and_return('user name')
    @modification_view_model.stub(:getComment).and_return('comment')
    @modification_view_model.stub(:getEmailAddress).and_return('test@test.com')
    @modification_view_model.stub(:getModifiedFiles).and_return([create_modified_file_view_model])
    @modification_view_model
  end

  def create_empty_modification_view_model
    @modification_view_model = double('ModificationViewModel')
    @modification_view_model.stub(:getId).and_return(nil)
    @modification_view_model.stub(:getRevision).and_return(nil)
    @modification_view_model.stub(:getModifiedTime).and_return(nil)
    @modification_view_model.stub(:getUserName).and_return(nil)
    @modification_view_model.stub(:getComment).and_return(nil)
    @modification_view_model.stub(:getEmailAddress).and_return(nil)
    @modification_view_model.stub(:getModifiedFiles).and_return([create_empty_modified_file_view_model])
    @modification_view_model
  end

  def create_material_revision_view_model
    @material_revisions_view_model = double('MaterialRevisionsViewModel')
    @material_revisions_view_model.stub(:getMaterial).and_return(create_material_view_model)
    @material_revisions_view_model.stub(:isChanged).and_return(true)
    @material_revisions_view_model.stub(:getModifications).and_return([create_modification_view_model])
    @material_revisions_view_model
  end

  def create_empty_material_revision_view_model
    @material_revisions_view_model = double('MaterialRevisionsViewModel')
    @material_revisions_view_model.stub(:getMaterial).and_return(create_empty_material_view_model)
    @material_revisions_view_model.stub(:isChanged).and_return(nil)
    @material_revisions_view_model.stub(:getModifications).and_return([create_empty_modification_view_model])
    @material_revisions_view_model
  end

  def create_build_cause_model
    @build_cause_view_model = double('BuildCauseViewModel')
    @build_cause_view_model.stub(:getBuildCauseMessage).and_return('message')
    @build_cause_view_model.stub(:isForced).and_return(true)
    @build_cause_view_model.stub(:getApprover).and_return('me')
    @build_cause_view_model.stub(:getMaterialRevisions).and_return([create_material_revision_view_model])
    @build_cause_view_model
  end

  def create_empty_build_cause_model
    @build_cause_view_model = double('BuildCauseViewModel')
    @build_cause_view_model.stub(:getBuildCauseMessage).and_return(nil)
    @build_cause_view_model.stub(:isForced).and_return(nil)
    @build_cause_view_model.stub(:getApprover).and_return(nil)
    @build_cause_view_model.stub(:getMaterialRevisions).and_return([create_empty_material_revision_view_model])
    @build_cause_view_model
  end

  def create_job_history_model
    @job_view_model = double('JobViewModel')
    @job_view_model.stub(:getId).and_return(543)
    @job_view_model.stub(:getName).and_return('job name')
    @job_view_model.stub(:getState).and_return('state')
    @job_view_model.stub(:getResult).and_return('result')
    time = double('time')
    time.stub(:getTime).and_return(12345678)
    @job_view_model.stub(:getScheduledDate).and_return(time)
    @job_view_model
  end

  def create_empty_job_history_model
    @job_view_model = double('JobViewModel')
    @job_view_model.stub(:getId).and_return(nil)
    @job_view_model.stub(:getName).and_return(nil)
    @job_view_model.stub(:getState).and_return(nil)
    @job_view_model.stub(:getResult).and_return(nil)
    @job_view_model.stub(:getScheduledDate).and_return(nil)
    @job_view_model
  end

  def create_job_state_transition_model
    @job_state_transition_view_model = double('JobStateTransitionViewModel')
    @job_state_transition_view_model.stub(:getId).and_return(987)
    @job_state_transition_view_model.stub(:getCurrentState).and_return('building')
    time = double('time')
    time.stub(:getTime).and_return(12345678)
    @job_state_transition_view_model.stub(:getStateChangeTime).and_return(time)
    @job_state_transition_view_model
  end

  def create_empty_job_state_transition_model
    @job_state_transition_view_model = double('JobStateTransitionViewModel')
    @job_state_transition_view_model.stub(:getId).and_return(nil)
    @job_state_transition_view_model.stub(:getCurrentState).and_return(nil)
    @job_state_transition_view_model.stub(:getStateChangeTime).and_return(nil)
    @job_state_transition_view_model
  end

  def create_job_model
    @job_view_model = create_job_history_model
    @job_view_model.stub(:isRerun).and_return(false)
    @job_view_model.stub(:getOriginalJobId).and_return(0)
    @job_view_model.stub(:getAgentUuid).and_return('uuid')
    @job_view_model.stub(:getPipelineName).and_return('pipeline')
    @job_view_model.stub(:getPipelineCounter).and_return(123)
    @job_view_model.stub(:getStageName).and_return('stage')
    @job_view_model.stub(:getStageCounter).and_return('1')
    @job_view_model.stub(:getTransitions).and_return([create_job_state_transition_model])
    @job_view_model
  end

  def create_empty_job_model
    @job_view_model = create_empty_job_history_model
    @job_view_model.stub(:isRerun).and_return(nil)
    @job_view_model.stub(:getOriginalJobId).and_return(nil)
    @job_view_model.stub(:getAgentUuid).and_return(nil)
    @job_view_model.stub(:getPipelineName).and_return(nil)
    @job_view_model.stub(:getPipelineCounter).and_return(nil)
    @job_view_model.stub(:getStageName).and_return(nil)
    @job_view_model.stub(:getStageCounter).and_return(nil)
    @job_view_model.stub(:getTransitions).and_return([create_empty_job_state_transition_model])
    @job_view_model
  end

  def create_agent_job_run_history_model
    @job_view_model = create_job_model

    @job_history_view_model = double('JobHistoryViewModel')
    @job_history_view_model.stub(:getJobInstances).and_return([@job_view_model])
    @job_history_view_model
  end

  def create_empty_agent_job_run_history_model
    @job_view_model = create_empty_job_model

    @job_history_view_model = double('JobHistoryViewModel')
    @job_history_view_model.stub(:getJobInstances).and_return([@job_view_model])
    @job_history_view_model
  end

  def create_stage_model
    @stage_view_model = double('StageViewModel')
    @stage_view_model.stub(:getId).and_return(456)
    @stage_view_model.stub(:getName).and_return('stage name')
    @stage_view_model.stub(:getCounter).and_return('1')
    @stage_view_model.stub(:isScheduled).and_return(false)
    @stage_view_model.stub(:getApprovalType).and_return('manual')
    @stage_view_model.stub(:getApprovedBy).and_return('me')
    @stage_view_model.stub(:getResult).and_return('passed')
    @stage_view_model.stub(:getRerunOfCounter).and_return(1)
    @stage_view_model.stub(:hasOperatePermission).and_return('yes')
    @stage_view_model.stub(:getCanRun).and_return(true)
    @stage_view_model.stub(:getBuildHistory).and_return([create_job_history_model])
    @stage_view_model.stub(:getIdentifier).and_return('random')
    @stage_view_model.stub(:getPipelineName).and_return('pipeline')
    @stage_view_model.stub(:getPipelineCounter).and_return(1)
    @stage_view_model
  end

  def create_stage_model_for_instance
    @stage_identifier_view_model = double('StageIdentifierModel')
    @stage_identifier_view_model.stub(:getPipelineName).and_return('pipeline name')
    @stage_identifier_view_model.stub(:getPipelineCounter).and_return(1)

    @stage_view_model = double('StageInstanceViewModel')
    @stage_view_model.stub(:getId).and_return(456)
    @stage_view_model.stub(:getName).and_return('stage name')
    @stage_view_model.stub(:getCounter).and_return('1')
    @stage_view_model.stub(:getApprovalType).and_return('manual')
    @stage_view_model.stub(:getApprovedBy).and_return('me')
    @stage_view_model.stub(:getResult).and_return('passed')
    @stage_view_model.stub(:getRerunOfCounter).and_return(1)
    @stage_view_model.stub(:shouldFetchMaterials).and_return(true)
    @stage_view_model.stub(:shouldCleanWorkingDir).and_return(true)
    @stage_view_model.stub(:isArtifactsDeleted).and_return(true)
    @stage_view_model.stub(:getJobInstances).and_return([create_job_model])
    @stage_view_model.stub(:getIdentifier).and_return(@stage_identifier_view_model)
    @stage_view_model
  end

  def create_empty_stage_model
    @stage_view_model = double('StageViewModel')
    @stage_view_model.stub(:getId).and_return(nil)
    @stage_view_model.stub(:getName).and_return(nil)
    @stage_view_model.stub(:getCounter).and_return(nil)
    @stage_view_model.stub(:isScheduled).and_return(nil)
    @stage_view_model.stub(:getApprovalType).and_return(nil)
    @stage_view_model.stub(:getApprovedBy).and_return(nil)
    @stage_view_model.stub(:getResult).and_return(nil)
    @stage_view_model.stub(:getRerunOfCounter).and_return(nil)
    @stage_view_model.stub(:hasOperatePermission).and_return(nil)
    @stage_view_model.stub(:getCanRun).and_return(nil)
    @stage_view_model.stub(:getBuildHistory).and_return([create_empty_job_history_model])
    @stage_view_model.stub(:getIdentifier).and_return(nil)
    @stage_view_model.stub(:getPipelineName).and_return('not-required')
    @stage_view_model.stub(:getPipelineCounter).and_return('not-required')
    @stage_view_model
  end

  def create_pipeline_model
    @pipeline_view_model = double('PipelineViewModel')
    @pipeline_view_model.stub(:getId).and_return(321)
    @pipeline_view_model.stub(:getName).and_return('pipeline name')
    @pipeline_view_model.stub(:getCounter).and_return(123)
    @pipeline_view_model.stub(:getLabel).and_return('label')
    @pipeline_view_model.stub(:getNaturalOrder).and_return(1.0)
    @pipeline_view_model.stub(:getCanRun).and_return(true)
    @pipeline_view_model.stub(:isPreparingToSchedule).and_return(false)
    @pipeline_view_model.stub(:isCurrentlyLocked).and_return(false)
    @pipeline_view_model.stub(:isLockable).and_return(true)
    @pipeline_view_model.stub(:canUnlock).and_return(false)
    @pipeline_view_model.stub(:getBuildCause).and_return(create_build_cause_model)
    @pipeline_view_model.stub(:getStageHistory).and_return([create_stage_model])
    @pipeline_view_model.stub(:getComment).and_return('pipeline comment')
    @pipeline_view_model
  end

  def create_empty_pipeline_model
    @pipeline_view_model = double('PipelineViewModel')
    @pipeline_view_model.stub(:getId).and_return(nil)
    @pipeline_view_model.stub(:getName).and_return(nil)
    @pipeline_view_model.stub(:getCounter).and_return(nil)
    @pipeline_view_model.stub(:getLabel).and_return(nil)
    @pipeline_view_model.stub(:getNaturalOrder).and_return(nil)
    @pipeline_view_model.stub(:getCanRun).and_return(nil)
    @pipeline_view_model.stub(:isPreparingToSchedule).and_return(nil)
    @pipeline_view_model.stub(:isCurrentlyLocked).and_return(nil)
    @pipeline_view_model.stub(:isLockable).and_return(nil)
    @pipeline_view_model.stub(:canUnlock).and_return(nil)
    @pipeline_view_model.stub(:getBuildCause).and_return(create_empty_build_cause_model)
    @pipeline_view_model.stub(:getStageHistory).and_return([create_empty_stage_model])
    @pipeline_view_model.stub(:getComment).and_return(nil)
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
    @agent_view_model.stub(:getUuid).and_return("uuid3")
    @agent_view_model.stub(:getHostname).and_return("CCeDev01")
    @agent_view_model.stub(:getIpAddress).and_return("127.0.0.1")
    @agent_view_model.stub(:getLocation).and_return("/var/lib/go-server")
    @agent_view_model.stub(:getStatusForDisplay).and_return("Idle")
    @agent_view_model.stub(:buildLocator).and_return("/pipeline/1/stage/1/job")
    @agent_view_model.stub(:getOperatingSystem).and_return("Linux")
    disk_space = DiskSpace.new(0)
    @agent_view_model.stub(:freeDiskSpace).and_return(disk_space)
    @agent_view_model.stub(:getResources).and_return(["java"])
    @agent_view_model.stub(:getEnvironments).and_return(["foo"])
    @agent_view_model
  end

  def create_empty_agent_model
    @agent_view_model = double('AgentViewModel')
    @agent_view_model.stub(:getUuid).and_return(nil)
    @agent_view_model.stub(:getHostname).and_return(nil)
    @agent_view_model.stub(:getIpAddress).and_return(nil)
    @agent_view_model.stub(:getLocation).and_return(nil)
    @agent_view_model.stub(:getStatusForDisplay).and_return(nil)
    @agent_view_model.stub(:buildLocator).and_return(nil)
    @agent_view_model.stub(:getOperatingSystem).and_return(nil)
    @agent_view_model.stub(:freeDiskSpace).and_return(nil)
    @agent_view_model.stub(:getResources).and_return(nil)
    @agent_view_model.stub(:getEnvironments).and_return(nil)
    @agent_view_model
  end

  def create_config_revision_model
    @config_revision_view_model = double('config_revision_view_model')
    @config_revision_view_model.stub(:getMd5).and_return('md5')
    @config_revision_view_model.stub(:getUsername).and_return('user name')
    @config_revision_view_model.stub(:getGoVersion).and_return('version')
    time = double('time')
    time.stub(:getTime).and_return(12345678)
    @config_revision_view_model.stub(:getTime).and_return(time)
    @config_revision_view_model.stub(:getSchemaVersion).and_return('schema')
    @config_revision_view_model.stub(:getCommitSHA).and_return('commit')
    @config_revision_view_model
  end

  def create_empty_config_revision_model
    @config_revision_view_model = double('config_revision_view_model')
    @config_revision_view_model.stub(:getMd5).and_return(nil)
    @config_revision_view_model.stub(:getUsername).and_return(nil)
    @config_revision_view_model.stub(:getGoVersion).and_return(nil)
    @config_revision_view_model.stub(:getTime).and_return(nil)
    @config_revision_view_model.stub(:getSchemaVersion).and_return(nil)
    @config_revision_view_model.stub(:getCommitSHA).and_return(nil)
    @config_revision_view_model
  end

  def create_pipeline_status_model
    @pipeline_status_model = double('PipelineStatusViewModel')
    @pipeline_status_model.stub(:isPaused).and_return(true)
    @pipeline_status_model.stub(:pausedCause).and_return('Pausing it for some reason')
    @pipeline_status_model.stub(:pausedBy).and_return('admin')
    @pipeline_status_model.stub(:isPaused).and_return(true)
    @pipeline_status_model.stub(:isLocked).and_return(true)
    @pipeline_status_model.stub(:isSchedulable).and_return(true)
    @pipeline_status_model
  end

  def create_stage_config_model
    @stage_config_view_model = double('StageConfigViewModel')
    @stage_config_view_model.stub(:name).and_return('stage name')
    @stage_config_view_model
  end

  def create_material_config_model
    @material_config_view_model = double('MaterialConfigViewModel')
    @material_config_view_model.stub(:getFingerprint).and_return('fingerprint')
    @material_config_view_model.stub(:getTypeForDisplay).and_return('git')
    @material_config_view_model.stub(:getLongDescription).and_return('URL: http://test.com Branch: master')
    @material_config_view_model
  end

  def create_pipeline_config_model
    @pipeline_config_view_model = double('PipelineConfigViewModel')
    @pipeline_config_view_model.stub(:name).and_return('pipeline name')
    @pipeline_config_view_model.stub(:getLabelTemplate).and_return('label')
    @pipeline_config_view_model.stub(:materialConfigs).and_return([create_material_config_model])
    @pipeline_config_view_model.stub(:getStages).and_return([create_stage_config_model])
    @pipeline_config_view_model
  end

  def create_pipeline_group_config_model
    @pipeline_group_config_view_model = double('PipelineGroupConfigViewModel')
    @pipeline_group_config_view_model.stub(:getGroup).and_return('pipeline group name')
    @pipeline_group_config_view_model.stub(:getPipelines).and_return([create_pipeline_config_model])
    @pipeline_group_config_view_model
  end
end

