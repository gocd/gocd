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

describe 'stages/stage.json.erb' do
  include StageModelMother, PipelineModelMother

  before do
    view.stub(:is_user_an_admin?).and_return(true)
    view.stub(:config_change_path)
    assign :pipeline,  pipeline_model("pipeline_name", "blah_label", false, false, "working with agent", false).getLatestPipelineInstance()
    assign :stage_history_page, stage_history_page(10)
  end

  it "should localize strings" do
    params[:action] = 'overview'
    assign :pipeline,  pipeline_model("pipeline_name", "blah_label", false, false, "working with agent", false).getLatestPipelineInstance()
    assign :stage, stage_with_5_jobs
    view.stub(:url_for)
    render
    json = JSON.parse(response.body)
    expect(json["jobs_in_progress"]["html"]).to have_selector ".elapsed_time", /^Elapsed:\s+2 minutes.*/
  end

  describe "with render mocked" do

    before do
      assign :stage, stage_with_three_runs
      view.stub(:render_json) do | options |
        "\"#{options[:partial]}\""
      end
    end

    def render_json
      render
      json = JSON.parse(response.body)
      common_asserts json
      json
    end

    def common_asserts json
      expect(json["pipeline_status_bar"]["html"]).to eq "pipelines/status_bar.html.erb"
      expect(json["stage_result"]["html"]).to eq "stage_result.html"
      expect(json["stage_run_details"]["html"]).to eq "run_details.html"
      expect(json["other_stage_runs"]["html"]).to eq "other_stage_runs.html"
      expect(json["current_stage_run"]["html"]).to eq "current_stage_run.html"
      expect(json["pipeline_header"]["html"]).to eq "pipelines/pipeline_header.html"
    end

    it "should auto refresh overview" do
      params[:action] = 'overview'
      json = render_json
      expect(json["jobs_failed"]["html"]).to eq "jobs_breakdown.html.erb"
      expect(json["jobs_passed"]["html"]).to eq "jobs_breakdown.html.erb"
      expect(json["jobs_in_progress"]["html"]).to eq "jobs_breakdown.html.erb"
      expect(json["stage_history"]["html"]).to eq "stage_history.html"

    end

    it "should auto refresh pipeline" do
      params[:action] = 'pipeline'
      json = render_json
      expect(json["pipeline_visualization"]["html"]).to eq "pipelines/pipeline_dependencies.html"
      expect(json["jobs_failed"]).to eq nil
    end

    it "should not autorefresh stage history for stats tab" do
      params[:action] = 'stats'
      json = render_json
      expect(json["stage_history"]).to be_nil
    end

    it "should not autorefresh stage history for pipeilnes tab" do
      params[:action] = 'pipeline'
      json = render_json
      expect(json["stage_history"]).to be_nil
    end

    it "should auto refresh materials" do
      params[:action] = 'materials'
      json = render_json
      expect(json["pipeline_visualization"]).to eq nil
      expect(json["stage_history"]["html"]).to eq "stage_history.html"
    end

    it "should auto refresh jobs" do
      params[:action] = 'jobs'
      json = render_json
      expect(json["jobs_grid"]["html"]).to eq 'jobs.html'
      expect(json["stage_history"]["html"]).to eq "stage_history.html"
    end

    it "should auto refresh fbh" do
      params[:action] = 'tests'
      json = render_json
      expect(json["non_passing_tests"]["html"]).to eq "non_passing_tests.html"
      expect(json["stage_history"]["html"]).to eq "stage_history.html"
    end
  end

  describe "fbh partial caching" do
    before do
      assign :response_format, "json"
      assign :failing_tests, failing_tests = double(:failing_tests)
      failing_tests.stub(:numberOfTests).and_return(0)
      params[:action] = "tests"
      view.stub(:stage_history_url).and_return("history_url")
      view.stub(:stage_history_path).and_return("history_path")
      view.stub(:stage_bar_url).and_return("detail_tab_url")
    end

    it "should cache fbh partial" do
      @failing_stage = StageMother.custom("dev")
      @failing_stage.getJobInstances().get(0).fail()
      @failing_stage.calculateResult()

      @passed_stage = StageMother.custom("dev")
      @passed_stage.passed()
      @passed_stage.setIdentifier(@failing_stage.getIdentifier())
      check_fragment_caching(@failing_stage, @passed_stage, proc {|stage| [ViewCacheKey.new.forFbhOfStagesUnderPipeline(stage.getIdentifier().pipelineIdentifier()), {:subkey => ViewCacheKey.new.forFailedBuildHistoryStage( stage, "json" ), :skip_digest=>true}]}) do |stage|
        assign :stage, stage_model_for(stage)
        render
      end
    end

    it "should use json to scope the key to format" do
      failing_stage = StageMother.custom("dev")
      failing_stage.getJobInstances().get(0).fail()
      failing_stage.calculateResult()

      assign :stage, stage_model_for(failing_stage)
      view.stub(:view_cache_key).and_return(key = double('view_cache_key'))
      key.should_receive(:forFbhOfStagesUnderPipeline).with(failing_stage.getIdentifier().pipelineIdentifier()).and_return("pipeline_id_based_key")
      key.should_receive(:forFailedBuildHistoryStage).with(failing_stage, "json").and_return("stage_fbh_json_key")
      view.should_receive(:cache).with("pipeline_id_based_key", {:subkey => "stage_fbh_json_key", :skip_digest=>true})
      render
    end
  end

end
