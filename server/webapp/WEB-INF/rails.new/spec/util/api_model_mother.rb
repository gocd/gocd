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

  def create_material_view_model
    @material_view_model = double('MaterialViewModel')
    @material_view_model.stub(:getId).and_return(2)
    @material_view_model.stub(:getFingerprint).and_return('fingerprint')
    @material_view_model.stub(:getTypeForDisplay).and_return('git')
    @material_view_model.stub(:getLongDescription).and_return('URL: http://test.com Branch: master')
    @material_view_model
  end

  def create_modification_view_model
    @modification_view_model = double('ModificationViewModel')
    @modification_view_model.stub(:getId).and_return(3)
    @modification_view_model.stub(:getRevision).and_return('revision')
    @modification_view_model.stub(:getModifiedTime).and_return('modification time')
    @modification_view_model.stub(:getUserName).and_return('user name')
    @modification_view_model.stub(:getComment).and_return('comment')
    @modification_view_model.stub(:getEmailAddress).and_return('test@test.com')
    @modification_view_model
  end

  def create_material_revision_view_model
    @material_revisions_view_model = double('MaterialRevisionsViewModel')
    @material_revisions_view_model.stub(:getMaterial).and_return(create_material_view_model)
    @material_revisions_view_model.stub(:isChanged).and_return(true)
    @material_revisions_view_model.stub(:getModifications).and_return([create_modification_view_model])
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

  def create_job_model
    @job_view_model = double('JobViewModel')
    @job_view_model.stub(:getId).and_return(5)
    @job_view_model.stub(:getName).and_return('job name')
    @job_view_model.stub(:getState).and_return('state')
    @job_view_model.stub(:getResult).and_return('result')
    @job_view_model.stub(:getScheduledDate).and_return('scheduled time')
    @job_view_model
  end

  def create_stage_model
    @stage_view_model = double('StageViewModel')
    @stage_view_model.stub(:getId).and_return(4)
    @stage_view_model.stub(:getName).and_return('stage name')
    @stage_view_model.stub(:getCounter).and_return('1')
    @stage_view_model.stub(:isScheduled).and_return(false)
    @stage_view_model.stub(:getApprovalType).and_return('manual')
    @stage_view_model.stub(:getApprovedBy).and_return('me')
    @stage_view_model.stub(:getResult).and_return('passed')
    @stage_view_model.stub(:getRerunOfCounter).and_return(1)
    @stage_view_model.stub(:hasOperatePermission).and_return('yes')
    @stage_view_model.stub(:getCanRun).and_return(true)
    @stage_view_model.stub(:getBuildHistory).and_return([create_job_model])
    @stage_view_model
  end

  def create_pipeline_model
    @pipeline_view_model = double('PipelineViewModel')
    @pipeline_view_model.stub(:getId).and_return(1)
    @pipeline_view_model.stub(:getName).and_return('pipeline name')
    @pipeline_view_model.stub(:getCounter).and_return(11)
    @pipeline_view_model.stub(:getLabel).and_return('label')
    @pipeline_view_model.stub(:getNaturalOrder).and_return(1.0)
    @pipeline_view_model.stub(:getCanRun).and_return(true)
    @pipeline_view_model.stub(:isPreparingToSchedule).and_return(false)
    @pipeline_view_model.stub(:isCurrentlyLocked).and_return(false)
    @pipeline_view_model.stub(:isLockable).and_return(true)
    @pipeline_view_model.stub(:canUnlock).and_return(false)
    @pipeline_view_model.stub(:getBuildCause).and_return(create_build_cause_model)
    @pipeline_view_model.stub(:getStageHistory).and_return([create_stage_model])
    @pipeline_view_model
  end

  def create_pipeline_history_model
    @pipeline_history_view_model = [create_pipeline_model]
    @pipeline_history_view_model
  end
end

