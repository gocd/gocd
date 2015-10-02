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

describe "/environments/_environment.html.erb" do
  include PipelineModelMother

  before do
    date = org.joda.time.DateTime.new.toDate

    @stages_for_pipeline_1 = PipelineHistoryMother.stagePerJob("blahStage", [PipelineHistoryMother.job(JobState::Building, JobResult::Unknown, date),
                                                                                                                      PipelineHistoryMother.job(JobState::Completed, JobResult::Failed, date),
                                                                                                                      PipelineHistoryMother.job(JobState::Completed, JobResult::Cancelled, date),
                                                                                                                      PipelineHistoryMother.job(JobState::Completed, JobResult::Passed, date),
                                                                                                                      PipelineHistoryMother.job(JobState::Unknown, JobResult::Unknown, date)])
    @p_1 = pipeline1 = PipelineHistoryMother.singlePipeline("blahPipeline1", @stages_for_pipeline_1)
    pipeline2 = PipelineInstanceModel.createEmptyPipelineInstanceModel("blahPipeline2", BuildCause.createWithEmptyModifications(),  StageInstanceModels.new)
    pipeline3 = PipelineInstanceModel.createEmptyPipelineInstanceModel("blahPipeline3", BuildCause.createWithEmptyModifications(),  StageInstanceModels.new)
    @pipelines = [pipeline_model(pipeline1.getName(),"label"),
                  pipeline_model(pipeline2.getName(),"label"),
                  pipeline_model(pipeline3.getName(),"label")]
    @pipelines[0].addPipelineInstance(pipeline1)
    @pipelines[1].addPipelineInstance( pipeline2)
    @pipelines[2].addPipelineInstance( pipeline3)
    @environment = double('environment uat', :name => "UAT", :getPipelineModels => @pipelines)
    class << view
      include StagesHelper
    end
    allow(view).to receive(:on_pipeline_dashboard?).and_return(false)
  end

  def render_show
    render :partial => 'environments/environment.html.erb', locals: {scope: {environment: @environment, show_edit_environments: true}}
  end

  describe "environment title" do

      it "should display the environment name as link if show_add_environment is true" do
        render :partial => 'environments/environment.html.erb', locals: {scope: {environment: @environment, show_edit_environments: true}}
        expect(response).to have_selector("div.environment h2.entity_title a[href='/environments/UAT/show']", :text => /UAT/)
      end

      it "should display the environment name as plain text if show_add_environment is false" do
        render :partial => 'environments/environment.html.erb', locals: {scope: {environment: @environment, show_edit_environments: false}}
        expect(response).to have_selector("div.environment h2.entity_title", :text => /UAT/)
        expect(response).to_not have_selector("div.environment h2.entity_title a[href='/environments/UAT/show']", :text => /UAT/)
      end

      it "should render environment with environment_pipeline partial" do
        stub_template "_environment_pipeline.html.erb" => "environment pipeline"

        render :partial => 'environments/environment.html.erb', locals: {scope: {environment: @environment, show_edit_environments: true}}

        assert_template partial: "environment_pipeline.html.erb" , locals: {scope: {pipeline_model: @pipelines[0], pipeline_model_subkey: 'environment_html'}}
        assert_template partial: "environment_pipeline.html.erb" , locals: {scope: {pipeline_model: @pipelines[1], pipeline_model_subkey: 'environment_html'}}
        assert_template partial: "environment_pipeline.html.erb" , locals: {scope: {pipeline_model: @pipelines[2], pipeline_model_subkey: 'environment_html'}}
      end
  end

  describe "has no new revisions" do

    before do
      allow(@environment).to receive(:hasNewRevisions).and_return(false)
      render_show
    end

    it "should display all piplelines in a environemnt" do
      Capybara.string(response.body).all("div.pipeline").tap do |pipelines|
        expect(pipelines[0]).to have_selector("h3.title a", :text => "blahPipeline1")
        expect(pipelines[1]).to have_selector("h3.title a", :text => "blahPipeline2")
        expect(pipelines[2]).to have_selector("h3.title a", :text => "blahPipeline3")
      end

      Capybara.string(response.body).all("div.pipeline h3.title").tap do |pipelines|
        expect(pipelines[0]).to have_selector("a[href='/tab/pipeline/history/blahPipeline1']", :text => "blahPipeline1")
        expect(pipelines[1]).to have_selector("a[href='/tab/pipeline/history/blahPipeline2']", :text => "blahPipeline2")
        expect(pipelines[2]).to have_selector("a[href='/tab/pipeline/history/blahPipeline3']", :text => "blahPipeline3")
      end
    end

    it "should display url for each stage" do
      expect(response).to have_selector(".stages a.stage[href='/pipelines/blahPipeline1/1/blahStage-0/1']")
    end

    it "should display URL for pipeline for an unknown stage" do
      Capybara.string(response.body).all(".stages").tap do |stages|
        expect(stages[1]).to have_selector("span.stage div[class='stage_bar Unknown']")
      end
    end

    it "should display Label for pipeline with a link to the pipeline detail page" do
      expect(response).to have_selector(".pipelines .status .label", :text => /Label:\s+1/)
      expect(response).to have_selector(".pipelines .status .label a[href='/pipelines/value_stream_map/blahPipeline1/1']", :text => "1")
    end

    it "should set the input to time that the pipeline was run for javascript to display the converted time" do
      expect(response).to have_selector(".pipeline .status .schedule_time", :text => /Triggered/)
      expect(response).to have_selector(".pipeline .status .schedule_time input[type='hidden'][value='#{@stages_for_pipeline_1.getScheduledDate().getTime()}']")
    end

    it "should display all stages for the pipeline" do
      Capybara.string(response.body).all("div.pipeline .stages").tap do |stages|
        #TODO: add href
        expect(stages[1]).to have_selector("div.stage_bar[title='blahStage-0 (Building)']")
        expect(stages[1]).to have_selector("div.stage_bar[title='blahStage-1 (Failed)']")
        expect(stages[1]).to have_selector("div.stage_bar[title='blahStage-2 (Cancelled)']")
        expect(stages[1]).to have_selector("div.stage_bar[title='blahStage-3 (Passed)']")
      end
    end
  end

  describe "caching" do
    it "should cache pipeline partials of different pipelines separately" do
      environment1 = environment_for_caching(JobState::Building, JobResult::Unknown)
      environment2 = environment_for_caching(JobState::Completed, JobResult::Failed)
      key_proc = proc {|environment| [ViewCacheKey.new.forEnvironmentPipelineBox(environment.getPipelineModels()[0]), {:subkey => "environment_html"}]}
      check_fragment_caching(environment1, environment2, key_proc) do |environment|
        render :partial => "environments/environment.html", :locals => {:scope => {:environment => environment, :show_changes => true}}
      end
    end

    def environment_for_caching(latest_pipeline_job_state, latest_pipeline_job_result)
      model = PipelineModel.new("pipelineName", true, true, PipelinePauseInfo.notPaused())
      stages = StageInstanceModels.new
      stages.add(stage_instance("stageName", 13, latest_pipeline_job_state, latest_pipeline_job_result))
      stages.add(NullStageHistoryItem.new("stage2", true))
      pipeline_instance = PipelineInstanceModel.createPipeline("pipelineName", 10, "label-10", BuildCause.createExternal(), stages)
      pipeline_instance.setId(12)
      model.addPipelineInstance(pipeline_instance)
      Environment.new("uat", [model])
    end

    def stage_instance(name, id, state, job_result)
      jobs = JobHistory.new()
      jobs.addJob("dev", state, job_result, java.util.Date.new())
      stage_instance = StageInstanceModel.new(name, "2", jobs)
      stage_instance.setId(id)
      stage_instance
    end
  end
end
