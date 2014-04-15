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

require File.join(File.dirname(__FILE__), "..", "..", "spec_helper")

describe 'stages/stage.json.erb' do
  include StageModelMother, PipelineModelMother

  before do
    template.stub!(:is_user_an_admin?).and_return(true)
    template.stub!(:config_change_path)
    assigns[:pipeline] =  pipeline_model("pipeline_name", "blah_label", false, false, "working with agent", false).getLatestPipelineInstance()
    assigns[:stage_history_page] = stage_history_page(10)    
  end
  
  it "should localize strings" do
    params[:action] = 'overview'
    assigns[:pipeline] =  pipeline_model("pipeline_name", "blah_label", false, false, "working with agent", false).getLatestPipelineInstance()
    assigns[:stage] = stage_with_5_jobs
    template.stub!(:url_for)
    render "stages/stage.json"
    json = JSON.parse(response.body)
    json["jobs_in_progress"]["html"].should have_tag ".elapsed_time", /^Elapsed:\s+2 minutes.*/
  end

  describe "with render mocked" do

    before do
      assigns[:stage] = stage_with_three_runs
      template.stub(:render_json) do | options |
        "\"#{options[:partial]}\""
      end
    end

    def render_json
      render "stages/stage.json"
      json = JSON.parse(response.body)
      common_asserts json
      json
    end

    def common_asserts json
      json["pipeline_status_bar"]["html"].should == "pipelines/status_bar.html.erb"
      json["stage_result"]["html"].should == "stage_result.html"
      json["stage_run_details"]["html"].should == "run_details.html"
      json["other_stage_runs"]["html"].should == "other_stage_runs.html"
      json["current_stage_run"]["html"].should == "current_stage_run.html"
      json["pipeline_header"]["html"].should == "pipelines/pipeline_header.html"
    end

    it "should auto refresh overview" do
      params[:action] = 'overview'
      json = render_json
      json["jobs_failed"]["html"].should == "jobs_breakdown.html.erb"
      json["jobs_passed"]["html"].should == "jobs_breakdown.html.erb"
      json["jobs_in_progress"]["html"].should == "jobs_breakdown.html.erb"
      json["stage_history"]["html"].should == "stage_history.html"

    end

    it "should auto refresh pipeline" do
      params[:action] = 'pipeline'
      json = render_json
      json["pipeline_visualization"]["html"].should == "pipelines/pipeline_dependencies.html"
      json["jobs_failed"].should == nil
    end

    it "should not autorefresh stage history for stats tab" do
      params[:action] = 'stats'
      json = render_json
      json["stage_history"].should == nil
    end

    it "should not autorefresh stage history for pipeilnes tab" do
      params[:action] = 'pipeline'
      json = render_json
      json["stage_history"].should == nil
    end

    it "should auto refresh materials" do
      params[:action] = 'materials'
      json = render_json
      json["pipeline_visualization"].should == nil
      json["stage_history"]["html"].should == "stage_history.html"
    end

    it "should auto refresh jobs" do
      params[:action] = 'jobs'
      json = render_json
      json["jobs_grid"]["html"].should == 'jobs.html'
      json["stage_history"]["html"].should == "stage_history.html"
    end

    it "should auto refresh fbh" do
      params[:action] = 'tests'
      json = render_json
      json["non_passing_tests"]["html"].should == "non_passing_tests.html"
      json["stage_history"]["html"].should == "stage_history.html"
    end
  end

  describe "fbh partial caching" do
    before do
      assigns[:response_format] = "json"
      assigns[:failing_tests] = failing_tests = mock(:failing_tests)
      failing_tests.stub(:numberOfTests).and_return(0)
      params[:action] = "tests"
      template.stub!(:stage_history_url).and_return("history_url")
      template.stub!(:stage_history_path).and_return("history_path")
      template.stub!(:stage_bar_url).and_return("detail_tab_url")
    end

    it "should cache fbh partial" do
      @failing_stage = StageMother.custom("dev")
      @failing_stage.getJobInstances().get(0).fail()
      @failing_stage.calculateResult()

      @passed_stage = StageMother.custom("dev")
      @passed_stage.passed()
      @passed_stage.setIdentifier(@failing_stage.getIdentifier())
      check_fragment_caching(@failing_stage, @passed_stage, proc {|stage| [ViewCacheKey.new.forFbhOfStagesUnderPipeline(stage.getIdentifier().pipelineIdentifier()), {:subkey => ViewCacheKey.new.forFailedBuildHistoryStage( stage, "json" )}]}) do |stage|
        assigns[:stage] = stage_model_for(stage)
        render 'stages/stage.json.erb'
      end
    end

    it "should use json to scope the key to format" do
      failing_stage = StageMother.custom("dev")
      failing_stage.getJobInstances().get(0).fail()
      failing_stage.calculateResult()

      assigns[:stage] = stage_model_for(failing_stage)
      template.stub!(:view_cache_key).and_return(key = mock('view_cache_key'))
      key.should_receive(:forFbhOfStagesUnderPipeline).with(failing_stage.getIdentifier().pipelineIdentifier()).and_return("pipeline_id_based_key")
      key.should_receive(:forFailedBuildHistoryStage).with(failing_stage, "json").and_return("stage_fbh_json_key")
      template.should_receive(:cache).with("pipeline_id_based_key", :subkey => "stage_fbh_json_key")
      render 'stages/stage.json.erb'
    end
  end

end
