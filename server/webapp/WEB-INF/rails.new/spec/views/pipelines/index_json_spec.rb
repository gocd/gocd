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

describe "/pipelines/index.json.erb" do
  include PipelineModelMother
  include PipelinesHelper

  it "should return json of partials" do
    first = PipelineGroupModel.new("first")

    pip1 = pipeline_model("pip1", "1")
    pip2 = pipeline_model("pip2", "1")
    pip3 = pipeline_model("pip3", "1")
    first.add(pip1)
    first.add(pip2)

    second = PipelineGroupModel.new("second")
    second.add(pip3)

    assign(:pipeline_groups, [first, second])

    allow(view).to receive(:render_json).with(:partial=>'pipelines/pipeline_group.html', :locals => {:scope => { :pipeline_group => first, :omit_pipeline => true}} ).and_return("\"first_group\"")
    allow(view).to receive(:render_json).with(:partial=>'pipelines/pipeline_group.html', :locals => {:scope => { :pipeline_group => second, :omit_pipeline => true}} ).and_return("\"second_group\"")
    allow(view).to receive(:render_json).with(:partial=>'pipelines/pipeline_selector_pipelines.html', :locals => {:scope => {}} ).and_return("\"pipeline_selectors\"")

    allow(view).to receive(:render_json).with(:partial=>'shared/pipeline.html', :locals => { :scope => { :pipeline_model => pip1, :should_display_previously_blurb => true, :show_controls => true, :show_changes => true, :show_compare => true} } ).and_return("\"first pipeline\"")
    allow(view).to receive(:render_json).with(:partial=>'shared/pipeline.html', :locals => { :scope => { :pipeline_model => pip2, :should_display_previously_blurb => true, :show_controls => true, :show_changes => true, :show_compare => true} } ).and_return("\"second pipeline\"")
    allow(view).to receive(:render_json).with(:partial=>'shared/pipeline.html', :locals => { :scope => { :pipeline_model => pip3, :should_display_previously_blurb => true, :show_controls => true, :show_changes => true, :show_compare => true} } ).and_return("\"third pipeline\"")

    render

    json = JSON.parse(response.body)

    expect(json["pipeline_group_first_panel"]).to eq({"html" => "first_group", "parent_id" => "pipeline_groups_container", "index" => 0, "type" => "group_of_pipelines"})
    expect(json["pipeline_group_second_panel"]).to eq({"html" => "second_group", "parent_id" => "pipeline_groups_container", "index" => 1, "type" => "group_of_pipelines"})
    expect(json["pipeline_pip1_panel"]).to eq({"html" => "first pipeline", "parent_id" => "pipeline_group_first_panel", "index" => 0, "type" => "pipeline"})
    expect(json["pipeline_pip2_panel"]).to eq({"html" => "second pipeline", "parent_id" => "pipeline_group_first_panel", "index" => 1, "type" => "pipeline"})
    expect(json["pipeline_pip3_panel"]).to eq({"html" => "third pipeline", "parent_id" => "pipeline_group_second_panel", "index" => 0, "type" => "pipeline"})
    expect(json["pipelines_selector_pipelines"]).to eq({ "html" => "pipeline_selectors", "type"=>"pipeline_selector" })
  end

  describe "caching" do
    it "should cache pipeline partials of different pipelines separately" do
      pipeline_group1 = pipeline_group_model_for_caching(JobState::Building, JobResult::Unknown)
      pipeline_model_1 = pipeline_group1.getPipelineModels().get(0)

      pipeline_group2 = pipeline_group_model_for_caching(JobState::Completed, JobResult::Failed)
      pipeline_model_2 = pipeline_group2.getPipelineModels().get(0)
      key_proc = proc { |pipeline_group| [ViewCacheKey.new.forPipelineModelBox(pipeline_group.getPipelineModels().get(0)), {:subkey => "pipeline_json_#{pipelines_dom_id(pipeline_group.getName())}", :skip_digest => true}] }

      allow(view).to receive(:render_json).with(:partial=>'shared/pipeline.html', :locals => { :scope => { :pipeline_model => pipeline_model_1, :should_display_previously_blurb => true, :show_controls => true, :show_changes => true, :show_compare => true } } ).and_return("\"first pipeline\"")
      allow(view).to receive(:render_json).with(:partial=>'shared/pipeline.html', :locals => { :scope => { :pipeline_model => pipeline_model_2, :should_display_previously_blurb => true, :show_controls => true, :show_changes => true, :show_compare => true } } ).and_return("\"first pipeline\"")

      allow(view).to receive(:render_json).with(:partial=>'pipelines/pipeline_group.html', :locals => {:scope => { :pipeline_group => pipeline_group1, :omit_pipeline => true}} ).and_return("\"first_group\"")
      allow(view).to receive(:render_json).with(:partial=>'pipelines/pipeline_group.html', :locals => {:scope => { :pipeline_group => pipeline_group2, :omit_pipeline => true}} ).and_return("\"second_group\"")

      allow(view).to receive(:render_json).with(:partial=>'pipelines/pipeline_selector_pipelines.html', :locals => {:scope => {}} ).and_return("\"pipeline_selectors_1\"")

      check_fragment_caching(pipeline_group1, pipeline_group2, key_proc) do |pipeline_group|
        assign(:pipeline_groups, [pipeline_group])
        render
      end
    end

    def pipeline_group_model_for_caching(latest_pipeline_job_state, latest_pipeline_job_result)
      model = PipelineModel.new("pipelineName", true, true, PipelinePauseInfo.notPaused())
      stages = StageInstanceModels.new
      stages.add(stage_instance("stageName", 13, latest_pipeline_job_state, latest_pipeline_job_result))
      stages.add(NullStageHistoryItem.new("stage2", true))
      pipeline_instance = PipelineInstanceModel.createPipeline("pipelineName", 10, "label-10", BuildCause.createExternal(), stages)
      pipeline_instance.setId(12)
      model.addPipelineInstance(pipeline_instance)

      stages2 = StageInstanceModels.new()
      stages2.add(stage_instance("stageName", 7, JobState::Completed, JobResult::Passed))
      stages2.add(stage_instance("stage2", 10, JobState::Assigned, JobResult::Unknown))
      pipeline_instance2 = PipelineInstanceModel.createPipeline("pipelineName", 7, "label-7", BuildCause.createExternal(), stages2)
      pipeline_instance2.setId(14)
      model.addPipelineInstance(pipeline_instance2)
      group = PipelineGroupModel.new("group1")
      group.add(model)
      group
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
