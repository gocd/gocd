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

describe "/shared/_pipeline.html.erb" do
  include PipelineModelMother

  before do
    allow(view).to receive(:go_config_service).and_return(@go_config_service = double('go_config_service'))
    @go_config_service.stub(:getTrackingToolFor).with("blah_pipeline").and_return(TrackingTool.new("http://pavan/${ID}", "#(\\d+)"))
  end

  it "should display trigger button if showControls is true" do
    render :partial => "shared/pipeline", :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label"), :show_controls=>true}}
    expect(response).to have_selector("button[value='Trigger']")
  end

  it "should display trigger with options button if showControls is true" do
    render :partial => "shared/pipeline", :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label"), :show_controls=>true}}
    expect(response).to have_selector("button[value='Trigger with options']")
  end


  it "should not display trigger button if showControls is false" do
    render :partial => "shared/pipeline", :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label"), :show_controls=>false}}
    expect(response).to_not have_selector("input[type='button']")
  end

  it "should disable trigger button if pipeline can not be triggered" do
    render :partial => "shared/pipeline", :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label", false, false, false, true), :show_controls=>true}}
    expect(response).to have_selector("button[value='Trigger'][disabled]")
  end

  it "should disable trigger with options button if pipeline can not be triggered" do
    render :partial => "shared/pipeline", :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label", false, false, false, true), :show_controls=>true}}
    expect(response).to have_selector("button[value='Trigger with options'][disabled]")
  end

  it "should display pause button if not already paused" do
    render :partial => "shared/pipeline", :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label", false, false), :show_controls=>true}}

    expect(response).to_not have_selector("input[value='Unpause']")
    expect(response).to have_selector("button[value='Pause']")
    expect(response).to have_selector("form[action='/api/pipelines/blah_pipeline/pause']", visible: false)
  end

  it "should display unpause button if already paused" do
    render :partial => "shared/pipeline", :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label", false, false, "i like it"), :show_controls=>true}}

    expect(response).to_not have_selector("input[value='Pause']")
    expect(response).to have_selector("button[value='Unpause']")
    expect(response).to have_selector("form[action='/api/pipelines/blah_pipeline/unpause']")
    expect(response).to have_selector(".pause_description.paused_by", :text => "Paused by raghu")
    expect(response).to have_selector(".pause_description.pause_message", :text => "(i like it)")
  end

  it "should escape markup in pause cause" do
    render :partial => "shared/pipeline", :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label", false, false, "<div><script></div>"), :show_controls=>true}}

    expect(response).to have_selector(".pause_description.paused_by", :text => "Paused by raghu")
    expect(Capybara.string(response.body).find(".pause_description.pause_message").native.inner_html).to eq("(&lt;div&gt;&lt;script&gt;&lt;/div&gt;)")
  end

  it "should restrict maxlength of pause cause" do
    render :partial => "shared/pipeline", :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label", false, false), :show_controls=>true}}
    expect(response).to have_selector("input[type='text'][name='pauseCause'][maxlength='255']", visible: false)
  end

  describe "when not operatable" do
    it "should not display trigger button if pipeline can not be operated upon" do
      render :partial => "shared/pipeline", :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label", false, false, false, false), :show_controls=>true}}
      expect(response).to_not have_selector("input[value='Trigger']")
    end

    it "should display pause button if not already paused" do
      render :partial => "shared/pipeline", :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label", false, false, false, false), :show_controls=>true}}
      expect(response).to_not have_selector("input[value='Unpause']")
      expect(response).to_not have_selector("input[value='Pause']")
    end

    it "should display unpause button if not already paused" do
      render :partial => "shared/pipeline", :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label", false, false, "working with agent", false), :show_controls=>true}}
      expect(response).to_not have_selector("input[value='Pause']")
      expect(response).to_not have_selector("input[value='Unpause']")
    end

    it "should display warn only if show changes is false" do
      now = org.joda.time.DateTime.new
      tomorrow = now.plusDays(1)
      first_job = PipelineHistoryMother.job(JobState::Completed, JobResult::Cancelled, tomorrow.toDate())
      second_job = PipelineHistoryMother.job(JobState::Completed, JobResult::Passed, now.toDate())
      stage = PipelineHistoryMother.stagePerJob("stage", [first_job, second_job])
      pim = PipelineHistoryMother.singlePipeline("pipeline-name", stage);
      pim.setLatestRevisions(MaterialRevisions::EMPTY)
      pim.setMaterialConfigs(MaterialConfigs.new([MaterialConfigsMother.hgMaterialConfig()]))

      pipeline_model = PipelineModel.new("pipeline", true, true, PipelinePauseInfo::notPaused())

      pipeline_model.addPipelineInstance(pim)

      ModificationsMother.createHgMaterialRevisions();

      render :partial => "shared/pipeline", :locals => {:scope => {:pipeline_model => pipeline_model, :show_changes => false}}

      expect(response).to have_selector(".has_new_materials")
    end

    it "should not display warn if show changes is true" do
      now = org.joda.time.DateTime.new
      tomorrow = now.plusDays(1)
      first_job = PipelineHistoryMother.job(JobState::Completed, JobResult::Cancelled, tomorrow.toDate())
      second_job = PipelineHistoryMother.job(JobState::Completed, JobResult::Passed, now.toDate())
      stage = PipelineHistoryMother.stagePerJob("stage", [first_job, second_job])
      pim = PipelineHistoryMother.singlePipeline("pipeline-name", stage)
      pim.setLatestRevisions(MaterialRevisions::EMPTY)
      pim.setMaterialConfigs(MaterialConfigs.new([MaterialConfigsMother.hgMaterialConfig()]))

      pipeline_model = PipelineModel.new("pipeline", true, true, PipelinePauseInfo::notPaused())

      pipeline_model.addPipelineInstance(pim)

      ModificationsMother.createHgMaterialRevisions()

      render :partial => "shared/pipeline", :locals => {:scope => {:pipeline_model => pipeline_model, :show_changes => true}}

      expect(response).to_not have_selector(".warn_message", :text => "new revisions")
    end

    it "should show compare link when show_compare is set to true" do
      render :partial=> 'shared/pipeline.html', :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label", false, false, "working with agent", false), :show_compare => true}}

      expect(response.body).to have_selector("span.compare_pipeline")
      expect(response.body).to have_selector("span.compare_pipeline a[title='Compare with the previous build']")
    end

    it "should not show compare link when show_compare is not set" do
      render :partial=> 'shared/pipeline.html', :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label", false, false, "working with agent", false)}}

      expect(response.body).to_not have_selector("span.compare_pipeline")
    end

    it "should show settings icon in pipeline when user is an admin" do
      render :partial=> 'shared/pipeline.html', :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label", false, false, "working with agent", false, MaterialRevisions.new([].to_java(MaterialRevision)), true)}}

      expect(response.body).to have_selector("a.setting")
    end

    it "should not show settings icon in pipeline when user is not an admin" do
      render :partial=> 'shared/pipeline.html', :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label", false, false, "working with agent", false, MaterialRevisions.new([].to_java(MaterialRevision)), false)}}

      expect(response.body).to_not have_selector("a.setting")
    end

    it "should add a separator if both changes and compare are present" do
      now = org.joda.time.DateTime.new
      tomorrow = now.plusDays(1)
      first_job = PipelineHistoryMother.job(JobState::Completed, JobResult::Cancelled, tomorrow.toDate())
      stage = PipelineHistoryMother.stagePerJob("stage", [first_job])
      pim = PipelineHistoryMother.singlePipeline("pipeline-name", stage)
      pipeline_model = PipelineModel.new("pipeline", true, true, PipelinePauseInfo::notPaused())
      pipeline_model.addPipelineInstance(pim)

      render :partial=> 'shared/pipeline.html', :locals => {:scope => {:pipeline_model => pipeline_model, :show_compare => true, :show_changes => true}}

      expect(response.body).to have_selector("span.separator")
    end
  end
end
