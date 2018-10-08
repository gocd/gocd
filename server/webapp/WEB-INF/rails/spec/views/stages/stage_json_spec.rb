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

require 'rails_helper'

describe 'stages/stage.json.erb' do
  include StageModelMother
  include PipelineModelMother
  include GoUtil

  before do
    allow(view).to receive(:is_user_an_admin?).and_return(true)
    allow(view).to receive(:config_change_path)
    assign :pipeline,  pipeline_model("pipeline_name", "blah_label", false, false, "working with agent", false).getLatestPipelineInstance()
    assign :stage_history_page, stage_history_page(10)
  end

  it "should localize strings" do
    in_params({pipeline_counter: 1, pipeline_name: 'foo', stage_counter: 1, stage_name: 'bar'})
    params[:action] = 'overview'
    assign :pipeline,  pipeline_model("pipeline_name", "blah_label", false, false, "working with agent", false).getLatestPipelineInstance()
    assign :stage, stage_with_5_jobs
    allow(view).to receive(:url_for)
    render
    json = JSON.parse(response.body)
    expect(json["jobs_in_progress"]["html"]).to have_selector ".elapsed_time", /^Elapsed:\s+2 minutes.*/
  end

  describe "with render mocked" do

    before do
      assign :stage, stage_with_three_runs
      allow(view).to receive(:render_json) do | options |
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
      expect(json["stage_history"]["html"]).to eq "stage_history.html"
    end

    it "should auto refresh jobs" do
      params[:action] = 'jobs'
      json = render_json
      expect(json["jobs_grid"]["html"]).to eq 'jobs.html'
      expect(json["stage_history"]["html"]).to eq "stage_history.html"
    end
  end

end
