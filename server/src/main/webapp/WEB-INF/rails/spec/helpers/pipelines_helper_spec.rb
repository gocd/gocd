#
# Copyright 2024 Thoughtworks, Inc.
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

require 'rails_helper'

describe PipelinesHelper do
  include PipelinesHelper
  include PipelineModelMother
  include GoUtil

  before do
    @now = org.joda.time.DateTime.new
  end

  describe "stage_bar_url" do
    before do
      @stages = PipelineHistoryMother.stagePerJob("stage", [PipelineHistoryMother.job(JobState::Completed, JobResult::Cancelled, @now.toDate())])
      @stages.add(NullStageHistoryItem.new('blah-stage'))
      @request.path_parameters.reverse_merge!(params)
    end

    it "should have url with action when action is set" do
      expect(stage_bar_url(@stages[0], 'overview')).to eq "/pipelines/pipeline/1/stage-0/1/overview"
    end

    it "should not link to stage not scheduled" do
      expect(stage_bar_url(@stages[1], 'history')).to eq "#"
    end
  end

  describe "run_stage_label" do
    it "should show Rerun for scheduled stage" do
      stages = PipelineHistoryMother.stagePerJob("stage_name", [PipelineHistoryMother.job(JobState::Completed, JobResult::Cancelled, @now.toDate())])
      stage = stages.get(0)
      expect(run_stage_label(stage)).to eq("rerun")
    end

    it "should show Trigger for stage not yet scheduled" do
      stage = NullStageHistoryItem.new("stage_name")
      expect(run_stage_label(stage)).to eq("trigger")
    end
  end

  describe "stage_status_for_ui" do

    before :each do
      @default_timezone = java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Colombo"))
    end

    after :each do
      java.util.TimeZone.setDefault(@default_timezone)
    end

    it "should display the trigger message with the time and username" do
      joda_date = org.joda.time.DateTime.new(2010, 8, 20, 18, 3, 44, 0, org.joda.time.DateTimeZone.forOffsetHoursMinutes(5, 30))
      message = trigger_message_with_formatted_date_time(joda_date.to_date, "Vipul")
      expect(message).to have_selector(".who", text: "Vipul")
      expect(message).to have_selector(".time[data='#{joda_date.to_date.getTime}']")
    end

    it "should display appropriate message when when auto triggered " do
      joda_date = org.joda.time.DateTime.new(2010, 8, 20, 18, 3, 44, 0, org.joda.time.DateTimeZone.forOffsetHoursMinutes(5, 30))
      message = trigger_message_with_formatted_date_time(joda_date.to_date, GoConstants::DEFAULT_APPROVED_BY)
      expect(message).to have_selector(".label", :text => "Automatically triggered")
      expect(message).to have_selector(".time[data='#{joda_date.to_date.getTime}']")
    end
  end

  it "should return the url for value stream map of given pipeline instance" do
    pim = pipeline_model("blah-pipeline", "blah-label", false, false, "working with agent", false).getLatestPipelineInstance()
    expect(url_for_pipeline_value_stream_map(pim)).to eq("/pipelines/value_stream_map/blah-pipeline/5")
  end

  it "should return the url for given DMR" do
    dmr = DependencyMaterialRevision.create("blah-pipeline", 2, "blah-label", "blah-stage", 3)
    expect(url_for_dmr(dmr)).to eq("/go/pipelines/value_stream_map/blah-pipeline/2")
  end
end
