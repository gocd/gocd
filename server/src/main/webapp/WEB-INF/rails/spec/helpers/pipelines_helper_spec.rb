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

    it "should display the trigger message with username" do
      triggered_date = java.util.Date.new
      pim = pipeline_model("blah-pipeline", "blah-label", false, false, "working with agent", false).getLatestPipelineInstance()
      message = trigger_message(triggered_date, pim)

      expect(message).to have_selector(".who", text: "Anonymous")
    end

    it "should not display the trigger message when the pipeline is being scheduled for the first time" do
      triggered_date = java.util.Date.new
      pim = PipelineInstanceModel.createPreparingToSchedule("pipeline", nil)

      message = trigger_message(triggered_date, pim)

      expect(message.blank?).to be_truthy
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
      expect(message).to have_selector(".label", "Automatically triggered")
      expect(message).to have_selector(".time[data='#{joda_date.to_date.getTime}']")
    end
  end

  it "should return the type of the material" do
    expect(material_type(MaterialsMother.hgMaterial())).to eq "scm"
    expect(material_type(MaterialsMother.svnMaterial("url", "folder"))).to eq "scm"
    expect(material_type(MaterialsMother.dependencyMaterial("blah_pipeline", "blah_stage"))).to eq "dependency"
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
    expect(url_for_dmr(dmr)).to eq("/go/pipelines/value_stream_map/blah-pipeline/2")
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

  describe "with_pipeline_analytics_support" do
    before :each do
      @default_plugin_info_finder = double('default_plugin_info_finder')
      vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
      about = GoPluginDescriptor::About.builder
                .name("Foo plugin")
                .version("1.2.3")
                .targetGoVersion("17.2.0")
                .description("Does foo")
                .vendor(vendor)
                .targetOperatingSystems(["Linux"])
                .build
      descriptor = proc do |id| GoPluginDescriptor.builder.id(id).version("1.0").about(about).build end
      supports_analytics = proc do |supports_pipeline_analytics, supports_dashboard_analytics|
        supported = []
        supported << com.thoughtworks.go.plugin.domain.analytics.SupportedAnalytics.new("pipeline", "id1", "title1") if supports_pipeline_analytics
        supported << com.thoughtworks.go.plugin.domain.analytics.SupportedAnalytics.new("dashboard", "id2", "title2") if supports_dashboard_analytics
        com.thoughtworks.go.plugin.domain.analytics.Capabilities.new(supported)
      end

      @plugin_info1 = CombinedPluginInfo.new(AnalyticsPluginInfo.new(descriptor.call('plugin1'), nil, supports_analytics.call(true, true), nil))
      @plugin_info2 = CombinedPluginInfo.new(AnalyticsPluginInfo.new(descriptor.call('plugin2'), nil, supports_analytics.call(true, false), nil))
      @plugin_info3 = CombinedPluginInfo.new(AnalyticsPluginInfo.new(descriptor.call('plugin3'), nil, supports_analytics.call(false, true), nil))
      @plugin_info4 = CombinedPluginInfo.new(AnalyticsPluginInfo.new(descriptor.call('plugin3'), nil, supports_analytics.call(false, false), nil))

    end

    it "should find the first plugin where pipeline analytics are supported" do
      def default_plugin_info_finder; @default_plugin_info_finder; end
      def is_user_an_admin?; true; end
      def show_analytics_only_for_admins?; false; end

      allow(@default_plugin_info_finder).to receive('allPluginInfos').with(PluginConstants.ANALYTICS_EXTENSION).and_return([@plugin_info1, @plugin_info2, @plugin_info3, @plugin_info4])

      ids = []
      with_pipeline_analytics_support do |plugin_id|
        ids << plugin_id
      end

      expect(ids).to eq(['plugin1'])
    end

    it "should not find any plugin ids for analytics if user is non admin and if only admin users can view pipeline analytics" do
      def default_plugin_info_finder; @default_plugin_info_finder; end
      def is_user_an_admin?; false; end
      def show_analytics_only_for_admins?; true; end

      allow(@default_plugin_info_finder).to receive('allPluginInfos').with(PluginConstants.ANALYTICS_EXTENSION).and_return([@plugin_info1, @plugin_info2, @plugin_info3, @plugin_info4])

      ids = []
      with_pipeline_analytics_support do |plugin_id|
        ids << plugin_id
      end

      expect(ids).to eq([])
    end
  end
end
