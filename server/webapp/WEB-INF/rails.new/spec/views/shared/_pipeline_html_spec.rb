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

require File.expand_path(File.dirname(__FILE__) + '/../../spec_helper')

describe "/shared/_pipeline.html.erb" do
  include PipelineModelMother

  before do
    template.stub!(:go_config_service).and_return(@go_config_service = mock('go_config_service'))
    @go_config_service.stub(:getTrackingToolFor).with("blah_pipeline").and_return(TrackingTool.new("http://pavan/${ID}", "#(\\d+)"))
  end

  it "should display trigger button if showControls is true" do
    render :partial => "shared/pipeline", :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label"), :show_controls=>true}}
    response.should have_tag "button[value='Trigger']"
  end

  it "should display trigger with options button if showControls is true" do
    render :partial => "shared/pipeline", :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label"), :show_controls=>true}}
    response.should have_tag "button[value='Trigger with options']"
  end


  it "should not display trigger button if showControls is false" do
    render :partial => "shared/pipeline", :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label"), :show_controls=>false}}
    response.should_not have_tag "input[type='button']"
  end

  it "should disable trigger button if pipeline can not be triggered" do
    render :partial => "shared/pipeline", :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label", false, false, false, true), :show_controls=>true}}
    response.should have_tag "button[value='Trigger'][disabled]"
  end

  it "should disable trigger with options button if pipeline can not be triggered" do
    render :partial => "shared/pipeline", :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label", false, false, false, true), :show_controls=>true}}
    response.should have_tag "button[value='Trigger with options'][disabled]"
  end

  it "should display pause button if not already paused" do
    render :partial => "shared/pipeline", :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label", false, false), :show_controls=>true}}
    response.should_not have_tag "input[value='Unpause']"
    response.should have_tag "button[value='Pause']"
    response.should have_tag "form[action='/api/pipelines/blah_pipeline/pause']"
  end

  it "should display unpause button if already paused" do
    render :partial => "shared/pipeline", :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label", false, false, "i like it"), :show_controls=>true}}
    response.should_not have_tag "input[value='Pause']"
    response.should have_tag "button[value='Unpause']"
    response.should have_tag "form[action='/api/pipelines/blah_pipeline/unpause']"
    response.should have_tag ".pause_description.paused_by", "Paused by raghu"
    response.should have_tag ".pause_description.pause_message", "(i like it)"
  end

  it "should escape markup in pause cause" do
    render :partial => "shared/pipeline", :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label", false, false, "<div><script></div>"), :show_controls=>true}}
    response.should have_tag ".pause_description.paused_by", "Paused by raghu"
    response.should have_tag ".pause_description.pause_message", "(&lt;div&gt;&lt;script&gt;&lt;/div&gt;)"
  end

  it "should restrict maxlength of pause cause" do
    render :partial => "shared/pipeline", :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label", false, false), :show_controls=>true}}
    response.should have_tag("input[type='text'][name='pauseCause'][maxlength=255]")
  end

  describe "when not operatable" do
    it "should not display trigger button if pipeline can not be operated upon" do
      render :partial => "shared/pipeline", :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label", false, false, false, false), :show_controls=>true}}
      response.should_not have_tag "input[value='Trigger']"
    end

    it "should display pause button if not already paused" do
      render :partial => "shared/pipeline", :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label", false, false, false, false), :show_controls=>true}}
      response.should_not have_tag "input[value='Unpause']"
      response.should_not have_tag "input[value='Pause']"
    end

    it "should display unpause button if not already paused" do
      render :partial => "shared/pipeline", :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label", false, false, "working with agent", false), :show_controls=>true}}
      response.should_not have_tag "input[value='Pause']"
      response.should_not have_tag "input[value='Unpause']"
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

      response.should have_tag ".has_new_materials"
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

      response.should_not have_tag ".warn_message", "new revisions"
    end

    it "should show compare link when show_compare is set to true" do
      render :partial=> 'shared/pipeline.html', :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label", false, false, "working with agent", false), :show_compare => true}}

      response.body.should have_tag("span.compare_pipeline")
      response.body.should have_tag("span.compare_pipeline a[title=?]", "Compare with the previous build")
    end

    it "should not show compare link when show_compare is not set" do
      render :partial=> 'shared/pipeline.html', :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label", false, false, "working with agent", false)}}

      response.body.should_not have_tag("span.compare_pipeline")
    end

    it "should show settings icon in pipeline when user is an admin" do
      render :partial=> 'shared/pipeline.html', :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label", false, false, "working with agent", false, MaterialRevisions.new([].to_java(MaterialRevision)), true)}}

      response.body.should have_tag("a.setting")
    end

    it "should not show settings icon in pipeline when user is not an admin" do
      render :partial=> 'shared/pipeline.html', :locals => {:scope => {:pipeline_model => pipeline_model("blah_pipeline", "blah_label", false, false, "working with agent", false, MaterialRevisions.new([].to_java(MaterialRevision)), false)}}

      response.body.should_not have_tag("a.setting")
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

      response.body.should have_tag("span.separator")
    end
  end
end
