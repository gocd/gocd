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

describe "stage_trigger_gate.html" do

  before do
    now = org.joda.time.DateTime.new
    @stage = PipelineHistoryMother.stagePerJob("stage", [PipelineHistoryMother.job(JobState::Completed, JobResult::Cancelled, now.toDate())]).first()
    @stage.setId(12);
    @stage.setOperatePermission(true);
  end

  it "should show approver when a stage is not automatically approved" do
    @stage.setCanRun(false)
    @stage.setApprovedBy("admin")
    render :partial => "pipelines/stage_trigger_gate.html", :locals => {:scope => {:stage_in_status_bar => @stage}}
    expect(response).to have_selector("span[title='Approved by admin']")
  end

  it "should show link with 'Awaiting approval' when stage is manual and runnable" do
    @request.path_parameters[:pipeline_name] = 'cruise'
    @request.path_parameters[:pipeline_counter] = '1'

    @stage.setCanRun(true)
    @stage.setScheduled(false)
    @stage.setApprovedBy(nil)
    @stage.setApprovalType("manual")
    render :partial => "pipelines/stage_trigger_gate.html",
           :locals => {:scope => {:stage_in_status_bar => @stage, :update_opts => {}}}
    expect(response).to have_selector("a[title='Awaiting approval'][class='manual']")
  end

  it "should show link with 'Awaiting approval' when stage is auto and runnable" do
    @request.path_parameters[:pipeline_name] = 'cruise'
    @request.path_parameters[:pipeline_counter] = '1'

    @stage.setCanRun(true)
    @stage.setScheduled(false)
    @stage.setApprovedBy(nil)
    @stage.setApprovalType("success")

    render :partial => "pipelines/stage_trigger_gate.html",
           :locals => {:scope => {:stage_in_status_bar => @stage, :update_opts => {}}}
    expect(response).to have_selector("a[title='Awaiting approval'][class='auto']")
  end

end
