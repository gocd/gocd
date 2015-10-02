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

describe "/pipelines/_pipelines_locking_blurb.html.erb" do
  include PipelineModelMother

  def create_pipeline_instance_model()
    now = org.joda.time.DateTime.new
    tomorrow = now.plusDays(1)
    first_job = PipelineHistoryMother.job(JobState::Completed, JobResult::Cancelled, tomorrow.toDate())
    second_job = PipelineHistoryMother.job(JobState::Completed, JobResult::Passed, now.toDate())
    stage = PipelineHistoryMother.stagePerJob("stage", [first_job, second_job])

    PipelineHistoryMother.singlePipeline("pipeline-name", stage)
  end

  it "should show locked when pipeline is lockable and a current instance running " do
    pim = create_pipeline_instance_model()
    pim.set_currently_locked(true)
    pim.setIsLockable(true)

    pipeline_model = PipelineModel.new("pipeline", true, true, PipelinePauseInfo::notPaused())
    pipeline_model.addPipelineInstance(pim)

    render :partial => "pipelines/pipeline_locking_blurb", :locals => {:scope => {:pipeline_model => pipeline_model}}

    expect(response).to have_selector("span[class='locked_instance locked']")
  end

  it "should show click to unlock when pipeline is lockable and it can be unlocked " do
    pim = create_pipeline_instance_model()
    pim.set_currently_locked(true)
    pim.setIsLockable(true)
    pim.set_can_unlock(true)

    pipeline_model = PipelineModel.new("pipeline", true, true, PipelinePauseInfo::notPaused())
    pipeline_model.addPipelineInstance(pim)

    render :partial => "pipelines/pipeline_locking_blurb", :locals => {:scope => {:pipeline_model => pipeline_model}}

    expect(response).to have_selector("span[class='locked_instance click_to_unlock']")
  end
end
