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

describe PipelinesHelper do
  include PipelinesHelper, PipelineModelMother, GoUtil

  before do
    @now = org.joda.time.DateTime.new
  end

  describe :stage_bar_url do
    before do
      @stages = PipelineHistoryMother.stagePerJob("stage", [PipelineHistoryMother.job(JobState::Completed, JobResult::Cancelled, @now.toDate())])
      @stages.add(NullStageHistoryItem.new('blah-stage'))
      @request.path_parameters.reverse_merge!(params)
    end

    it "should have url with action when action is set" do
      expect(stage_bar_url(@stages[0], 'overview')).to eq "/pipelines/pipeline/1/stage-0/1"
    end

    it "should not link to stage not scheduled" do
      expect(stage_bar_url(@stages[1], 'history')).to eq "#"
    end
  end

  describe :run_stage_label do
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

  describe :stage_status_for_ui do

    before :each do
      @default_timezone = java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Colombo"))
    end

    after :each do
      java.util.TimeZone.setDefault(@default_timezone)
    end

    it "should display the trigger message with username and isodate in title" do
      triggered_date = java.util.Date.new
      pim = pipeline_model("blah-pipeline", "blah-label", false, false, "working with agent", false).getLatestPipelineInstance()
      message = trigger_message(triggered_date.getTime(), pim)

      expect(message).to have_selector(".who", text: "Anonymous")
      expect(message).to have_selector("input[value='#{triggered_date.getTime()}']")
    end

    it "should not display the trigger message when the pipeline is being scheduled for the first time" do
      triggered_date = java.util.Date.new
      pim = PipelineInstanceModel.createPreparingToSchedule("pipeline", nil)

      message = trigger_message(triggered_date.getTime(), pim)

      expect(message.blank?).to be_true
    end

    it "should display the trigger message with the time and username" do
      joda_date = org.joda.time.DateTime.new(2010, 8, 20, 18, 3, 44, 0, org.joda.time.DateTimeZone.forOffsetHoursMinutes(5, 30))
      message = trigger_message_with_formatted_date_time(joda_date.to_date, "Vipul")
      expect(message).to have_selector(".who", text: "Vipul")
      expect(message).to have_selector(".time", text: "20 Aug, 2010 at 18:03:44 [+0530]")
    end

    it "should display appropriate message when when auto triggered " do
      joda_date = org.joda.time.DateTime.new(2010, 8, 20, 18, 3, 44, 0, org.joda.time.DateTimeZone.forOffsetHoursMinutes(5, 30))
      message = trigger_message_with_formatted_date_time(joda_date.to_date, GoConstants::DEFAULT_APPROVED_BY)
      expect(message).to have_selector(".label", "Automatically triggered")
      expect(message).to have_selector(".time", "20 Aug, 2010 at 18:03:44 [+0530]")
    end
  end

  it "should return the type of the material" do
    expect(material_type(MaterialsMother.hgMaterial())).to eq "scm"
    expect(material_type(MaterialsMother.svnMaterial("url", "folder"))).to eq "scm"
    expect(material_type(MaterialsMother.dependencyMaterial("blah_pipeline", "blah_stage"))).to eq "dependency"
  end

  it "should return the url for given pipeline instance" do
    pim = pipeline_model("blah-pipeline", "blah-label", false, false, "working with agent", false).getLatestPipelineInstance()
    expect(url_for_pipeline_instance(pim)).to eq "/pipelines/blah-pipeline/5/cruise/10/pipeline"
  end

  it "should return the url for value stream map of given pipeline instance" do
    pim = pipeline_model("blah-pipeline", "blah-label", false, false, "working with agent", false).getLatestPipelineInstance()
    expect(url_for_pipeline_value_stream_map(pim)).to eq("/pipelines/value_stream_map/blah-pipeline/5")
  end

  it "should return the pipeline instance identifier" do
    pim = pipeline_model("blah-pipeline", "blah-label", false, false, "working with agent", false).getLatestPipelineInstance()
    expect(pipeline_instance_identifier(pim)).to eq("blah-pipeline_5")
  end

  it "should return the url for given DMR" do
    dmr = DependencyMaterialRevision.create("blah-pipeline", 2, "blah-label", "blah-stage", 3)
    expect(url_for_dmr(dmr)).to eq("/pipelines/blah-pipeline/2/blah-stage/3/pipeline")
  end

  it "should return the dom id for a pipeline group" do
    expect(pipelines_dom_id("blah")).to eq("pipeline_group_blah_panel")
  end

  it "should return the dom id for a pipeline" do
    pipeline_model = pipeline_model("blah-pipeline", "blah-label", false, false, "working with agent", false)
    expect(pipelines_pipeline_dom_id(pipeline_model)).to eq("pipeline_blah-pipeline_panel")
  end

  it "should return the build cause popup id for a given pipeline instance model" do
    pim = pipeline_model("blah-pipeline", "blah-label", false, false, "working with agent", false).getLatestPipelineInstance()
    expect(pipeline_build_cause_popup_id(pim)).to eq("changes_blah-pipeline_5")
  end

  describe "revision_for" do
    it "should return short revision for scm material" do
      revision = ModificationsMother.createHgMaterialRevisions().getRevisions().get(0)
      expect(revision_for(revision)).to eq(revision.getLatestShortRevision())
    end

    it "should return pipeline identifier for pipeline material" do
      revision = ModificationsMother.createPipelineMaterialRevision("p1/2/s2/1")
      expect(revision_for(revision)).to eq("p1/2")
    end

    it "should return package revision when material type is package" do
      revision = ModificationsMother.createPackageMaterialRevision("go-agent-13.1.noarch.rpm")
      expect(revision_for(revision)).to eq("go-agent-13.1.noarch.rpm")
    end
  end
end
