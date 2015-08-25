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

describe StagesHelper do
  include StagesHelper, GoUtil

  before do
    @stage = StageMother.scheduledStage("cruise", 10, "dev", 5, "unit")
    @stage_summary = StageSummaryModel.new(@stage, Stages.new(), JobDurationStrategy::ConstantJobDuration.new(10), @stage.getIdentifier())
    @new_stage = Stage.new()
    @new_stage_summary = StageSummaryModel.new(@new_stage, Stages.new(), JobDurationStrategy::ConstantJobDuration.new(10), StageIdentifier.new())
  end

  it "should generate pipeline url when stage identifier is given" do
    expect(stage_detail_path_for_identifier(StageIdentifier.new("pipeline", 10, "stage", "5"))).to eq("/pipelines/pipeline/10/stage/5")
  end

  it "should generate a stage tabl url for a given tab" do
    expect(tab_aware_path_for_stage(StageIdentifier.new("pipeline", 10, "stage", "5"), "jobs")).to eq("/pipelines/pipeline/10/stage/5/jobs")
  end

  it "should generate pipeline url when stage identifier is given" do
    expect(stage_detail_pipeline_tab_for_identifier(StageIdentifier.new("pipeline", 10, "stage", "5"))).to eq("/pipelines/pipeline/10/stage/5/pipeline")
  end

  it "should understand if stage is dummy" do
    expect(placeholder_stage?(@stage_summary)).to be_false
    expect(placeholder_stage?(@new_stage_summary)).to be_true
  end

  it "should understand if stage is the current stage" do
    params[:pipeline_name] = "pipeline_name"
    params[:pipeline_counter] = "1"
    params[:stage_name] = "stage_name"
    params[:stage_counter] = "2"
    expect(is_current_stage?(StageIdentifier.new('pipeline_name', 1, "stage_name", "2"))).to be_true
    expect(is_current_stage?(StageIdentifier.new('pipeline_name_x', 1, "stage_name", "2"))).to be_false
    expect(is_current_stage?(StageIdentifier.new('pipeline_name', 10, "stage_name", "2"))).to be_false
    expect(is_current_stage?(StageIdentifier.new('pipeline_name', 1, "stage_name_x", "2"))).to be_false
    expect(is_current_stage?(StageIdentifier.new('pipeline_name', 1, "stage_name", "20"))).to be_false
  end

  it "should generate link with current tab css if this is the current tab" do
    in_params(:pipeline_name => "foo_bar", :stage_name => "stage-name", :pipeline_counter => "1", :stage_counter => "1", :action => "jobs", :controller => "stages")
    link = link_with_current_tab "Jobs", "jobs"
    expect(link).to have_selector("li.current a[href='/pipelines/foo_bar/1/stage-name/1/jobs']", :text => "Jobs")
  end

  it "should not generate link with current tab css if this is not the current tab" do
    in_params(:pipeline_name => "foo_bar", :stage_name => "stage-name", :pipeline_counter => "1", :stage_counter => "1", :action => "jobs", :controller => "stages")
    link = link_with_current_tab "Tests", "tests"
    expect(link).to_not have_selector("li.current")
    expect(link).to have_selector("li a[href='/pipelines/foo_bar/1/stage-name/1/tests']", :text => "Tests")
  end

  it "should return true when config version mismatches" do
    expect(is_config_used_to_run_this_stage_out_of_sync_with_current?('foo', 'bar')).to eq(true)
  end

  it "should return false when config version matches" do
    expect(is_config_used_to_run_this_stage_out_of_sync_with_current?('foo', 'foo')).to eq(false)
  end
end
